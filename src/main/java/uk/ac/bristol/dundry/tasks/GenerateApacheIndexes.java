package uk.ac.bristol.dundry.tasks;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import com.hp.hpl.jena.vocabulary.DCTerms;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.MetadataStore;
import uk.ac.bristol.dundry.dao.MetadataStoreDS;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.vocabs.Bibo;

/**
 * Create files for web publishing: header and footer for apache index, plus
 * write out metadata
 *
 * @author pldms
 */
public class GenerateApacheIndexes extends JobBase {

    final static Logger log = LoggerFactory.getLogger(GenerateApacheIndexes.class);

    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        // Fetch settings
        String templateBase = jobData.getString("indexpublish.templatebase");
        String headerTemplate = jobData.getString("indexpublish.headertemplate");
        String footerTemplate = jobData.getString("indexpublish.footertemplate");
        String headerFilename = jobData.getString("indexpublish.headerfilename");
        String footerFilename = jobData.getString("indexpublish.footerfilename");
        String rdfFilename = jobData.getString("indexpublish.rdffilename");

        log.info("Generate index for publishing: '{}' <{}> <{}> <{}> <{}> <{}> <{}>",
                new String[]{id, templateBase, headerTemplate, footerTemplate, headerFilename, footerFilename, rdfFilename});

        // Get item metadata
        Resource metadata = repo.getMetadata(id);

        // Make a freemarker configuration
        Configuration conf = new Configuration();
        conf.setClassForTemplateLoading(GenerateApacheIndexes.class, templateBase);
        conf.setObjectWrapper(new DefaultObjectWrapper());

        // Our model for rendering
        Map<String, String> model = ImmutableMap.of(
                "title", getValue(metadata, DCTerms.title),
                "description", getValue(metadata, DCTerms.description),
                "doi", getValue(metadata, Bibo.doi));

        // Write header and footer
        write(model, conf, headerTemplate, root.resolve(headerFilename));
        write(model, conf, footerTemplate, root.resolve(footerFilename));

        // Link header and footer into all sub directories
        linkSubtree(root.resolve(headerFilename), root);
        linkSubtree(root.resolve(footerFilename), root);

        // Write rdf out
        Path rdfOutputTarget = root.resolve(rdfFilename);
        try (OutputStream out = Files.newOutputStream(rdfOutputTarget)) {
            log.debug("Writing mode to {}", rdfOutputTarget);
            metadata.getModel().write(out, "RDF/XML-ABBREV", item.getURI());
        } catch (IOException ex) {
            log.error("Issue writing rdf file", ex);
            throw new JobExecutionException("Problem writing rdf file", ex);
        }
    }

    // Safe get string value
    private String getValue(Resource r, Property p) {
        if (r.hasProperty(p)) {
            return r.getProperty(p).getString();
        } else {
            return "";
        }
    }

    private void write(Map<String, String> model, Configuration conf, String templateName, Path target) throws JobExecutionException {
        log.debug("Write template {} with model {} to {}", new Object[]{templateName, model, target});

        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            Template template = conf.getTemplate(templateName);
            template.process(model, writer);
            writer.flush();
        } catch (TemplateException | IOException ex) {
            log.error("Problem executing template {}: {}", templateName, ex);
            throw new JobExecutionException("Error running template", ex);
        }
    }
    
    // Make links to target from all subdirs of root
    // For populating apache index
    private Path linkSubtree(final Path target, final Path root) throws JobExecutionException {
        try {
            return Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!root.equals(dir)) {
                        // same file name as target, but in dir
                        Path link = dir.resolve(target.getFileName());
                        // relative path to target from dir
                        Path backToTarget = dir.relativize(target);
                        log.debug("Link <{}> to <{}>", link, backToTarget);
                        Files.createSymbolicLink(
                            dir.resolve(target.getFileName()), 
                            dir.relativize(target));
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            throw new JobExecutionException("Problem linking apache index", ex);
        }
    }

    public static void main(String... args) throws JobExecutionException {
        Model data = ModelFactory.createDefaultModel();
        Resource r = data.createResource("repo:xkjkj989890asd");

        r.addProperty(DCTerms.title, "Test title");
        r.addProperty(DCTerms.description, "And this is a description");
        //r.addProperty(Bibo.doi, "10.142/xkjkj989890asd");
        
        DatasetGraph ds = DatasetLib.createDatasetGraphMem();
        
        ds.addGraph(Node.createURI("repo:xkjkj989890asd"), data.getGraph());
        
        MetadataStore mStore = new MetadataStoreDS(DatasetImpl.wrap(ds));
        Repository repo = new Repository("", null, mStore, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        
        GenerateApacheIndexes instance = new GenerateApacheIndexes();

        JobDataMap jd = new JobDataMap();
        jd.put("indexpublish.templatebase", "/templates");
        jd.put("indexpublish.headertemplate", "header.ftl");
        jd.put("indexpublish.footertemplate", "footer.ftl");
        jd.put("indexpublish.headerfilename", ".header.html");
        jd.put("indexpublish.footerfilename", ".footer.html");
        jd.put("indexpublish.rdffilename", ".info.rdf");

        instance.execute(repo, r, null, "xkjkj989890asd", Paths.get("/tmp/dundry-src"), jd);
    }
}
