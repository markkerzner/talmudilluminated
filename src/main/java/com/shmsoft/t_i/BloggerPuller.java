package com.shmsoft.t_i;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Pulls Blogger content
 * https://developers.google.com/blogger/docs/3.0/reference/posts/get
 */
public class BloggerPuller {
    private static final String CODE_BLOG_ID = "3539559106913091506"; // The BlogId for the http://mkerzner.blogspot.com/ blog.
    public static final String[] masechetNames = {"brachot", "shabbat", "eruvin", "pesachim", "shekalim", "yoma",
            "sukkah", "beitzah", "rosh hashanah", "taanit", "megillah", "moed katan", "chagigah", "yebamot", "ketubot",
            "nedarim", "nazir", "sotah", "gittin", "kiddushin", "bava kamma", "bava metzia", "bava batra",
            "sanhedrin", "makkot", "shevuot", "avoda zarah", "horayot", "zevachim", "menachot", "chullin", "bechorot",
            "arachin", "temurah", "keritot", "meilah", "niddah"};
    public static final int[] masechetPages = {64, 157, 105, 121, 22, 88,
            56, 40, 35, 31, 32, 29, 27, 122, 112,
            91, 66, 49, 90, 82, 119, 119, 176,
            113, 24, 49, 76, 14, 120, 110, 142, 61,
            34, 34, 28, 37, 73};
    private int foundExactlyOne = 0;
    private int foundTooMany = 0;

    public static void main(String[] argv) {
        assert (masechetNames.length == masechetPages.length);
        for (int m = 0; m < masechetNames.length; ++m) {
            System.out.println("Pulling masechet " + masechetNames[m]);
            BloggerPuller bloggerPuller = new BloggerPuller();
            bloggerPuller.getPages(masechetNames[m], masechetPages[m]);
        }
    }

    public void getPage(String masechet, int pageNumber) {
        String[] values = {"", ""}; // title, content
        try {
            String jsonResultString = getSearchResultAsJsonString(masechet, pageNumber);
            JsonNode jsonNode = new ObjectMapper().readTree(jsonResultString);
            String titleSearch = makeTitleSearch(masechet, pageNumber);
            JsonHandler jsonHandler = new JsonHandler(titleSearch);
            values = jsonHandler.collectTheseFields(jsonNode);
            if (values[0].startsWith(titleSearch)) ++foundExactlyOne;
            if (!jsonHandler.isValid()) ++foundTooMany;
            String pathToStore = "content/" + masechet.replace(' ', '_') + "/";
            new File(pathToStore).mkdirs();
            FileUtils.write(new File(pathToStore + pageNumber + ".txt"),
                    values[0] + "\n" + values[1],
                    "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getPages(String masechet, int numberPages) {
        try {
            for (int page = 2; page <= numberPages; ++page) {
                getPage(masechet, page);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getSearchResultAsJsonString(String masechet, int pageNumber) {
        try {
            String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");
            String urlStr = "https://www.googleapis.com/blogger/v3/blogs/" +
                    CODE_BLOG_ID +
                    "/posts/search?q=" +
                    masechet.replace(' ', '+') +
                    "+" + pageNumber +
                    "&key=" + GOOGLE_API_KEY;
            URL url = new URL(urlStr);
            return IOUtils.toString(url, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String makeTitleSearch(String masechet, int pageNumber) {
        return masechet + " " + pageNumber;
    }
}
