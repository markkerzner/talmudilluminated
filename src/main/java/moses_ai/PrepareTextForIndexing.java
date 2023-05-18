package moses_ai;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PrepareTextForIndexing {
    private static final String INPUT_DIR = "./content";
    private static final String OUTPUT_DIR = "./text-for-indexing";

    public static void main(String[] args) {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR)); // Create output directory if it does not exist

            File folder = new File(INPUT_DIR);
            File[] listOfFiles = folder.listFiles();

            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    Document doc = Jsoup.parse(file, "UTF-8");
                    String text = doc.body().text(); // extract text
                    String[] paragraphs = text.split("\\n+"); // split into paragraphs

                    // output each paragraph into a separate file in the output directory
                    for (int i = 0; i < paragraphs.length; i++) {
                        String paragraphFile = OUTPUT_DIR + "/" + file.getName() + "-paragraph-" + i + ".txt";
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(paragraphFile))) {
                            writer.write(paragraphs[i]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


