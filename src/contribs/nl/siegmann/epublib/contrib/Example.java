/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.siegmann.epublib.contrib;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubWriter;

/**
 *
 * @author Herve
 */
public class Example {
   private static InputStream getResource(String path) {
      URL url = Example.class.getResource(path);
      return Example.class.getResourceAsStream(path);
   }

   private static Resource getResource(String path, String href) throws IOException {
      return new Resource(getResource(path), href);
   }

   public static void main(String[] args) {
      try {
         // Create new Book
         Book book = new Book();
         Metadata metadata = book.getMetadata();

         // Set the title
         metadata.addTitle("Epublib test book 1");

         // Add an Author
         metadata.addAuthor(new Author("Joe", "Tester"));

         // Set cover image
         book.setCoverImage(getResource("book1/cover.png", "cover.png"));

         // Add Chapter 1
         book.addSection("Introduction", getResource("book1/chapter1.html", "chapter1.html"));

         // Add css file
         book.getResources().add(getResource("book1/book1.css", "book1.css"));

         // Add Chapter 2
         TOCReference chapter2 = book.addSection("Second Chapter", getResource("book1/chapter2.html", "chapter2.html"));

         // Add image used by Chapter 2
         book.getResources().add(getResource("book1/flowers_320x240.jpg", "flowers.jpg"));

         // Add Chapter2, Section 1
         book.addSection(chapter2, "Chapter 2, section 1", getResource("book1/chapter2_1.html", "chapter2_1.html"));

         // Add Chapter 3
         book.addSection("Conclusion", getResource("book1/chapter3.html", "chapter3.html"));

         // Create EpubWriter
         EpubWriter epubWriter = new EpubWriter();

         // Write the Book as Epub
         epubWriter.write(book, new FileOutputStream("test1_book1.epub"));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
