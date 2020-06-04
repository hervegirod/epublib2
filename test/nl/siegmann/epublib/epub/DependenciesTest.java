package nl.siegmann.epublib.epub;

import static org.junit.Assert.assertFalse;
import java.io.File;
import org.jdepend.model.JarDependencies;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test jar dependencies.
 *
 * @since 0.1
 */
public class DependenciesTest {
   private static final String CORE = "epublib.jar";
   private static final String VIEWER = "epublibViewer.jar";
   private static JarDependencies coreDepend = null;
   private static JarDependencies viewerDepend = null;

   public DependenciesTest() {
   }

   @BeforeClass
   public static void setUpClass() throws Exception {
      // get the path of the dist directory
      File userDir = new File(System.getProperty("user.dir"));
      File dist = new File(userDir, "dist");

      // get the path of the jar files
      File jarFile = new File(dist, CORE);
      coreDepend = new JarDependencies();
      coreDepend.setFile(jarFile);

      jarFile = new File(dist, VIEWER);
      viewerDepend = new JarDependencies();
      viewerDepend.setFile(jarFile);
   }

   @AfterClass
   public static void tearDownClass() {
      coreDepend = null;
      viewerDepend = null;
   }

   @Before
   public void setUp() {
   }

   @After
   public void tearDown() {
   }

   /**
    * Test that the epublib jar file don't depend on epublibViewer.
    */
   @Test
   public void testDependencies() {
      System.out.println("DependenciesTest : testDependencies");
      assertFalse("epublib.jar must not depend on epublibViewer.jar", coreDepend.isDependingOn(viewerDepend));
   }
}
