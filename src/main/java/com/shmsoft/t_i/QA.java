package com.shmsoft.t_i;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class QA {
    public static void main(String [] argv) {
        int missingCount = 0;
        String[] masechetNames = BloggerPuller.masechetNames;
        int[] masechetPages = BloggerPuller.masechetPages;
        assert (masechetNames.length == masechetPages.length);
        for (int m = 0; m < masechetNames.length; ++m) {
            String pathToMasechet = "content/" + masechetNames[m] + "/";
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
                        System.out.println("Missing " + titleStart);
                        ++missingCount;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Total missing pages count: " + missingCount);
        }
    }
}
