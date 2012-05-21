package uk.ac.bristol.dundry.tasks;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class CopyTaskTest {
    final static Logger log = LoggerFactory.getLogger(CopyTaskTest.class);
    
    private Path destination;
    private CopyTask instance;
    
    @Before
    public void setUp() throws IOException {
        instance = new CopyTask();
        destination = Files.createTempDirectory("dundry-test");
    }
    
    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(destination, new Deleter());
    }

    /**
     * Test of copyDirectory method, of class CopyTask.
     */
    @Test
    public void testCopyDirectory() throws Exception {
        Path source = getPath("/copy/dir");
        
        instance.copyDirectory(source, destination);
        
        assertTrue("Same directory structure", compare(source, destination.resolve("dir")));
    }
    
    /**
     * Test of copying a simple file
     */
    @Test
    public void testCopyFile() throws Exception {
        Path source = getPath("/copy/file");
        
        instance.copyDirectory(source, destination);
        
        assertTrue("Same directory structure", compare(source, destination.resolve("file")));
    }
    
    private Path getPath(String p) throws URISyntaxException {
        URL pathURL = this.getClass().getResource(p);
        return Paths.get(pathURL.toURI());
    }
    
    private boolean compare(Path source, Path destination) throws IOException {        
        Comparer left = new Comparer(source, destination);
        Files.walkFileTree(source, left);
        
        Comparer right = new Comparer(destination, source);
        Files.walkFileTree(destination, right);
        
        if (!left.identical) log.warn(left.reason);
        if (!right.identical) log.warn(right.reason);
        
        return left.identical && right.identical;
    }
    
    private static class Deleter extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                          throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                          throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }
    }
    
    private static class Comparer extends SimpleFileVisitor<Path> {
        
        private final Path toCompare;
        private final Path base;
        private boolean identical = true;
        private String reason = "No difference";
        
        private Comparer(Path base, Path toCompare) {
            this.base = base;
            this.toCompare = toCompare;
        }
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                          throws IOException {
            if (!existsElsewhere(file)) {
                identical = false;
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
        
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                          throws IOException {
            if (!existsElsewhere(dir)) {
                identical = false;
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        }
        
        private boolean existsElsewhere(Path visiting) {
            Path rel = base.relativize(visiting);
            Path other = toCompare.resolve(rel);
                        
            if (!Files.exists(other)) {
                reason = String.format("<%s> does not exist", other);
                return false;
            }
            
            if (Files.isDirectory(visiting) != Files.isDirectory(other)) {
                reason = String.format("<%s> and <%s> are not of the same kind", other, toCompare);
                return false;
            }
            
            return true;
        }
    }
}
