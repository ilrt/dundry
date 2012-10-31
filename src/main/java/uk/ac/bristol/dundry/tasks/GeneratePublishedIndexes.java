package uk.ac.bristol.dundry.tasks;

import com.google.common.collect.ImmutableMap;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.vocabs.Bibo;

/**
 * Create files for web publishing: header and footer for apache index, plus write
 * out metadata
 * 
 * @author pldms
 */
public class GeneratePublishedIndexes extends JobBase {
    
    final static Logger log = LoggerFactory.getLogger(GeneratePublishedIndexes.class);
    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        // Fetch settings
        String templateBase = jobData.getString("indexpublish.templatebase");
        String headerTemplate = jobData.getString("indexpublish.headertemplate");
        String footerTemplate = jobData.getString("indexpublish.footertemplate");
        String headerFilename = jobData.getString("indexpublish.headerfilename");
        String footerFilename = jobData.getString("indexpublish.footerfilename");
        String rdfFilename = jobData.getString("indexpublish.rdffilename");
        
        log.info("Generate index for publishing: {} {} {} {} {} {} {}",
                new String[]{id, templateBase, headerTemplate, footerTemplate, headerFilename, footerFilename, rdfFilename});
        
        // Make a freemarker configuration
        Configuration conf = new Configuration();
        conf.setClassForTemplateLoading(GeneratePublishedIndexes.class, templateBase);
        conf.setObjectWrapper(new DefaultObjectWrapper());
        
        // Our model for rendering
        Map<String, String> model = ImmutableMap.of(
            "title", getValue(item, DCTerms.title), 
            "description", getValue(item, DCTerms.description),
            "doi", getValue(item, Bibo.doi)
        );
        
        // Write header and footer
        write(model, conf, headerTemplate, root.resolve(headerFilename));
        write(model, conf, footerTemplate, root.resolve(footerFilename));
        
        // Write rdf out
        Path rdfOutputTarget = root.resolve(rdfFilename);
        try (OutputStream out = Files.newOutputStream(rdfOutputTarget)) {
            log.debug("Writing mode to {}", rdfOutputTarget); 
            item.getModel().write(out, "RDF/XML-ABBREV", item.getURI());
        } catch (IOException ex) {
            log.error("Issue writing rdf file", ex);
            throw new JobExecutionException("Problem writing rdf file", ex);
        }
    }
    
    // Safe get string value
    private String getValue(Resource r, Property p) {
        if (r.hasProperty(p)) return r.getProperty(p).getString();
        else return "";
    }
    
    private void write(Map<String, String> model, Configuration conf, String templateName, Path target) throws JobExecutionException {
        log.debug("Write template {} with model {} to {}", new Object[] {templateName, model, target});
        
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            Template template = conf.getTemplate(templateName);
            template.process(model, writer);
            writer.flush();
        } catch (TemplateException | IOException ex) {
            log.error("Problem executing template {}: {}", templateName, ex);
            throw new JobExecutionException("Error running template", ex);
        }
    }
    
    public static void main(String... args) throws JobExecutionException {
        Model data = ModelFactory.createDefaultModel();
        Resource r = data.createResource("repo:xkjkj989890asd");
        
        r.addLiteral(DCTerms.title, "Test title");
        r.addLiteral(DCTerms.description, "And this is a description");
        //r.addLiteral(Bibo.doi, "10.142/xkjkj989890asd");
        
        GeneratePublishedIndexes instance = new GeneratePublishedIndexes();
        
        JobDataMap jd = new JobDataMap();
        jd.put("indexpublish.templatebase", "/templates");
        jd.put("indexpublish.headertemplate", "header.ftl");
        jd.put("indexpublish.footertemplate", "footer.ftl");
        jd.put("indexpublish.headerfilename", ".header.html");
        jd.put("indexpublish.footerfilename", ".footer.html");
        jd.put("indexpublish.rdffilename", ".info.rdf");
        
        instance.execute(null, r, null, "xkjkj989890asd", Paths.get("/tmp/dundry-src"), jd);
    }
}
