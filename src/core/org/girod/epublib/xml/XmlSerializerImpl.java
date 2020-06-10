package org.girod.epublib.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Implementation of XmlSerializer interface from XmlPull V1 API.
 *
 * @version 1.1
 */
public class XmlSerializerImpl implements XmlSerializer {
   protected final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
   protected final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

   protected String indentationString = "  "; //" ";
   protected String lineSeparator = "\n";
   protected Writer out;

   protected int autoDeclaredPrefixes;

   protected int depth = 0;

   // element stack
   protected String elNamespace[] = new String[2];
   protected String elName[] = new String[elNamespace.length];
   protected String elPrefix[] = new String[elNamespace.length];
   protected int elNamespaceCount[] = new int[elNamespace.length];

   //namespace stack
   protected int namespaceEnd = 0;
   protected String namespacePrefix[] = new String[8];
   protected String namespaceUri[] = new String[namespacePrefix.length];

   protected boolean finished;
   protected boolean pastRoot;
   protected boolean setPrefixCalled;
   protected boolean startTagIncomplete;

   protected boolean doIndent;
   protected boolean seenTag;

   protected boolean seenBracket;
   protected boolean seenBracketBracket;

   protected static final String precomputedPrefixes[];

   static {
      precomputedPrefixes = new String[32]; //arbitrary number ...
      for (int i = 0; i < precomputedPrefixes.length; i++) {
         precomputedPrefixes[i] = ("n" + i).intern();
      }
   }

   protected void reset() {
      out = null;
      autoDeclaredPrefixes = 0;
      depth = 0;

      // nullify references on all levels to allow it to be GCed
      for (int i = 0; i < elNamespaceCount.length; i++) {
         elName[i] = null;
         elPrefix[i] = null;
         elNamespace[i] = null;
         elNamespaceCount[i] = 2;
      }

      namespaceEnd = 0;

      //NOTE: no need to intern() as all literal strings and string-valued constant expressions
      //are interned. String literals are defined in 3.10.5 of the Java Language Specification
      // just checking ...
      //assert "xmlns" == "xmlns".intern();
      //assert XMLNS_URI == XMLNS_URI.intern();
      //TODO: how to prevent from reporting this namespace?
      // this is special namespace declared for consistensy with XML infoset
      namespacePrefix[namespaceEnd] = "xmlns";
      namespaceUri[namespaceEnd] = XMLNS_URI;
      ++namespaceEnd;

      namespacePrefix[namespaceEnd] = "xml";
      namespaceUri[namespaceEnd] = XML_URI;
      ++namespaceEnd;

      finished = false;
      pastRoot = false;
      setPrefixCalled = false;
      startTagIncomplete = false;
      //doIntent is not changed
      seenTag = false;

      seenBracket = false;
      seenBracketBracket = false;
   }

   protected void ensureElementsCapacity() {
      final int elStackSize = elName.length;
      //assert (depth + 1) >= elName.length;
      // we add at least one extra slot ...
      final int newSize = (depth >= 7 ? 2 * depth : 8) + 2; // = lucky 7 + 1 //25
      final boolean needsCopying = elStackSize > 0;
      String[] arr = null;
      // reuse arr local variable slot
      arr = new String[newSize];
      if (needsCopying) {
         System.arraycopy(elName, 0, arr, 0, elStackSize);
      }
      elName = arr;

      arr = new String[newSize];
      if (needsCopying) {
         System.arraycopy(elPrefix, 0, arr, 0, elStackSize);
      }
      elPrefix = arr;

      arr = new String[newSize];
      if (needsCopying) {
         System.arraycopy(elNamespace, 0, arr, 0, elStackSize);
      }
      elNamespace = arr;

      final int[] iarr = new int[newSize];
      if (needsCopying) {
         System.arraycopy(elNamespaceCount, 0, iarr, 0, elStackSize);
      } else {
         // special initialization
         iarr[0] = 0;
      }
      elNamespaceCount = iarr;
   }

