package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;
import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonHandlerTest {
    @Test
    public void parseJson() {
        try {
            String jsonString = "{ \"color\" : \"Black\", \"type\" : \"FIAT\" }";
            JsonNode jsonNode = new JsonHandler().parseJson(jsonString);
            String color = jsonNode.get("color").asText();
            assertEquals(color, "Black");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void parseJson1() {
        System.out.println("Running parseJson1");
        try {
            String jsonString = FileUtils.readFileToString(new File("data/searchResult1.json"), "UTF-8");
            JsonNode jsonNode = new JsonHandler().parseJson(jsonString);
            String [] values = new JsonHandler().collectTheseFields(jsonNode);
            assertTrue(values[0].startsWith("Yoma 58"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void parseJson2() {
        System.out.println("Running parseJson2");
        try {
            String jsonString = FileUtils.readFileToString(new File("data/searchResult2.json"), "UTF-8");
            JsonNode jsonNode = new JsonHandler().parseJson(jsonString);
            String [] values = new JsonHandler().collectTheseFields(jsonNode);
            assertTrue(values[0].startsWith("Yoma 3"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}