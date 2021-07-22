package com.shmsoft.t_i;

import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.apache.htrace.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;

public class JsonHandler {
    private static String TITLE = "title";
    private static String CONTENT = "content";
    String[] fieldsValues = new String[2];

    public static void main(String[] argv) throws Exception {
        String json = "{ \"color\" : \"Black\", \"type\" : \"FIAT\" }";
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);

        String color = jsonNode.get("color").asText();
        System.out.println(color);
    }

    public JsonNode parseJson(String jsonString) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        return jsonNode;
    }
    public String[] collectTheseFields(JsonNode root) {
        if (root.isObject()) {
            Iterator<String> fieldNames = root.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (TITLE.equalsIgnoreCase(fieldName)) {
                    fieldsValues[0] = root.get(fieldName).asText();
                }
                if (CONTENT.equalsIgnoreCase(fieldName)) {
                    fieldsValues[1] = root.get(fieldName).asText();
                }
                JsonNode fieldValue = root.get(fieldName);
                collectTheseFields(fieldValue);
            }
        } else if (root.isArray()) {
            ArrayNode arrayNode = (ArrayNode) root;
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode arrayElement = arrayNode.get(i);
                collectTheseFields(arrayElement);
            }
        } else {
            // JsonNode root represents a single value field - do something with it.

        }
        return fieldsValues;
    }

}
