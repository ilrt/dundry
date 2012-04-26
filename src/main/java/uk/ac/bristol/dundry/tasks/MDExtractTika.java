package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.quartz.JobDataMap;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MDExtractTika extends JobBase {
    
    static final org.slf4j.Logger log = LoggerFactory.getLogger(MDExtractTika.class);
    
    @Override
    public void execute(Resource item, Resource prov, String id, Path root, JobDataMap jobData) {
        try {
            Files.walkFileTree(root, new MDExtractVisitor(root, item, prov));
        } catch (IOException ex) {
            log.error("Error extracting metadata under {}: {}", root, ex);
        }
    }
    
    static class MDExtractVisitor extends SimpleFileVisitor<Path> {
        private final Resource item;
        private final Resource prov;
        private final Path root;

        private MDExtractVisitor(Path root, Resource item, Resource prov) {
            this.root = root;
            this.item = item;
            this.prov = prov;
        }
        
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path rel = root.relativize(file);
            
            Parser parser = new AutoDetectParser(); // Should auto-detect!
            ContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            
            try (InputStream in = Files.newInputStream(file)) {
                parser.parse(in, handler, metadata, context);
                storeMetadata(metadata, rel, item);
            } catch (SAXException | TikaException e) {
                log.error("Error extracting metadata from {}: {}", file, e);
            }

            return FileVisitResult.CONTINUE;
        }

        private void storeMetadata(Metadata metadata, Path rel, Resource item) {
            // TODO: utility functions doing this cleanly everywhere
            Resource subject = item.getModel().createResource(item.getURI() + "/" + rel);
            
            
        }
    }
}
