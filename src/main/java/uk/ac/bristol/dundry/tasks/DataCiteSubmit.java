package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.MetadataStoreDS;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.vocabs.Bibo;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class DataCiteSubmit extends JobBase {
    
    final static Logger log = LoggerFactory.getLogger(DataCiteSubmit.class);
    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        String username = jobData.getString("datacite.username");
        String password = jobData.getString("datacite.password");
        String endpoint = jobData.getString("datacite.endpoint");
        String doiprefix = jobData.getString("datacite.doiprefix");
        
        try {
            // see https://test.datacite.org/mds/static/apidoc
            
            DefaultHttpClient httpClient = new DefaultHttpClient();
            
            CredentialsProvider cp = new BasicCredentialsProvider();
            
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username,password));
            
            httpClient.setCredentialsProvider(cp);
            
            HttpPost httpPost = new HttpPost(endpoint);
            httpPost.setHeader("Content-Type", "text/plain;charset=UTF-8");
            
            String doi = doiprefix + "/" + id;
            String url = repo.getPublishedURL(id);
            
            String message = String.format("doi=%s\nurl=%s\n", doi, url);
            
            log.info("Submitting to datacite: \n{}\n", message);
            
            httpPost.setEntity(new StringEntity(message));
            HttpResponse response = httpClient.execute(httpPost);
            
            int sc = response.getStatusLine().getStatusCode();
            String res = EntityUtils.toString(response.getEntity());
            if ((sc / 100) != 2) {
                log.info("Request failed ({}): {}\n", sc, res);
            } else {
                prov.addProperty(Bibo.doi, doi);
                log.info("Request succeeded ({}): {}\n", sc, res);
            }
            
        } catch (MalformedURLException ex) {
            throw new JobExecutionException("URL is malformed", ex);
        } catch (IOException ex) {
            throw new JobExecutionException("Issue communicating with datacite", ex);
        }
    }
    
    public static void main(String[] args) throws JobExecutionException {
        JobBase job = new DataCiteSubmit();
        String id = "XXXX123456789";
        Resource prov = ModelFactory.createDefaultModel().createResource("repo:" + id);
        Resource item = ModelFactory.createDefaultModel().createResource("repo:" + id);
        
        Dataset ds = new DatasetImpl(ModelFactory.createDefaultModel());
        
        ds.addNamedModel("repo:" + id, ModelFactory.createDefaultModel());
        ds.addNamedModel("repo:" + id + "/prov", ModelFactory.createDefaultModel());
        
        Repository r = new Repository("http://example.com/deposits/",null,new MetadataStoreDS(ds),Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        
        job.execute(r, item, prov, id, Paths.get("/home/pldms/Development/Projects/2012/data.bris/dundry/working/example/"), null);
        
        System.out.println("========== prov =========");
        prov.getModel().write(System.out, "TTL");
        System.out.println("========== item =========");
        item.getModel().write(System.out, "TTL");
    }
    
    // Write item out in datacite format
    public void toDataCite(Resource item, String doi, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument();
        
        // Preamble
        writer.setDefaultNamespace("http://datacite.org/schema/kernel-2.2");
        writer.writeStartElement("resource");
        writer.writeDefaultNamespace("http://datacite.org/schema/kernel-2.2");
        writer.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        writer.writeAttribute("xsi:schemaLocation", "http://datacite.org/schema/kernel-2.2 http://schema.datacite.org/meta/kernel-2.2/metadata.xsd");
        
        // Titles
        writer.writeStartElement("titles");
        write(item, DCTerms.title, "title", writer);
        write(item, DCTerms.alternative, "title", writer, "titleType", "Alternative");
        writer.writeEndElement();
        
        // Description
        writer.writeStartElement("descriptions");
        write(item, DCTerms.description, "description", writer);
        writer.writeEndElement();
        
        
        
        // Close root and document
        writer.writeEndElement();
        writer.writeEndDocument();
        
        writer.flush();
    }
    
    /**
     * Write out rdf values, if present, to XML stream
     * 
     * @param item Resource being written
     * @param property Property to write
     * @param element XML element to use
     * @param writer XML stream to write to
     * @param attVals Addition attribute values to include
     */
    private void write(Resource item, Property property, String element, 
            XMLStreamWriter writer, String... attVals) throws XMLStreamException {
        StmtIterator si = item.listProperties(property);
        while (si.hasNext()) {
            writer.writeStartElement(element);
            // Write attributes out
            for (int i = 0; i < attVals.length; i+=2) {
                writer.writeAttribute(attVals[i], attVals[i+1]);
            }
            
            // Write value
            writer.writeCharacters(toString(si.next().getObject()));
            
            writer.writeEndElement();
        }
    }

    
    private String toString(RDFNode object) {
        if (object.isURIResource()) return object.asResource().getURI();
        else if (object.isLiteral()) return object.asLiteral().getLexicalForm();
        else return "";
    }
}
