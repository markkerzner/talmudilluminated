package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the static Talmud Illuminated site from the scraped content/ files.
 *
 * Output (under site/):
 *   site/style.css                         shared responsive stylesheet (kept as-is, not regenerated)
 *   site/<masechet>/<masechet><p>.html      one page per daf, with prev/next navigation
 *   site/<masechet>/index.html              styled, filterable list of that tractate's pages
 *   site/search.html                        client-side site search
 *   site/search-index.json                  data for the search page
 *   site/manifest.webmanifest               PWA manifest: makes the site installable
 *   site/sw.js                              service worker, for offline reading
 *   site/pwa.js                             registers the service worker
 *   site/offline.html                       shown for a daf that was never opened online
 *   site/icon-*.png                         home-screen icons
 *
 * The PWA files come from src/main/resources/pwa/. Note that the site root's
 * index.html and about.html are maintained by hand in the content repo, not
 * generated here, so they need the PWA head tags added separately -- publish.sh
 * warns when the deployed home page is missing them.
 */
public class MakeSite {

    private static final String BRAND = "Talmud Illuminated";

    /**
     * Subscribe page for the daily email list (hosts the Sender.net form).
     * Rendered as visible text, not just an href: these pages get copied and
     * pasted into Facebook, which strips the markup, so a bare URL is the only
     * form that survives as something a reader can click.
     */
    private static final String SUBSCRIBE_URL = "https://talmudilluminated.com/subscribe/";

    /** Matches --accent-dk in style.css, so the browser chrome blends with the page. */
    private static final String THEME_COLOR = "#6b4420";

    /** Binary and text assets copied verbatim from src/main/resources/pwa/ into site/. */
    private static final String[] PWA_ASSETS = {
            "manifest.webmanifest", "pwa.js",
            "icon-192.png", "icon-512.png", "icon-maskable-512.png"
    };

    public static void main(String[] argv) throws IOException {
        // For testing qa, give it a string to clean
        if (argv.length == 1) {
            System.out.println("Converting this into that:");
            System.out.println(argv[0]);
            System.out.println(qa(argv[0]));
            return;
        }
        String[] masechetNames = BloggerPuller.masechetNames;
        int[] masechetPages = BloggerPuller.masechetPages;
        assert (masechetNames.length == masechetPages.length);

        StringBuilder searchIndex = new StringBuilder("[\n");
        boolean firstEntry = true;

        for (int m = 0; m < masechetNames.length; ++m) {
            String masechet = masechetNames[m].replace(' ', '_');
            String display = displayName(masechetNames[m]);
            String pathToMasechet = "content/" + masechet + "/";
            String pathToMasechetOnSite = "site/" + masechet + "/";
            new File(pathToMasechetOnSite).mkdirs();

            // First pass: collect the valid pages (so prev/next skip missing ones)
            List<Integer> validPages = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            List<String> bodies = new ArrayList<>();
            for (int p = 2; p <= masechetPages[m]; ++p) {
                File pageFile = new File(pathToMasechet + p + ".txt");
                if (!pageFile.exists()) continue;
                List<String> lines = FileUtils.readLines(pageFile, "UTF-8");
                String title = lines.get(0);
                String titleStart = masechetNames[m] + " " + p;
                if (title.toLowerCase().startsWith(titleStart)
                        && title.length() >= titleStart.length()
                        && lines.size() > 1) {
                    StringBuilder bodyContent = new StringBuilder();
                    for (int ln = 1; ln < lines.size(); ++ln) {
                        bodyContent.append(qa(lines.get(ln))).append("\n");
                    }
                    validPages.add(p);
                    titles.add(qa(title));
                    bodies.add(bodyContent.toString());
                }
            }

            // Second pass: write each page with prev/next, plus collect the index + search entries
            StringBuilder indexList = new StringBuilder();
            for (int i = 0; i < validPages.size(); ++i) {
                int p = validPages.get(i);
                String title = titles.get(i);
                Integer prev = i > 0 ? validPages.get(i - 1) : null;
                Integer next = i < validPages.size() - 1 ? validPages.get(i + 1) : null;

                String html = renderPage(masechet, display, p, title, bodies.get(i), prev, next);
                FileUtils.write(new File(pathToMasechetOnSite + masechet + p + ".html"), html, "UTF-8");

                String desc = shortDescription(title, masechetNames[m], p);
                indexList.append("    <li><a href=\"").append(masechet).append(p).append(".html\">")
                        .append("<span class=\"num\">").append(p).append("</span>")
                        .append(escapeHtml(desc)).append("</a></li>\n");

                if (!firstEntry) searchIndex.append(",\n");
                firstEntry = false;
                searchIndex.append("  {\"u\":\"").append(masechet).append('/').append(masechet).append(p)
                        .append(".html\",\"m\":\"").append(jsonEscape(display))
                        .append("\",\"t\":\"").append(jsonEscape(title)).append("\"}");
            }

            writeIndex(masechet, display, validPages.size(), indexList.toString());
        }

        searchIndex.append("\n]\n");
        FileUtils.write(new File("site/search-index.json"), searchIndex.toString(), "UTF-8");
        FileUtils.write(new File("site/search.html"), renderSearchPage(), "UTF-8");
        writePwaFiles(searchIndex.toString());
        System.out.println("Site generated under site/");
    }

