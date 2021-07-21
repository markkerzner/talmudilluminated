package com.shmsoft.t_i;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BloggerPullerTest {
    @Test
    public void testGetPage() {
        BloggerPuller bloggerPuller = new BloggerPuller();
        String answer = bloggerPuller.getPage("brachot", 2);
        System.out.println(answer);
        assertTrue(true);
    }
}