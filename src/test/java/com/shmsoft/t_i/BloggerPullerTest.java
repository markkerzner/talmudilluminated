package com.shmsoft.t_i;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BloggerPullerTest {
    //@Test
    public void getMasechet() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPages("brachot", 63);
        assertTrue(true);
    }

    @Test
    public void getPage() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPage("brachot", 57);
        assertTrue(true);
    }
}