    /**
     * Writes the PWA files into site/.
     *
     * The service worker's cache version is a hash of the search index, which
     * lists every page that exists and its title. So the version moves when the
     * content moves and stays put otherwise: readers pick up new dapim on their
     * next visit, while a rebuild that changed nothing neither invalidates
     * caches nor shows up as a diff in the content repo.
     */
    private static void writePwaFiles(String searchIndexJson) throws IOException {
        String cacheVersion = "ti-" + shortHash(searchIndexJson);

        String sw = readResource("sw.js");
        if (!sw.contains("@@CACHE_VERSION@@")) {
            throw new IOException("pwa/sw.js has no @@CACHE_VERSION@@ placeholder to substitute");
        }
        FileUtils.write(new File("site/sw.js"), sw.replace("@@CACHE_VERSION@@", cacheVersion), "UTF-8");

        for (String asset : PWA_ASSETS) {
            copyResource(asset, new File("site/" + asset));
        }

        FileUtils.write(new File("site/offline.html"), renderOfflinePage(), "UTF-8");
        System.out.println("PWA files written (cache version " + cacheVersion + ")");
    }

    /** Shown when a reader opens a daf offline that they never opened online. */
    private static String renderOfflinePage() {
        return documentHead("Offline")
                + header("Offline")
                + "<main>\n"
                + "<h1>Not available offline</h1>\n"
                + "<p>This page has not been opened on this device while connected, so there is "
                + "no copy stored here. Dapim you have already read stay available offline.</p>\n"
                + "<p><a href=\"/index.html\">Go to the home page</a></p>\n"
                + "</main>\n"
                + footer()
                + "</body>\n</html>\n";
    }

