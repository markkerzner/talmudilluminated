package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;
import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class JsonHandlerTest {
    //@Test
    public void parseJson1() {
        String titleSearch = "Yoma 58";
        try {
            String jsonString = FileUtils.readFileToString(new File("data/searchResult1.json"), "UTF-8");
            JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
            JsonHandler jsonHandler = new JsonHandler(titleSearch);
            String [] values = jsonHandler.collectTheseFields(jsonNode);
            assertTrue(values[0].startsWith(titleSearch));
            assertTrue(jsonHandler.isValid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void parseJson2() {
        String titleSearch = "Yoma 3";
        try {
            String jsonString = FileUtils.readFileToString(new File("data/searchResult2.json"), "UTF-8");
            JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
            JsonHandler jsonHandler = new JsonHandler(titleSearch);
            String [] values = jsonHandler.collectTheseFields(jsonNode);
            assertTrue(values[0].startsWith(titleSearch));
            assertTrue(jsonHandler.isValid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void parseJson3() {
        String masechet = "Yoma";
        int pageNumber = 58;
        String titleSearch = masechet + " " + pageNumber;
        try {
            String jsonString = new BloggerPuller().getSearchResultAsJsonString(masechet, pageNumber);
            JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
            JsonHandler jsonHandler = new JsonHandler(titleSearch);
            String [] values = jsonHandler.collectTheseFields(jsonNode);
            assertTrue(values[0].startsWith(titleSearch));
            assertTrue(jsonHandler.isValid());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void parseJsonBrachot() {
        String masechet = "Brachot";
        for (int pageNumber = 2; pageNumber <= 64; ++pageNumber) {
            System.out.println("parseJsonBrachot, pageNumber = " + pageNumber);
            String titleSearch = masechet + " " + pageNumber;
            try {
                String jsonString = new BloggerPuller().getSearchResultAsJsonString(masechet, pageNumber);
                JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
                JsonHandler jsonHandler = new JsonHandler(titleSearch);
                String[] values = jsonHandler.collectTheseFields(jsonNode);
                assertTrue(values[0].startsWith(titleSearch));
                assertTrue(jsonHandler.isValid());
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //@Test
    public void parseJsonShabbat() {
        String masechet = "Shabbat";
        for (int pageNumber = 2; pageNumber <= 146; ++pageNumber) {
            System.out.println("parseJsonShabbat, pageNumber = " + pageNumber);
            String titleSearch = masechet + " " + pageNumber;
            try {
                String jsonString = new BloggerPuller().getSearchResultAsJsonString(masechet, pageNumber);
                JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
                JsonHandler jsonHandler = new JsonHandler(titleSearch);
                String[] values = jsonHandler.collectTheseFields(jsonNode);
                assertTrue(values[0].startsWith(titleSearch));
                assertTrue(jsonHandler.isValid());
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}