   protected void ensureNamespacesCapacity() {
      final int newSize = namespaceEnd > 7 ? 2 * namespaceEnd : 8;
      final String[] newNamespacePrefix = new String[newSize];
      final String[] newNamespaceUri = new String[newSize];
      if (namespacePrefix != null) {
         System.arraycopy(
            namespacePrefix, 0, newNamespacePrefix, 0, namespaceEnd);
         System.arraycopy(
            namespaceUri, 0, newNamespaceUri, 0, namespaceEnd);
      }
      namespacePrefix = newNamespacePrefix;
      namespaceUri = newNamespaceUri;
   }

   // precomputed variables to simplify writing indentation
   protected int offsetNewLine;
   protected int indentationJump;
   protected char[] indentationBuf;
   protected int maxIndentLevel;
   protected boolean writeLineSepartor; //should end-of-line be written
   protected boolean writeIndentation; // is indentation used?

   /**
    * For maximum efficiency when writing indents the required output is pre-computed
    * This is internal function that recomputes buffer after user requested chnages.
    */
   protected void rebuildIndentationBuf() {
      if (doIndent == false) {
         return;
      }
      final int maxIndent = 65; //hardcoded maximum indentation size in characters
      int bufSize = 0;
      offsetNewLine = 0;
      if (writeLineSepartor) {
         offsetNewLine = lineSeparator.length();
         bufSize += offsetNewLine;
      }
      maxIndentLevel = 0;
      if (writeIndentation) {
         indentationJump = indentationString.length();
         maxIndentLevel = maxIndent / indentationJump;
         bufSize += maxIndentLevel * indentationJump;
      }
      if (indentationBuf == null || indentationBuf.length < bufSize) {
         indentationBuf = new char[bufSize + 8];
      }
      int bufPos = 0;
      if (writeLineSepartor) {
         for (int i = 0; i < lineSeparator.length(); i++) {
            indentationBuf[bufPos++] = lineSeparator.charAt(i);
         }
      }
      if (writeIndentation) {
         for (int i = 0; i < maxIndentLevel; i++) {
            for (int j = 0; j < indentationString.length(); j++) {
               indentationBuf[bufPos++] = indentationString.charAt(j);
            }
         }
      }
   }

   // if(doIndent) writeIndent();
   protected void writeIndent() throws IOException {
      final int start = writeLineSepartor ? 0 : offsetNewLine;
      final int level = (depth > maxIndentLevel) ? maxIndentLevel : depth;
      out.write(indentationBuf, start, ((level - 1) * indentationJump) + offsetNewLine);
   }

   // this is special method that can be accessed directly to retrieve Writer serializer is using
   public Writer getWriter() {
      return out;
   }

   @Override
   public void setOutput(Writer writer) {
      reset();
      out = writer;
   }

   @Override
   public void setOutput(OutputStream os, String encoding) throws IOException {
      if (os == null) {
         throw new IllegalArgumentException("output stream can not be null");
      }
      reset();
      if (encoding != null) {
         out = new OutputStreamWriter(os, encoding);
      } else {
         out = new OutputStreamWriter(os);
      }
   }

   @Override
   public void startDocument(String encoding, Boolean standalone) throws IOException {
      out.write("<?xml version=\"1.0\"");
      if (encoding != null) {
         out.write(" encoding=");
         out.write('"');
         out.write(encoding);
         out.write('"');
      }
      if (standalone != null) {
         out.write(" standalone=");
         out.write('"');
         if (standalone.booleanValue()) {
            out.write("yes");
         } else {
            out.write("no");
         }
         out.write('"');
      }
      out.write("?>");
   }

   @Override
   public void endDocument() throws IOException {
      // close all unclosed tag;
      while (depth > 0) {
         endTag(elNamespace[depth], elName[depth]);
      }
      //assert depth == 0;
      //assert startTagIncomplete == false;
      finished = pastRoot = startTagIncomplete = true;
      out.flush();
   }

   @Override
   public void setPrefix(String prefix, String namespace) throws IOException {
      if (startTagIncomplete) {
         closeStartTag();
      }
      //assert prefix != null;
      //assert namespace != null;
      if (prefix == null) {
         prefix = "";
      }
      prefix = prefix.intern(); //will throw NPE if prefix==null

      //check that prefix is not duplicated ...
      for (int i = elNamespaceCount[depth]; i < namespaceEnd; i++) {
         if (prefix == namespacePrefix[i]) {
            throw new IllegalStateException("duplicated prefix " + prefix);
         }
      }
      namespace = namespace.intern();;

      if (namespaceEnd >= namespacePrefix.length) {
         ensureNamespacesCapacity();
      }
      namespacePrefix[namespaceEnd] = prefix;
      namespaceUri[namespaceEnd] = namespace;
      ++namespaceEnd;
      setPrefixCalled = true;
   }

