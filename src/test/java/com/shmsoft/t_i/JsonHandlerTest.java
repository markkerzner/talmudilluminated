package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;
import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class JsonHandlerTest {
    @Test
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
    @Test
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
    @Test
    public void parseJson3() {
        String masechet = "yoma";
        int pageNumber = 58;
        String titleSearch = masechet + " " + pageNumber;
        titleSearch = "Yoma 58";
        try {
            //String jsonString = FileUtils.readFileToString(new File("data/searchResult1.json"), "UTF-8");
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

    @Test
    public void parseJson4() {
        try {
            String masechet = "yoma";
            int pageNumber = 58;
            String jsonStringWeb = new BloggerPuller().getSearchResultAsJsonString(masechet, pageNumber);
            FileUtils.write(new File("data/searchOutput.json"), jsonStringWeb,"UTF-8");
            String jsonStringFile = FileUtils.readFileToString(new File("data/searchResult3.json"), "UTF-8");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}