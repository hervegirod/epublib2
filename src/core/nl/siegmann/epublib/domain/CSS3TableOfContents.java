package nl.siegmann.epublib.domain;

import java.io.IOException;
import java.util.Iterator;

/**
 * The CSS 3 Table of contents
 *
 * @since 1.1
 */
public class CSS3TableOfContents {
   private TableOfContents toc = null;
   private Resource tocResource = null;
   private String title = "Table of Contents";
   private Resource stylesheet = null;

   public CSS3TableOfContents(Resource stylesheet) {
      this.stylesheet = stylesheet;
      tocResource = new Resource("__toc__.html");
   }

   public CSS3TableOfContents(Resource stylesheet, String title) {
      this(stylesheet);
      this.title = title;
   }

   public void setTOCResource(Resource toc) {
      this.tocResource = toc;
   }

   public String getTitle() {
      return title;
   }

   public Resource getTOCResource() {
      return tocResource;
   }

   public Resource getStyleSheet() {
      return stylesheet;
   }

   public void setTableOfContents(TableOfContents toc) {
      this.toc = toc;
   }

   public TableOfContents getTableOfContents() {
      return toc;
   }

   public void finishResourceCreation() {
      try {
         StringBuilder buf = new StringBuilder();
         buf.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
         buf.append("<head>\n");
         buf.append("<title>").append(title).append("</title>\n");
         if (stylesheet != null) {
            String href = stylesheet.getHref();
            buf.append("<link href=\"").append(href).append("\" rel=\"stylesheet\" type=\"text/css\"/>\n");
         }
         buf.append("</head>\n");

         buf.append("<body>\n");
         buf.append("<h2>CONTENTS</h2>\n");

         buf.append("<table width=\"80%\">\n");
         buf.append("<tbody>\n");
         buf.append("<tr>\n");
         buf.append("<td align=\"right\" valign=\"top\">CHAPTER</td>\n");
         buf.append("<td align=\"left\" valign=\"top\"> </td>\n");
         buf.append("</tr>\n");
         Iterator<TOCReference> it = toc.getTocReferences().iterator();
         int index = 1;
         while (it.hasNext()) {
            TOCReference tocRef = it.next();
            String title = tocRef.getTitle();
            String href = tocRef.getResource().getHref();
            buf.append("<tr>\n");
            buf.append("<td align=\"right\" valign=\"top\">").append(index).append("</td>\n");
            buf.append("<td align=\"left\" valign=\"top\">");
            buf.append("<a href=\"").append(href).append("\">").append(title).append("</a></td>\n");
            buf.append("</tr>\n");
            index++;
         }
         buf.append("</tbody>\n");
         buf.append("</table>\n");

         buf.append("</body>\n");
         buf.append("</html>");
         tocResource.setData(buf.toString());
      } catch (IOException ex) {
      }
   }

}
