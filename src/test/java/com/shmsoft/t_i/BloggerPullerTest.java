package com.shmsoft.t_i;

import static org.junit.Assert.assertTrue;

public class BloggerPullerTest {
    //@Test
    public void getPage() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        String answer = bloggerPuller.getPage("brachot", 2);
        System.out.println(answer);
        assertTrue(true);
    }
    //@Test
    public void getMasechet() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPages("brachot", 263);
        assertTrue(true);
    }
}