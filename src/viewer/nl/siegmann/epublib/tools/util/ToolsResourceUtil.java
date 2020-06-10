package nl.siegmann.epublib.tools.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Scanner;
import java.util.regex.Pattern;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.ErrorManager;
import nl.siegmann.epublib.service.MediatypeService;

/**
 * Various resource utility methods
 *
 * @author paul
 * @version 1.1
 */
public class ToolsResourceUtil {
   public static String getTitle(Resource resource) {
      if (resource == null) {
         return "";
      }
      if (resource.getMediaType() != MediatypeService.XHTML) {
         return resource.getHref();
      }
      String title = findTitleFromXhtml(resource);
      if (title == null) {
         title = "";
      }
      return title;
   }

   /**
    * Retrieves whatever it finds between &lt;title&gt;...&lt;/title&gt; or &lt;h1-7&gt;...&lt;/h1-7&gt;.
    * The first match is returned, even if it is a blank string.
    * If it finds nothing null is returned.
    *
    * @param resource
    * @return whatever it finds in the resource between &lt;title&gt;...&lt;/title&gt; or &lt;h1-7&gt;...&lt;/h1-7&gt;.
    */
   public static String findTitleFromXhtml(Resource resource) {
      if (resource == null) {
         return "";
      }
      if (resource.getTitle() != null) {
         return resource.getTitle();
      }
      Pattern h_tag = Pattern.compile("^h\\d\\s*", Pattern.CASE_INSENSITIVE);
      String title = null;
      try {
         Reader content = resource.getReader();
         Scanner scanner = new Scanner(content);
         scanner.useDelimiter("<");
         while (scanner.hasNext()) {
            String text = scanner.next();
            int closePos = text.indexOf('>');
            String tag = text.substring(0, closePos);
            if (tag.equalsIgnoreCase("title")
               || h_tag.matcher(tag).find()) {

               title = text.substring(closePos + 1).trim();
               break;
            }
         }
      } catch (IOException e) {
         ErrorManager.error(e);
      }
      resource.setTitle(title);
      return title;
   }
}
