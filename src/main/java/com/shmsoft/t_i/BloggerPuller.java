package com.shmsoft.t_i;


import org.apache.commons.io.IOUtils;
import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;

/**
 * Pulls Blogger content
 * https://developers.google.com/blogger/docs/3.0/reference/posts/get
 */
public class BloggerPuller {
    private static final String CODE_BLOG_ID = "3539559106913091506"; // The BlogId for the http://mkerzner.blogspot.com/ blog.
    private int foundExactlyOne = 0;
    private int foundTooMany = 0;

    public void pullForLabel(String label) {
        System.out.println("Pulling for label " + label);
    }

    public String getPage(String masechet, int pageNumber) {
        String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");
        String urlStr = "https://www.googleapis.com/blogger/v3/blogs/" +
                CODE_BLOG_ID +
                "/posts/search?q=" +
                masechet + "+" + pageNumber +
                "&key=" + GOOGLE_API_KEY;
        try {
            String titleSearch = masechet + " " + pageNumber;
            URL url = new URL(urlStr);
            String jsonString = IOUtils.toString(url, "UTF-8");
            JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
            JsonHandler jsonHandler = new JsonHandler(titleSearch);
            String [] values = jsonHandler.collectTheseFields(jsonNode);
            if (values[0].startsWith(titleSearch)) ++foundExactlyOne;
            if (!jsonHandler.isValid()) ++foundTooMany;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public void getPages(String masechet, int numberPages)  {
        try {
            for (int page = 2; page <= numberPages; ++page) {
                getPage(masechet, page);
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
