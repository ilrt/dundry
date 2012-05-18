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
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Go through a new deposit, pulling the structure into the metadata
 *
 * @author pldms
 */
public class IndexDepositTask extends JobBase {

    final static Logger log = LoggerFactory.getLogger(IndexDepositTask.class);

    @Override
    public void execute(Resource item, Resource prov, String id, Path root, JobDataMap jobData) 
            throws JobExecutionException
    {
        try {
            walkDirectory(root, prov);
        } catch (IOException ex) {
            throw new JobExecutionException("Index walk failed", ex);
        }
    }

    /**
     * Walk directory, recording details in model
     *
     * @param from Source
     * @param root The resource corresponding to source in the model
     * @return
     * @throws IOException
     */
    private Path walkDirectory(final Path source, final Resource root) throws IOException {

        // We relativise paths to the parent of source
        final Path parent = source.getParent();

        return Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            // Keep track of parents as we walk the tree, so
            // tree structure is recorded faithfully
            Deque<Resource> parents = new ArrayDeque<>();

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = parent.relativize(file);
                log.trace("Visit {}", file);

                recordVisit(rel, RDFS.Resource); // we really don't know what it is

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = parent.relativize(dir);

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
             *
             * @return item in database
             */
            private Resource recordVisit(Path relativePath, Resource type) {
                // Parent is either top of stack or -- if absent -- root
                Resource parent = (parents.isEmpty()) ? root : parents.peek();

                Resource item = root.getModel().createResource(root.getURI() + "/" + relativePath.toString());
                if (!RDFS.Resource.equals(type)) {
                    item.addProperty(RDF.type, type);
                }

                parent.addProperty(DCTerms.hasPart, item);
                return item;
            }
        });
    }
}