    private static String readResource(String name) throws IOException {
        InputStream in = MakeSite.class.getResourceAsStream("/pwa/" + name);
        if (in == null) throw new IOException("missing resource: /pwa/" + name);
        try {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static void copyResource(String name, File target) throws IOException {
        InputStream in = MakeSite.class.getResourceAsStream("/pwa/" + name);
        if (in == null) throw new IOException("missing resource: /pwa/" + name);
        try {
            FileUtils.forceMkdirParent(target);
            OutputStream out = FileUtils.openOutputStream(target);
            try {
                IOUtils.copy(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    /** First 8 hex digits of the SHA-256 of {@code s} -- enough to name a cache. */
    private static String shortHash(String s) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; ++i) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 unavailable", e);
        }
    }

    /** A single daf page. */
    private static String renderPage(String masechet, String display, int p, String title,
                                     String body, Integer prev, Integer next) {
        String prevHtml = prev != null
                ? "<a class=\"prev\" href=\"" + masechet + prev + ".html\">&larr; Daf " + prev + "</a>"
                : "<span class=\"disabled\">&larr;</span>";
        String nextHtml = next != null
                ? "<a class=\"next\" href=\"" + masechet + next + ".html\">Daf " + next + " &rarr;</a>"
                : "<span class=\"disabled\">&rarr;</span>";

        return documentHead(title)
                + header("<a href=\"index.html\">" + escapeHtml(display) + "</a> &rsaquo; Daf " + p)
                + "<main>\n"
                + "<h1>" + escapeHtml(title) + "</h1>\n"
                + "<article class=\"daf\">\n" + body + subscribeLine() + "</article>\n"
                + "<p class=\"ask\"><a href=\"http://mosesai.org\">"
                + "Don't understand a point? Ask MosesAI.org about it.</a></p>\n"
                + "</main>\n"
                + "<nav class=\"pager\">\n  " + prevHtml + "\n"
                + "  <a class=\"up\" href=\"index.html\">All of " + escapeHtml(display) + "</a>\n"
                + "  " + nextHtml + "\n</nav>\n"
                + footer()
                + "</body>\n</html>\n";
    }

    /** A tractate index: filterable list of its pages. */
    private static void writeIndex(String masechet, String display, int count, String listItems)
            throws IOException {
        String html = documentHead(display)
                + header(escapeHtml(display))
                + "<main>\n"
                + "<h1>" + escapeHtml(display) + "</h1>\n"
                + "<p class=\"tractate-intro\">" + count + " pages summarized and illuminated.</p>\n"
                + "<input class=\"filter\" id=\"filter\" type=\"search\" autocomplete=\"off\" "
                + "placeholder=\"Filter " + escapeHtml(display) + " pages…\" "
                + "oninput=\"filterList(this.value)\">\n"
                + "<ul class=\"daf-list\" id=\"daf-list\">\n" + listItems + "</ul>\n"
                + "</main>\n"
                + footer()
                + "<script>\n"
                + "function filterList(q){q=q.toLowerCase();"
                + "document.querySelectorAll('#daf-list li').forEach(function(li){"
                + "li.style.display=li.textContent.toLowerCase().indexOf(q)>-1?'':'none';});}\n"
                + "</script>\n"
                + "</body>\n</html>\n";
        FileUtils.write(new File("site/" + masechet + "/index.html"), html, "UTF-8");
    }

    /** Site-wide search page (client-side, uses search-index.json). */
    private static String renderSearchPage() {
        return documentHead("Search")
                + header("Search")
                + "<main>\n"
                + "<h1>Search</h1>\n"
                + "<input class=\"filter\" id=\"q\" type=\"search\" autocomplete=\"off\" autofocus "
                + "placeholder=\"Search all tractates by page title…\">\n"
                + "<p class=\"search-meta\" id=\"meta\">Loading index…</p>\n"
                + "<ul id=\"results\"></ul>\n"
                + "</main>\n"
                + footer()
                + "<script>\n"
                + "var data=[];var meta=document.getElementById('meta');\n"
                + "fetch('/search-index.json').then(function(r){return r.json();}).then(function(j){"
                + "data=j;meta.textContent=data.length+' pages indexed. Type to search.';})"
                + ".catch(function(){meta.textContent='Could not load the search index.';});\n"
                + "var q=document.getElementById('q'),out=document.getElementById('results');\n"
                + "q.addEventListener('input',function(){var s=q.value.toLowerCase().trim();out.innerHTML='';"
                + "if(s.length<2){meta.textContent=data.length+' pages indexed. Type to search.';return;}"
                + "var hits=data.filter(function(d){return d.t.toLowerCase().indexOf(s)>-1;});"
                + "meta.textContent=hits.length+' result'+(hits.length===1?'':'s')+'.';"
                + "hits.slice(0,200).forEach(function(d){var li=document.createElement('li');"
                + "li.innerHTML='<a href=\"/'+d.u+'\"><span class=\"tractate\">'+d.m+'</span> &mdash; '+"
                + "d.t.replace(/</g,'&lt;')+'</a>';out.appendChild(li);});});\n"
                + "</script>\n"
                + "</body>\n</html>\n";
    }

    /**
     * The opening of every generated page, down to the end of &lt;head&gt;.
     *
     * The PWA tags live here so that any page can be the one a reader installs
     * from, or lands on from a search engine: the manifest makes the site
     * installable, and pwa.js registers the worker that keeps visited dapim
     * readable offline. pwa.js is a separate file rather than an inline script
     * because the same bytes would otherwise be repeated in ~2,400 pages.
     */
    private static String documentHead(String title) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + escapeHtml(title) + " | " + BRAND + "</title>\n"
                + "<link rel=\"stylesheet\" href=\"/style.css\">\n"
                + "<link rel=\"manifest\" href=\"/manifest.webmanifest\">\n"
                + "<meta name=\"theme-color\" content=\"" + THEME_COLOR + "\">\n"
                + "<link rel=\"apple-touch-icon\" href=\"/icon-192.png\">\n"
                + "<meta name=\"apple-mobile-web-app-title\" content=\"Talmud\">\n"
                + "<script src=\"/pwa.js\" defer></script>\n"
                + "</head>\n<body>\n";
    }

    private static String header(String crumbTail) {
        return "<header class=\"site-header\">\n"
                + "  <a class=\"brand\" href=\"/index.html\">📖 " + BRAND + "</a>\n"
                + "  <nav class=\"breadcrumb\"><a href=\"/index.html\">Home</a> &rsaquo; "
                + crumbTail + "</nav>\n"
                + "</header>\n";
    }

    /**
     * The daily-email offer. It is the last element inside &lt;article&gt;, directly
     * after the summary, so that selecting the daf text and pasting it elsewhere
     * (Facebook, email) carries the offer with it.
     */
    private static String subscribeLine() {
        return "<p class=\"subscribe\">A page of Talmud in your inbox every morning &mdash; "
                + "subscribe: <a href=\"" + SUBSCRIBE_URL + "\">" + SUBSCRIBE_URL + "</a></p>\n";
    }

    private static String footer() {
        return "<footer class=\"site-footer\">\n"
                + "  <a href=\"/index.html\">Home</a> &middot; <a href=\"/search.html\">Search</a> "
                + "&middot; <a href=\"http://mosesai.org\">MosesAI</a> "
                + "&middot; <a href=\"/about.html\">About</a><br>\n"
                + "  Summaries of every page of the Talmud Bavli, illuminated with the world's best art.\n"
                + "</footer>\n";
    }

    /** "Shabbat 2 - From the house to the street" -> "From the house to the street". */
    private static String shortDescription(String title, String masechetName, int p) {
        String prefix = masechetName + " " + p;
        if (title.toLowerCase().startsWith(prefix.toLowerCase())) {
            String rest = title.substring(prefix.length());
            rest = rest.replaceFirst("^[\\s\\-\\u2013:]+", "");
            if (!rest.isEmpty()) return rest;
        }
        return title;
    }

    /** "bava kamma" -> "Bava Kamma". */
    private static String displayName(String masechetName) {
        StringBuilder sb = new StringBuilder();
        boolean cap = true;
        for (char c : masechetName.toCharArray()) {
            if (c == ' ') { cap = true; sb.append(c); }
            else { sb.append(cap ? Character.toUpperCase(c) : c); cap = false; }
        }
        return sb.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ").replace("\r", " ").replace("\t", " ");
    }

    /**
     * Clean up MS Word characters (smart quotes)
     * Based on https://stackoverflow.com/questions/2826191/converting-ms-word-curly-quotes-and-apostrophes
     */
    private static String qa(String textToCheck) {
        return textToCheck.replaceAll("[\\u2018\\u2019]", "'")
                .replaceAll("[\\u201C\\u201D]", "\"")
                .replaceAll("–", "-");
    }
}
