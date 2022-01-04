package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MakeSite {
    public static void main(String[] argv) throws IOException {
        // For testing qa, give it a string to clean
        if (argv.length == 1) {
            System.out.println("Converting this into that:");
            System.out.println(argv[0]);
            System.out.println(qa(argv[0]));
            return;
        }
        String[] masechetNames = BloggerPuller.masechetNames;
        int[] masechetPages = BloggerPuller.masechetPages;
        assert (masechetNames.length == masechetPages.length);
        String fbMessenger = FileUtils.readFileToString(
                new File("doc/fb-messenger.txt"), StandardCharsets.UTF_8);
        for (int m = 0; m < masechetNames.length; ++m) {
            String masechet = masechetNames[m].replace(' ', '_');
            String pathToMasechet = "content/" + masechet + "/";
            String pathToMasechetOnSite = "site/" + masechet + "/";
            new File(pathToMasechetOnSite).mkdirs();
            ArrayList<String> index = new ArrayList<String>();
            index.add("<html>");
            index.add(fbMessenger);
            for (int p = 2; p <= masechetPages[m]; ++p) {
                File pageFile = new File(pathToMasechet + p + ".txt");
                List<String> lines = FileUtils.readLines(pageFile, "UTF-8");
                String title = lines.get(0);
                String titleStart = masechetNames[m] + " " + p;
                if (title.toLowerCase().startsWith(titleStart) &&
                        title.length() >= titleStart.length() &&
                        lines.size() > 1) {
                    // valid page, make html
                    title = qa(title);
                    StringBuffer bodyContent = new StringBuffer();
                    for (int lineNumber = 1; lineNumber < lines.size(); ++lineNumber) {
                        String textLine = qa(lines.get(lineNumber));
                        bodyContent.append(textLine).append("\n");
                    }
                    String searchString = "<br/>\n" +
                            "<br/>\n" +
                            "<a href=\"https://mkerzner.blogspot.com/search?q=" +
                            masechetNames[m] + "+" + p +
                            "\">Leave a comment</a>";
                    String html = "<html>" + "\n" +
                            "<title>" + title + "</title>" + "\n" +
                            fbMessenger + "\n" +
                            "<body>\n" +
                            "<h1>" + title + "</h1>" + "\n" +
                            bodyContent +
                            searchString +
                            "</body>\n" +
                            "</html>";
                    File outputFile = new File(pathToMasechetOnSite + masechet + p + ".html");
                    FileUtils.write(outputFile, html, "UTF-8");
                    String indexLine = "<a href=\"" + masechet + p + ".html\">" + title + "<br/></a>";
                    index.add(indexLine);
                } else {
                    // invalid
                }
            }
            index.add("</html>");
            writeIndex(masechet, index);
        }
    }

    private static void writeIndex(String masechet, ArrayList<String> index) throws IOException {
        FileUtils.writeLines(new File("site/" + masechet + "/index.html"), "UTF-8", index);
    }

    /**
     * Clean up MS Work characters (smart quotes)
     * Based on this https://stackoverflow.com/questions/2826191/converting-ms-word-curly-quotes-and-apostrophes
     * @param textToCheck
     * @return cleaned text
     */
    private static String qa(String textToCheck) {
        return textToCheck.replaceAll("[\\u2018\\u2019]", "'")
                .replaceAll("[\\u201C\\u201D]", "\"")
                .replaceAll("–", "-");
    }
}