   protected String lookupOrDeclarePrefix(String namespace) {
      return getPrefix(namespace, true);
   }

   @Override
   public String getPrefix(String namespace, boolean generatePrefix) {
      return getPrefix(namespace, generatePrefix, false);
   }

   protected String getPrefix(String namespace, boolean generatePrefix, boolean nonEmpty) {
      namespace = namespace.intern();
      if (namespace == null) {
         throw new IllegalArgumentException("namespace must be not null");
      } else if (namespace.length() == 0) {
         throw new IllegalArgumentException("default namespace cannot have prefix");
      }

      // first check if namespace is already in scope
      for (int i = namespaceEnd - 1; i >= 0; --i) {
         if (namespace == namespaceUri[i]) {
            final String prefix = namespacePrefix[i];
            if (nonEmpty && prefix.length() == 0) {
               continue;
            }
            // now check that prefix is still in scope
            for (int p = namespaceEnd - 1; p > i; --p) {
               if (prefix == namespacePrefix[p]) {
                  continue; // too bad - prefix is redeclared with different namespace
               }
            }
            return prefix;
         }
      }

      // so not found it ...
      if (!generatePrefix) {
         return null;
      }
      return generatePrefix(namespace);
   }

   private String generatePrefix(String namespace) {
      //assert namespace == namespace.intern();
      LOOP:
      while (true) {
         ++autoDeclaredPrefixes;
         //fast lookup uses table that was pre-initialized in static{} ....
         final String prefix = autoDeclaredPrefixes < precomputedPrefixes.length
            ? precomputedPrefixes[autoDeclaredPrefixes] : ("n" + autoDeclaredPrefixes).intern();
         // make sure this prefix is not declared in any scope (avoid hiding in-scope prefixes)!
         for (int i = namespaceEnd - 1; i >= 0; --i) {
            if (prefix == namespacePrefix[i]) {
               continue LOOP; // prefix is already declared - generate new and try again
            }
         }
         // declare prefix

         if (namespaceEnd >= namespacePrefix.length) {
            ensureNamespacesCapacity();
         }
         namespacePrefix[namespaceEnd] = prefix;
         namespaceUri[namespaceEnd] = namespace;
         ++namespaceEnd;

         return prefix;
      }
   }

   @Override
   public int getDepth() {
      return depth;
   }

   @Override
   public String getNamespace() {
      return elNamespace[depth];
   }

   @Override
   public String getName() {
      return elName[depth];
   }

