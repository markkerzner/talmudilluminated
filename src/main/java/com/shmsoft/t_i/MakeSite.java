package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MakeSite {

    public static void main(String[] argv) {
        String[] masechetNames = BloggerPuller.masechetNames;
        int[] masechetPages = BloggerPuller.masechetPages;
        assert (masechetNames.length == masechetPages.length);
        StringBuilder index = new StringBuilder();
        for (int m = 0; m < masechetNames.length; ++m) {
            String masechet = masechetNames[m].replace(' ', '_');
            String pathToMasechet = "content/" + masechet + "/";
            String pathToMasechetOnSite = "site/" + masechet + "/";
            new File(pathToMasechetOnSite).mkdirs();
            try {
                for (int p = 2; p <= masechetPages[m]; ++p) {
                    File pageFile = new File(pathToMasechet + p + ".txt");
                    List<String> lines = FileUtils.readLines(pageFile, "UTF-8");
                    String title = lines.get(0);
                    String titleStart = masechetNames[m] + " " + p;
                    if (title.toLowerCase().startsWith(titleStart) &&
                            title.length() >= titleStart.length() &&
                            lines.size() > 1) {
                        // valid page, make html
                        StringBuffer bodyContent = new StringBuffer();
                        for (int lineNumber = 1; lineNumber < lines.size(); ++lineNumber) {
                            bodyContent.append(lines.get(lineNumber)).append("\n");
                        }
                        String html = "<html>" + "\n" +
                                "<title>" + title + "</title>" + "\n" +
                                "<body>\n" +
                                "<h1>" + title + "</h1>" + "\n" +
                                bodyContent.toString() +
                                "</body>\n" +
                                "</html>";
                        File outputFile = new File(pathToMasechetOnSite + masechet + p + ".html");
                        FileUtils.write(outputFile, html, "UTF-8");
                        index.append(getIndexEntry(title));
                    } else {
                        // invalid
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            writeIndex(index.toString());
        }
    }

    private static String getIndexEntry(String title) {
        return title + "\n";
    }
    // TODO finish up collecting and writing index
    private static void writeIndex(String index) {
        System.out.print(index);
    }
}

