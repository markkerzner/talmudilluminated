package com.shmsoft.t_i;

import static org.junit.Assert.assertTrue;

public class BloggerPullerTest {
    //@Test
    public void getMasechetBrachot() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPages("brachot", 63);
        assertTrue(true);
    }

    //@Test
    public void getMasechetBavaKamma() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPages("bava kamma", 119);
        assertTrue(true);
    }

    //@Test
    public void getPage() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPage("brachot", 57);
        assertTrue(true);
    }

    //@Test
    public void getPageLongMasechetName() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        bloggerPuller.getPage("bava kamma", 57);
        assertTrue(true);
    }
}