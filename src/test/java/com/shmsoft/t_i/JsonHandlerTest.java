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

}