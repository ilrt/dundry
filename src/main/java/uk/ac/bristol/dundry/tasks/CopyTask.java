package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.quartz.JobDataMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author pldms
 */
public class CopyTask extends JobBase {
    
    final static Logger log = LoggerFactory.getLogger(CopyTask.class);

    public final static String FROM = "copy-task-from";
    
    @Override
    public void execute(Repository store, String id, Path root, JobDataMap jobData) {
        Path from = (Path) jobData.get(FROM);
        Path to = root;
        
        try {
            Resource rootRes = store.getMetadata(id);
            copyDirectory(from, to, rootRes);
            store.updateMetadata(id, rootRes);
        } catch (IOException ex) {
            throw new RuntimeException("Error copying", ex);
        }
    }
        
    /**
     * Recursively copy a directory into another directory.
     * Copying a to b will result in b/a...
     * @param from Source
     * @param to Destination
     * @return
     * @throws IOException 
     */
    private Path copyDirectory(final Path from, final Path to, final Resource root) throws IOException {
        
        log.info("Copy {} to {}", from, to);
        
        // We relativise paths to the parent of from
        // So /ex/a/b becomes /ex/a and we copy b directory
        final Path parent = from.getParent();
        
        try {
            return Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = parent.relativize(file);
                    log.debug("Visit {}", file);
                    Files.copy(file, to.resolve(rel), StandardCopyOption.COPY_ATTRIBUTES);
                    
                    recordVisit(rel);
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path rel = parent.relativize(dir);
                    log.trace("Visit dir {}", dir);
                    log.trace("Create dir {}", to.resolve(rel));
                    Files.createDirectory(to.resolve(rel));
                    
                    recordVisit(rel);
                    
                    return FileVisitResult.CONTINUE;
                }
                
                private void recordVisit(Path relativePath) {
                    Resource item = root.getModel().createResource(root.getURI() + "/" + relativePath.toString());
                    item.addProperty(DCTerms.type, "file");
                    root.addProperty(DCTerms.hasPart, item);
                }
            });
        } catch (IOException ex) {
            // Clean up?
            throw ex;
        }
    }
}
