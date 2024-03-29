package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class QA {
    public static void main(String [] argv) {
        int missingCount = 0;
        int totalPages = 0;
        String[] masechetNames = BloggerPuller.masechetNames;
        int[] masechetPages = BloggerPuller.masechetPages;
        assert (masechetNames.length == masechetPages.length);
        for (int m = 0; m < masechetNames.length; ++m) {
            totalPages += (masechetPages[m] - 1); // we start with page 2
            String masechet = masechetNames[m].replace(' ', '_');
            String pathToMasechet = "content/" + masechet + "/";
            try {
                for (int p = 2; p <= masechetPages[m]; ++p) {
                    File pageFile = new File(pathToMasechet + p + ".txt");
                    List<String> lines = FileUtils.readLines(pageFile, "UTF-8");
                    String title = lines.get(0).toLowerCase();
                    String titleStart = masechetNames[m] + " " + p;
                    if (title.startsWith(titleStart) &&
                            title.length() >= titleStart.length() &&
                            lines.size() > 1) {
                        // valid
                    } else {
                        // invalid
                        System.out.println(titleStart);
                        ++missingCount;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Missing pages count: " + missingCount + " of " + totalPages);
        System.out.println("Missing percent: " + 100 * missingCount / totalPages + "%");
    }
}
