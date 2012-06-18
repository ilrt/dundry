package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.ModelFactory;
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
import uk.ac.bristol.dundry.Util;
import uk.ac.bristol.dundry.dao.Repository;

/**
 * Go through a new deposit, pulling the structure into the metadata
 *
 * @author pldms
 */
public class IndexDepositTask extends JobBase {

    final static Logger log = LoggerFactory.getLogger(IndexDepositTask.class);

    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) 
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

        return Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

            // Keep track of parents as we walk the tree, so
            // tree structure is recorded faithfully
            Deque<Resource> parents = new ArrayDeque<>();

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                log.trace("Visit {}", file);

                recordVisit(rel, RDFS.Resource); // we really don't know what it is

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);

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

                Resource item = Util.resolve(root, relativePath);
                if (!RDFS.Resource.equals(type)) {
                    item.addProperty(RDF.type, type);
                }
                item.addProperty(RDFS.label, relativePath.getFileName().toString());
                parent.addProperty(DCTerms.hasPart, item);
                return item;
            }
        });
    }
    
    public static void main(String[] args) throws JobExecutionException {
        JobBase job = new IndexDepositTask();
        String id = "123456789";
        Resource prov = ModelFactory.createDefaultModel().createResource("repo:" + id);
        Resource item = ModelFactory.createDefaultModel().createResource("repo:" + id);
        
        job.execute(null, item, prov, id, Paths.get("/home/pldms/Development/Projects/2012/data.bris/dundry/working/RDSF_MV/nfs3-exports/marfc-cregan-2011/ACRC_Test_Area/2011/"), null);
        
        System.out.println("========== prov =========");
        prov.getModel().write(System.out, "TTL");
        System.out.println("========== item =========");
        item.getModel().write(System.out, "TTL");
    }
}