   @Override
   public XmlSerializer startTag(String namespace, String name) throws IOException {
      if (startTagIncomplete) {
         closeStartTag();
      }
      out.write("\n");
      seenBracket = seenBracketBracket = false;
      ++depth;
      if (doIndent && depth > 0 && seenTag) {
         writeIndent();
      }
      seenTag = true;
      setPrefixCalled = false;
      startTagIncomplete = true;
      if ((depth + 1) >= elName.length) {
         ensureElementsCapacity();
      }

      elNamespace[depth] = (namespace == null) ? namespace : namespace.intern();
      elName[depth] = (name == null) ? name : name.intern();
      if (out == null) {
         throw new IllegalStateException("setOutput() must called set before serialization can start");
      }
      out.write('<');
      if (namespace != null) {
         if (namespace.length() > 0) {
            //ALEK: in future make this algo a feature on serializer
            String prefix = null;
            if (depth > 0 && (namespaceEnd - elNamespaceCount[depth - 1]) == 1) {
               // if only one prefix was declared un-declare it if the prefix is already declared on parent el with the same URI
               String uri = namespaceUri[namespaceEnd - 1];
               if (uri == namespace || uri.equals(namespace)) {
                  String elPfx = namespacePrefix[namespaceEnd - 1];
                  // 2 == to skip predefined namesapces (xml and xmlns ...)
                  for (int pos = elNamespaceCount[depth - 1] - 1; pos >= 2; --pos) {
                     String pf = namespacePrefix[pos];
                     if (pf == elPfx || pf.equals(elPfx)) {
                        String n = namespaceUri[pos];
                        if (n == uri || n.equals(uri)) {
                           --namespaceEnd; //un-declare namespace: this is kludge!
                           prefix = elPfx;
                        }
                        break;
                     }
                  }
               }
            }
            if (prefix == null) {
               prefix = lookupOrDeclarePrefix(namespace);
            }
            //assert prefix != null;
            // make sure that default ("") namespace to not print ":"
            if (prefix.length() > 0) {
               elPrefix[depth] = prefix;
               out.write(prefix);
               out.write(':');
            } else {
               elPrefix[depth] = "";
            }
         } else {
            // make sure that default namespace can be declared
            for (int i = namespaceEnd - 1; i >= 0; --i) {
               if (namespacePrefix[i] == "") {
                  final String uri = namespaceUri[i];
                  if (uri == null) {
                     // declare default namespace
                     setPrefix("", "");
                  } else if (uri.length() > 0) {
                     throw new IllegalStateException(
                        "start tag can not be written in empty default namespace "
                        + "as default namespace is currently bound to '" + uri + "'");
                  }
                  break;
               }
            }
            elPrefix[depth] = "";
         }
      } else {
         elPrefix[depth] = "";
      }
      out.write(name);
      return this;
   }

   @Override
   public XmlSerializer attribute(String namespace, String name,
      String value) throws IOException {
      if (!startTagIncomplete) {
         throw new IllegalArgumentException("startTag() must be called before attribute()");
      }
      //assert setPrefixCalled == false;
      out.write(' ');
      ////assert namespace != null;
      if (namespace != null && namespace.length() > 0) {
         namespace = namespace.intern();
         String prefix = getPrefix(namespace, false, true);
         //assert( prefix != null);
         //if(prefix.length() == 0) {
         if (prefix == null) {
            // needs to declare prefix to hold default namespace
            //NOTE: attributes such as a='b' are in NO namespace
            prefix = generatePrefix(namespace);
         }
         out.write(prefix);
         out.write(':');
      }
      out.write(name);
      out.write('=');
      out.write('"');
      writeAttributeValue(value, out);
      out.write('"');
      return this;
   }

   protected void closeStartTag() throws IOException {
      if (finished) {
         throw new IllegalArgumentException("trying to write past already finished output");
      }
      if (seenBracket) {
         seenBracket = seenBracketBracket = false;
      }
      if (startTagIncomplete || setPrefixCalled) {
         if (setPrefixCalled) {
            throw new IllegalArgumentException(
               "startTag() must be called immediately after setPrefix()");
         }
         if (!startTagIncomplete) {
            throw new IllegalArgumentException("trying to close start tag that is not opened");
         }

         // write all namespace delcarations!
         writeNamespaceDeclarations();
         out.write('>');
         elNamespaceCount[depth] = namespaceEnd;
         startTagIncomplete = false;
      }
   }

   private void writeNamespaceDeclarations() throws IOException {
      //int start = elNamespaceCount[ depth - 1 ];
      for (int i = elNamespaceCount[depth - 1]; i < namespaceEnd; i++) {
         if (doIndent && namespaceUri[i].length() > 40) {
            writeIndent();
            out.write(" ");
         }
         if (namespacePrefix[i] != "") {
            out.write(" xmlns:");
            out.write(namespacePrefix[i]);
            out.write('=');
         } else {
            out.write(" xmlns=");
         }
         out.write('"');

         //NOTE: escaping of namespace value the same way as attributes!!!!
         writeAttributeValue(namespaceUri[i], out);

         out.write('"');
      }
   }

