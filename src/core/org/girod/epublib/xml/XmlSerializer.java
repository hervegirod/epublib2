package org.girod.epublib.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 * @author Herve
 */
public interface XmlSerializer {

   public void setOutput(OutputStream out, String string) throws IOException, IllegalArgumentException, IllegalStateException;

   public void setOutput(Writer writer) throws IOException, IllegalArgumentException, IllegalStateException;

   public void startDocument(String string, Boolean bln) throws IOException, IllegalArgumentException, IllegalStateException;

   public void endDocument() throws IOException, IllegalArgumentException, IllegalStateException;

   public void setPrefix(String string, String string1) throws IOException, IllegalArgumentException, IllegalStateException;

   public String getPrefix(String string, boolean bln) throws IllegalArgumentException;

   public int getDepth();

   public String getNamespace();

   public String getName();

   public XmlSerializer startTag(String string, String string1) throws IOException, IllegalArgumentException, IllegalStateException;

   public XmlSerializer attribute(String string, String string1, String string2) throws IOException, IllegalArgumentException, IllegalStateException;

   public XmlSerializer endTag(String string, String string1) throws IOException, IllegalArgumentException, IllegalStateException;

   public XmlSerializer text(String string) throws IOException, IllegalArgumentException, IllegalStateException;

   public XmlSerializer text(char[] chars, int i, int i1) throws IOException, IllegalArgumentException, IllegalStateException;

   public void flush() throws IOException;
}
