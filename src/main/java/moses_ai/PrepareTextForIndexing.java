package moses_ai;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class PrepareTextForIndexing {
    private static final String INPUT_DIR = "./site";

    private static final String OUTPUT_DIR_PARAGRAPHS = "./text-for-indexing_paragraphs";
    private static final String OUTPUT_DIR_PAGES = "./text-for-indexing_pages";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Need parameters");
            System.exit(0);
        }
        if ("paragraphs=yes".equals(args[0])) {
            breakParagraphs();
        } else {
            noBreakParagraphs();
        }
    }

    public static void breakParagraphs() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR_PARAGRAPHS)); // Create output directory if it does not exist

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
                                String paragraphFile = OUTPUT_DIR_PARAGRAPHS + "/" + file.getName() + "-paragraph-" + paragraphCount + ".txt";
                                String paragraph = paragraphs[i];
                                if (paragraph.length() > 3 && // skip empty paragraph
                                        !(paragraph.startsWith("Art")) && // skip description of art in summary
                                        !(paragraph.startsWith(" Art")) && // skip description of art in summary
                                        !paragraph.startsWith("Leave a comment") // skip "Leave a comment"
                                ) {
                                    FileUtils.writeStringToFile(new File(paragraphFile), paragraph, "UTF-8");
                                    paragraphCount++;
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

    public static void noBreakParagraphs() {
        try {
            Files.createDirectories(Paths.get(OUTPUT_DIR_PAGES)); // Create output directory if it does not exist

            File folder = new File(INPUT_DIR);
            File[] listOfDir = folder.listFiles();

            for (File dir : listOfDir) {
                if (dir.isDirectory()) {
                    File[] listOfFiles = dir.listFiles();
                    for (File file : listOfFiles) {
                        if (file.isFile() && file.getName().endsWith(".html")) {
                            Document doc = Jsoup.parse(file, "UTF-8");
                            String text = doc.body().text(); // extract text
                            String paragraphFile = OUTPUT_DIR_PAGES + "/" + file.getName() + ".txt";
                            if (text.length() > 3) {
                                FileUtils.writeStringToFile(new File(paragraphFile), text, "UTF-8");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