   @Override
   public XmlSerializer endTag(String namespace, String name, boolean newLine) throws IOException {
      seenBracket = seenBracketBracket = false;
      if (namespace != null) {
         namespace = namespace.intern();
      }

      if (namespace != elNamespace[depth]) {
         throw new IllegalArgumentException("expected namespace " + elNamespace[depth] + " and not " + namespace);
      }
      if (name == null) {
         throw new IllegalArgumentException("end tag name can not be null");
      }
      String startTagName = elName[depth];
      if ((!name.equals(startTagName)) || (name != startTagName)) {
         throw new IllegalArgumentException("expected element name " + elName[depth] + " and not " + name);
      }
      if (newLine) {
         out.write("\n");
      }
      if (startTagIncomplete) {
         writeNamespaceDeclarations();
         out.write(" />"); //space is added to make it easier to work in XHTML!!!
         --depth;
      } else {
         //assert startTagIncomplete == false;
         if (doIndent && seenTag) {
            writeIndent();
         }
         out.write("</");
         String startTagPrefix = elPrefix[depth];
         if (startTagPrefix.length() > 0) {
            out.write(startTagPrefix);
            out.write(':');
         }
         out.write(name);
         out.write('>');
         --depth;
      }
      namespaceEnd = elNamespaceCount[depth];
      startTagIncomplete = false;
      seenTag = true;
      return this;
   }

   @Override
   public XmlSerializer text(String text) throws IOException {
      if (startTagIncomplete || setPrefixCalled) {
         closeStartTag();
      }
      if (doIndent && seenTag) {
         seenTag = false;
      }
      writeElementContent(text, out);
      return this;
   }

   @Override
   public XmlSerializer text(char[] buf, int start, int len) throws IOException {
      if (startTagIncomplete || setPrefixCalled) {
         closeStartTag();
      }
      if (doIndent && seenTag) {
         seenTag = false;
      }
      writeElementContent(buf, start, len, out);
      return this;
   }

   @Override
   public void flush() throws IOException {
      if (!finished && startTagIncomplete) {
         closeStartTag();
      }
      out.flush();
   }

   // --- utility methods
   protected void writeAttributeValue(String value, Writer out) throws IOException {
      //.[apostrophe and <, & escaped],
      final char quot = '"';
      final String quotEntity = "&quot;";

      int pos = 0;
      for (int i = 0; i < value.length(); i++) {
         char ch = value.charAt(i);
         if (ch == '&') {
            if (i > pos) {
               out.write(value.substring(pos, i));
            }
            out.write("&amp;");
            pos = i + 1;
         }
         if (ch == '<') {
            if (i > pos) {
               out.write(value.substring(pos, i));
            }
            out.write("&lt;");
            pos = i + 1;
         } else if (ch == quot) {
            if (i > pos) {
               out.write(value.substring(pos, i));
            }
            out.write(quotEntity);
            pos = i + 1;
         } else if (ch < 32) {
            //in XML 1.0 only legal character are #x9 | #xA | #xD
            // and they must be escaped otherwise in attribute value they are normalized to spaces
            if (ch == 13 || ch == 10 || ch == 9) {
               if (i > pos) {
                  out.write(value.substring(pos, i));
               }
               out.write("&#");
               out.write(Integer.toString(ch));
               out.write(';');
               pos = i + 1;
            } else {
               throw new IllegalStateException(
                  //"character "+Integer.toString(ch)+" is not allowed in output"+getLocation());
                  "character " + ch + " (" + Integer.toString(ch) + ") is not allowed in output"
                  + " (attr value=" + value + ")");
               // in XML 1.1 legal are [#x1-#xD7FF]
               //              if(ch > 0) {
               //                  if(i > pos) out.write(text.substring(pos, i));
               //                  out.write("&#");
               //                  out.write(Integer.toString(ch));
               //                  out.write(';');
               //                  pos = i + 1;
               //              } else {
               //              throw new IllegalStateException(
               //                  "character zero is not allowed in XML 1.1 output"+getLocation());
               //              }
            }
         }
      }
      if (pos > 0) {
         out.write(value.substring(pos));
      } else {
         out.write(value);  // this is shortcut to the most common case
      }

   }

