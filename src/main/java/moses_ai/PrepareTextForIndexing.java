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
    private static final String INPUT_DIR = "./site";

    // private static final String INPUT_DIR = "./content";
    private static final String OUTPUT_DIR = "./text-for-indexing";

    public static void main(String[] args) {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR)); // Create output directory if it does not exist

            File folder = new File(INPUT_DIR);
            File[] listOfDir = folder.listFiles();

            for (File dir : listOfDir) {
                if (dir.isDirectory()) {
                    File[] listOfFiles = dir.listFiles();
                    for (File file : listOfFiles)
                        if (file.isFile() && file.getName().endsWith(".html")) {
                            Document doc = Jsoup.parse(file, "UTF-8");
                            String htmlWithLineBreaks = doc.html().replaceAll("(?i)<br[^>]*>", "br2n");
                            Document docWithLineBreaks = Jsoup.parse(htmlWithLineBreaks, "UTF-8");
                            String text = docWithLineBreaks.body().text(); // extract text
                            text = text.replaceAll("br2n", "\n");
                            String[] paragraphs = text.split("\n+"); // split into paragraphs

                            // output each paragraph into a separate file in the output director
                            int paragraphCount = 0;
                            for (int i = 0; i < paragraphs.length; i++) {
                                String paragraphFile = OUTPUT_DIR + "/" + file.getName() + "-paragraph-" + paragraphCount + ".txt";
                                try (BufferedWriter writer = new BufferedWriter(new FileWriter(paragraphFile))) {
                                    String paragraph = paragraphs[i];
                                    if (paragraph.length() > 3 && // skip empty paragraph
                                            !(paragraph.startsWith("Art")) && // skip description of art in summary
                                            !(paragraph.startsWith(" Art")) && // skip description of art in summary
                                            !paragraph.startsWith("Leave a comment") // skip "Leave a comment"
                                    ) {
                                        System.out.println("Writing: " + paragraph);
                                        writer.write(paragraph);
                                        paragraphCount++;
                                    }
                                }
                            }
                        }
                }
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}


