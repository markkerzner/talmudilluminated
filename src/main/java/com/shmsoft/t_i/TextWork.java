package com.shmsoft.t_i;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextWork {
    public static void main(String[] args) throws IOException, TikaException, SAXException {
        System.out.println("Hello World!");
        TextWork textWork = new TextWork();
        textWork.testTikaExtractText();
    }
    public void testTikaExtractText() throws IOException, TikaException, SAXException {
        Parser parser = new AutoDetectParser();
        ContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();
        InputStream stream = new FileInputStream(new File("data/01.pdf"));
        parser.parse(stream, handler, metadata, context);
        String contentString = handler.toString();
        System.out.println(contentString);

    }
}