   protected void writeElementContent(String text, Writer out) throws IOException {
      // esccape '<', '&', ']]>', <32 if necessary
      int pos = 0;
      for (int i = 0; i < text.length(); i++) {
         //TODO: check if doing char[] text.getChars() would be faster than getCharAt(i) ...
         char ch = text.charAt(i);
         if (ch == ']') {
            if (seenBracket) {
               seenBracketBracket = true;
            } else {
               seenBracket = true;
            }
         } else {
            if (ch == '&') {
               if (i > pos) {
                  out.write(text.substring(pos, i));
               }
               out.write("&amp;");
               pos = i + 1;
            } else if (ch == '<') {
               if (i > pos) {
                  out.write(text.substring(pos, i));
               }
               out.write("&lt;");
               pos = i + 1;
            } else if (seenBracketBracket && ch == '>') {
               if (i > pos) {
                  out.write(text.substring(pos, i));
               }
               out.write("&gt;");
               pos = i + 1;
            } else if (ch < 32) {
               //in XML 1.0 only legal character are #x9 | #xA | #xD
               if (ch == 9 || ch == 10 || ch == 13) {
                  // pass through

                  //                    } else if(ch == 13) { //escape
                  //                        if(i > pos) out.write(text.substring(pos, i));
                  //                        out.write("&#");
                  //                        out.write(Integer.toString(ch));
                  //                        out.write(';');
                  //                        pos = i + 1;
               } else {
                  throw new IllegalStateException(
                     "character " + Integer.toString(ch) + " is not allowed in output (text value=" + text + ")");
                  // in XML 1.1 legal are [#x1-#xD7FF]
                  //              if(ch > 0) {
                  //                  if(i > pos) out.write(text.substring(pos, i));
                  //                  out.write("&#");
                  //                  out.write(Integer.toString(ch));
                  //                  out.write(';');
                  //                  pos = i + 1;
                  //              } else {
                  //              throw new IllegalStateException(
                  //                  "character zero is not allowed in XML 1.1 output"+getLocation());
                  //              }
               }
            }
            if (seenBracket) {
               seenBracketBracket = seenBracket = false;
            }

         }
      }
      if (pos > 0) {
         out.write(text.substring(pos));
      } else {
         out.write(text);  // this is shortcut to the most common case
      }

   }

   protected void writeElementContent(char[] buf, int off, int len, Writer out) throws IOException {
      // esccape '<', '&', ']]>'
      final int end = off + len;
      int pos = off;
      for (int i = off; i < end; i++) {
         final char ch = buf[i];
         if (ch == ']') {
            if (seenBracket) {
               seenBracketBracket = true;
            } else {
               seenBracket = true;
            }
         } else {
            if (ch == '&') {
               if (i > pos) {
                  out.write(buf, pos, i - pos);
               }
               out.write("&amp;");
               pos = i + 1;
            } else if (ch == '<') {
               if (i > pos) {
                  out.write(buf, pos, i - pos);
               }
               out.write("&lt;");
               pos = i + 1;

            } else if (seenBracketBracket && ch == '>') {
               if (i > pos) {
                  out.write(buf, pos, i - pos);
               }
               out.write("&gt;");
               pos = i + 1;
            } else if (ch < 32) {
               //in XML 1.0 only legal character are #x9 | #xA | #xD
               if (ch == 9 || ch == 10 || ch == 13) {
                  // pass through

                  //                    } else if(ch == 13 ) { //if(ch == '\r') {
                  //                        if(i > pos) {
                  //                            out.write(buf, pos, i - pos);
                  //                        }
                  //                        out.write("&#");
                  //                        out.write(Integer.toString(ch));
                  //                        out.write(';');
                  //                        pos = i + 1;
               } else {
                  throw new IllegalStateException(
                     "character " + ch + " (" + Integer.toString(ch) + ") is not allowed in output");
                  // in XML 1.1 legal are [#x1-#xD7FF]
                  //              if(ch > 0) {
                  //                  if(i > pos) out.write(text.substring(pos, i));
                  //                  out.write("&#");
                  //                  out.write(Integer.toString(ch));
                  //                  out.write(';');
                  //                  pos = i + 1;
                  //              } else {
                  //              throw new IllegalStateException(
                  //                  "character zero is not allowed in XML 1.1 output"+getLocation());
                  //              }
               }
            }
            if (seenBracket) {
               seenBracketBracket = seenBracket = false;
            }
            // assert seenBracketBracket == seenBracket == false;
         }
      }
      if (end > pos) {
         out.write(buf, pos, end - pos);
      }
   }

}
