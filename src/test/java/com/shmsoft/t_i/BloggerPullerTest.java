package com.shmsoft.t_i;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BloggerPullerTest {
    @Test
    public void getPage() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        String answer = bloggerPuller.getPage("brachot", 2);
        System.out.println(answer);
        assertTrue(true);
    }
}