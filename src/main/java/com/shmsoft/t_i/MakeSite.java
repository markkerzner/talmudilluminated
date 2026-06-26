package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
 */
public class MakeSite {

    private static final String BRAND = "Talmud Illuminated";

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
        System.out.println("Site generated under site/");
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

        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + escapeHtml(title) + " | " + BRAND + "</title>\n"
                + "<link rel=\"stylesheet\" href=\"/style.css\">\n"
                + "</head>\n<body>\n"
                + header("<a href=\"index.html\">" + escapeHtml(display) + "</a> &rsaquo; Daf " + p)
                + "<main>\n"
                + "<h1>" + escapeHtml(title) + "</h1>\n"
                + "<article class=\"daf\">\n" + body + "</article>\n"
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
        String html = "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>" + escapeHtml(display) + " | " + BRAND + "</title>\n"
                + "<link rel=\"stylesheet\" href=\"/style.css\">\n"
                + "</head>\n<body>\n"
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
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n<head>\n"
                + "<meta charset=\"utf-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "<title>Search | " + BRAND + "</title>\n"
                + "<link rel=\"stylesheet\" href=\"/style.css\">\n"
                + "</head>\n<body>\n"
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

    private static String header(String crumbTail) {
        return "<header class=\"site-header\">\n"
                + "  <a class=\"brand\" href=\"/index.html\">📖 " + BRAND + "</a>\n"
                + "  <nav class=\"breadcrumb\"><a href=\"/index.html\">Home</a> &rsaquo; "
                + crumbTail + "</nav>\n"
                + "</header>\n";
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
