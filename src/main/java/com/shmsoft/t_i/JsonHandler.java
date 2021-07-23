package com.shmsoft.t_i;

import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;
import org.apache.htrace.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;

public class JsonHandler {
    private static String TITLE = "title";
    private static String CONTENT = "content";
    String[] fieldsValues = new String[2];
    private int foundResults = 0;
    private String searchTitle;
    public JsonHandler(String searchTitle) {
        this.searchTitle = searchTitle;
    }
    public JsonNode parseJson(String jsonString) throws Exception {
        JsonNode jsonNode = new ObjectMapper().readTree(jsonString);
        return jsonNode;
    }
    public String[] collectTheseFields(JsonNode root) {
        if (root.isObject()) {
            Iterator<String> fieldNames = root.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (TITLE.equals(fieldName) && root.get(fieldName).asText().startsWith(searchTitle)) {
                    // TODO Fix the bad assumption here that 'title' is follow by 'content'
                    // TODO Fix the bad assumption here that 'title' is follow by 'content'
                    fieldsValues[0] = root.get(fieldName).asText();
                    fieldName = fieldNames.next();
                    fieldsValues[1] = root.get(fieldName).asText();
                    ++foundResults;
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
            // skip it
        }
        return fieldsValues;
    }
    public boolean isValid() {
        return foundResults == 1;
    }
}
