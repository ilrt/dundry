package uk.ac.bristol.dundry.tasks;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import uk.ac.bristol.dundry.Util;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MDExtractTika extends JobBase {
    
    static final org.slf4j.Logger log = LoggerFactory.getLogger(MDExtractTika.class);
    
    static final Map<String, Property> FIELD_MAPPING =
            new HashMap<String, Property>() {{
                put(HttpHeaders.CONTENT_TYPE, DCTerms.format);
                put(HttpHeaders.CONTENT_TYPE, DCTerms.format);
                put(DublinCore.TITLE, DCTerms.title);
                put(DublinCore.MODIFIED, DCTerms.modified);
                put(DublinCore.DATE.getName(), DCTerms.date);
                put(HttpHeaders.CONTENT_LENGTH, DCTerms.extent);
            }};

    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) {
        try {
            log.debug("Extract metadata from <{}>", root);
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
                        
            Parser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            
            try (InputStream in = Files.newInputStream(file)) {
                parser.parse(in, handler, metadata, context);
                log.debug("Metadata for {}: {}", file, metadata);
                storeMetadata(metadata, rel, item);
            } catch (SAXException | TikaException e) {
                log.error("Error extracting metadata from {}: {}", file, e);
            }

            return FileVisitResult.CONTINUE;
        }

        private void storeMetadata(Metadata metadata, Path rel, Resource item) {
            Resource subject = Util.resolve(item, rel);
            
            for (Entry<String, Property> map: FIELD_MAPPING.entrySet()) {
                String[] values = metadata.getValues(map.getKey());
                if (values != null && values.length > 0) {
                    for (String value: values) subject.addProperty(map.getValue(), value);
                }
            }
        }
    }
    
    public static void main(String[] args) throws JobExecutionException {
        JobBase job = new MDExtractTika();
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
