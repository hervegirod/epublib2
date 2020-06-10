package nl.siegmann.epublib.epub;

/**
 * A default error listener.
 *
 * @since 1.1
 */
public class DefaultErrorListener implements ErrorListener {
   @Override
   public void error(String message) {
      System.err.println(message);
   }

   @Override
   public void error(Throwable e) {
      System.err.println(e.getMessage());
   }

}
