package com.shmsoft.t_i;


import org.apache.commons.io.IOUtils;

import java.net.URL;
/**
 * Pulls Blogger content
 * https://developers.google.com/blogger/docs/3.0/reference/posts/get
 */
public class BloggerPuller {
    private static final String CODE_BLOG_ID = "3539559106913091506"; // The BlogId for the http://mkerzner.blogspot.com/ blog.
    private static final String GOOGLE_API_KEY = System.getenv("GOOGLE_API_KEY");

    public void pullForLabel(String label) {
        System.out.println("Pulling for label " + label);
    }

    public String getPage(String masechet, int pageNumber) {
        String urlStr = "https://www.googleapis.com/blogger/v3/blogs/" +
                CODE_BLOG_ID +
                "/posts/search?q=" + masechet + "+" + pageNumber +
                "&key=" + GOOGLE_API_KEY;
        String pageContent = "";
        try {
            URL url = new URL(urlStr);
            pageContent = IOUtils.toString(url, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pageContent;
    }
}
