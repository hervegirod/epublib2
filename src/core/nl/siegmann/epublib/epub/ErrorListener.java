package nl.siegmann.epublib.epub;

/**
 *
 * An error listener.
 *
 * @since 1.1
 */
public interface ErrorListener {
   public void error(String message);

   public void error(Throwable e);

   public default void reset() {
   }
}
