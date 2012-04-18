package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.DCTypes;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
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
                
                // Keep track of parents as we walk the tree, so
                // tree structure is recorded faithfully
                Deque<Resource> parents = new ArrayDeque<>();
                                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = parent.relativize(file);
                    log.debug("Visit {}", file);
                    Files.copy(file, to.resolve(rel), StandardCopyOption.COPY_ATTRIBUTES);
                    
                    recordVisit(rel, RDFS.Resource); // we really don't know what it is
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path rel = parent.relativize(dir);
                    log.trace("Visit dir {}", dir);
                    log.trace("Create dir {}", to.resolve(rel));
                    Files.createDirectory(to.resolve(rel));
                    
                    // Record dir as a collection of resources
                    Resource item = recordVisit(rel, DCTypes.Collection);
                    parents.push(item); // make this the current parent
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exception) {
                    
                    parents.pop(); // finished. Forget this directory
                    
                    return FileVisitResult.CONTINUE;
                } 
                
                /**
                 * Make a note of this item in the database
                 * @return item in database
                 */
                private Resource recordVisit(Path relativePath, Resource type) {
                    // Parent is either top of stack or -- if absent -- root
                    Resource parent = (parents.isEmpty()) ? root : parents.peek() ;
                    
                    Resource item = root.getModel().createResource(root.getURI() + "/" + relativePath.toString());
                    if (!RDFS.Resource.equals(type)) item.addProperty(RDF.type, type);
                    
                    parent.addProperty(DCTerms.hasPart, item);
                    return item;
                }
            });
        } catch (IOException ex) {
            // Clean up?
            throw ex;
        }
    }
}
