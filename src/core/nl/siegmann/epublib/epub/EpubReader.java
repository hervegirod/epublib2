package nl.siegmann.epublib.epub;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import nl.siegmann.epublib.util.Constants;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.service.MediatypeService;
import nl.siegmann.epublib.util.ResourceUtil;

import nl.siegmann.epublib.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Reads an epub file.
 *
 * @author paul
 * @version 1.1
 */
public class EpubReader {
   private BookProcessor bookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR;

   public Book readEpub(InputStream in) throws IOException {
      return readEpub(in, Constants.CHARACTER_ENCODING);
   }

   public Book readEpub(ZipInputStream in) throws IOException {
      return readEpub(in, Constants.CHARACTER_ENCODING);
   }

   public Book readEpub(ZipFile zipfile) throws IOException {
      return readEpub(zipfile, Constants.CHARACTER_ENCODING);
   }

   /**
    * Read epub from inputstream
    *
    * @param in the inputstream from which to read the epub
    * @param encoding the encoding to use for the html files within the epub
    * @return the Book as read from the inputstream
    * @throws IOException
    */
   public Book readEpub(InputStream in, String encoding) throws IOException {
      return readEpub(new ZipInputStream(in), encoding);
   }

   /**
    * Reads this EPUB without loading any resources into memory.
    *
    * @param zipFile the file to load
    * @param encoding the encoding for XHTML files
    *
    * @return this Book without loading all resources into memory.
    * @throws IOException
    */
   public Book readEpubLazy(ZipFile zipFile, String encoding) throws IOException {
      return readEpubLazy(zipFile, encoding, Arrays.asList(MediatypeService.mediatypes));
   }

   public Book readEpub(ZipInputStream in, String encoding) throws IOException {
      return readEpub(ResourcesLoader.loadResources(in, encoding));
   }

   public Book readEpub(ZipFile in, String encoding) throws IOException {
      return readEpub(ResourcesLoader.loadResources(in, encoding));
   }

   /**
    * Reads this EPUB without loading all resources into memory.
    *
    * @param zipFile the file to load
    * @param encoding the encoding for XHTML files
    * @param lazyLoadedTypes a list of the MediaType to load lazily
    * @return this Book without loading all resources into memory.
    * @throws IOException
    */
   public Book readEpubLazy(ZipFile zipFile, String encoding, List<MediaType> lazyLoadedTypes) throws IOException {
      Resources resources = ResourcesLoader.loadResources(zipFile, encoding, lazyLoadedTypes);
      return readEpub(resources);
   }

   public Book readEpub(Resources resources) throws IOException {
      return readEpub(resources, new Book());
   }

   public Book readEpub(Resources resources, Book result) throws IOException {
      if (result == null) {
         result = new Book();
      }
      handleMimeType(result, resources);
      String packageResourceHref = getPackageResourceHref(resources);
      Resource packageResource = processPackageResource(packageResourceHref, result, resources);
      result.setOpfResource(packageResource);
      Resource ncxResource = processNcxResource(packageResource, result);
      result.setNcxResource(ncxResource);
      result = postProcessBook(result);
      return result;
   }

   private Book postProcessBook(Book book) {
      if (bookProcessor != null) {
         book = bookProcessor.processBook(book);
      }
      return book;
   }

   private Resource processNcxResource(Resource packageResource, Book book) {
      return NCXDocument.read(book, this);
   }

   private Resource processPackageResource(String packageResourceHref, Book book, Resources resources) {
      Resource packageResource = resources.remove(packageResourceHref);
      try {
         PackageDocumentReader.read(packageResource, this, book, resources);
      } catch (Exception e) {
         ErrorManager.error(e);
      }
      return packageResource;
   }

   private String getPackageResourceHref(Resources resources) {
      String defaultResult = "OEBPS/content.opf";
      String result = defaultResult;

      Resource containerResource = resources.remove("META-INF/container.xml");
      if (containerResource == null) {
         return result;
      }
      try {
         Document document = ResourceUtil.getAsDocument(containerResource);
         Element rootFileElement = (Element) ((Element) document.getDocumentElement().getElementsByTagName("rootfiles").item(0)).getElementsByTagName("rootfile").item(0);
         result = rootFileElement.getAttribute("full-path");
      } catch (Exception e) {
         ErrorManager.error(e);
      }
      if (StringUtil.isEmpty(result)) {
         result = defaultResult;
      }
      return result;
   }

   private void handleMimeType(Book result, Resources resources) {
      resources.remove("mimetype");
   }
}
