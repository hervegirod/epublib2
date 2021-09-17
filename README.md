# epublib2
Epublib2 is a copy of the https://github.com/psiegman/epublib java library for reading/writing/manipulating epub files, with
a few modifications from the original library.

Contrary to the original library, this library is not ready-to-use on Android devices, but a lot of dependencies
of the original library (necessary when using the library on Android) have been removed.

## Why not having just forked the original library?
The structure of the project has too many changes compared to the original project, and the reason because it does not answer to the same kind of requirements than the original library, therefore I don't think that it would be valid for example to merge changes made in  this project to the original project.

## Changes compared to the original
### 1.0
* Removed dependency to jazzlib (useful only on Android platforms)
* Removed dependency to xmlpull (is was difficult to configure because it needed a service provider)
* Removed dependency to Apache commons.lang
* Separate the viewer and tools library from the core library
* creation of a Netbeans project (not using maven anymore) 

### 1.1
* Add an error listener to be able to control how errors and exceptions are managed
* Add the Epub3 Table of contents

### 1.2
* Allow to set the cover page with a specified title in the guide reference

### 1.3
* Use HtmlCleaner 2.24

### 1.3.1
* Fix the manifests for the core library and the viewer

## License
As stated on the http://www.siegmann.nl/epublib/license page of the original library, the license is LGPL.

## Credits
Some Icons are Copyright © <a href="http://p.yusukekamiyamane.com/">Yusuke Kamiyamane</a>. All rights reserved. 
Licensed under a <a href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 license</a>.

## Command line examples

Set the author of an existing epub
	java -jar epublib.jar --in input.epub --out result.epub --author Tester,Joe

Set the cover image of an existing epub
	java -jar epublib.jar --in input.epub --out result.epub --cover-image my_cover.jpg

## Creating an epub programmatically

	package nl.siegmann.epublib.examples;

	import java.io.InputStream;
	import java.io.FileOutputStream;
	 
	import nl.siegmann.epublib.domain.Author;
	import nl.siegmann.epublib.domain.Book;
	import nl.siegmann.epublib.domain.Metadata;
	import nl.siegmann.epublib.domain.Resource;
	import nl.siegmann.epublib.domain.TOCReference;
	
	import nl.siegmann.epublib.epub.EpubWriter;
	 
	public class Translator {
	  private static InputStream getResource( String path ) {
	    return Translator.class.getResourceAsStream( path );
	  }
	
	  private static Resource getResource( String path, String href ) {
	    return new Resource( getResource( path ), href );
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
	      book.setCoverImage(
	        getResource("/book1/test_cover.png", "cover.png") );
	       
	      // Add Chapter 1
	      book.addSection("Introduction",
	        getResource("/book1/chapter1.html", "chapter1.html") );
	       
	      // Add css file
	      book.getResources().add(
	        getResource("/book1/book1.css", "book1.css") );
	       
	      // Add Chapter 2
	      TOCReference chapter2 = book.addSection( "Second Chapter",
	        getResource("/book1/chapter2.html", "chapter2.html") );
	       
	      // Add image used by Chapter 2
	      book.getResources().add(
	        getResource("/book1/flowers_320x240.jpg", "flowers.jpg"));
	       
	      // Add Chapter2, Section 1
	      book.addSection(chapter2, "Chapter 2, section 1",
	        getResource("/book1/chapter2_1.html", "chapter2_1.html"));
	       
	      // Add Chapter 3
	      book.addSection("Conclusion",
	        getResource("/book1/chapter3.html", "chapter3.html"));
	       
	      // Create EpubWriter
	      EpubWriter epubWriter = new EpubWriter();
	       
	      // Write the Book as Epub
	      epubWriter.write(book, new FileOutputStream("test1_book1.epub"));
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
	}

