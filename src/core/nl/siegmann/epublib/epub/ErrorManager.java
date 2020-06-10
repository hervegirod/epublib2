package nl.siegmann.epublib.epub;

/**
 *
 * @since 1.1
 */
public class ErrorManager {
   private static ErrorManager manager = null;
   private ErrorListener listener = new DefaultErrorListener();

   private ErrorManager() {
   }

   public void setErrorListener(ErrorListener listener) {
      this.listener = listener;
   }

   public ErrorListener getErrorListener() {
      return listener;
   }

   public static void error(String message) {
      getInstance();
      manager.listener.error(message);
   }

   public static void error(Throwable e) {
      getInstance();
      manager.listener.error(e);
   }

   public static void reset() {
      getInstance();
      manager.listener.reset();
   }

   public static final ErrorManager getInstance() {
      if (manager == null) {
         manager = new ErrorManager();
      }
      return manager;
   }

}
