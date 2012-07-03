package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
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
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class DataCiteSubmit extends JobBase {
    
    final String DOI_ENDPOINT = "https://test.datacite.org/mds/doi?testMode=true";
    final String PREFIX = "10.4124"; // 4124 or 5072??
    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        try {
            
            // see https://test.datacite.org/mds/static/apidoc
            
            DefaultHttpClient httpClient = new DefaultHttpClient();
            
            CredentialsProvider cp = new BasicCredentialsProvider();
            
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("BL.JISC-MRD","happyDOI12345"));
            
            httpClient.setCredentialsProvider(cp);
            
            HttpPost httpPost = new HttpPost(DOI_ENDPOINT);
            httpPost.setHeader("Content-Type", "text/plain;charset=UTF-8");
            
            String doi = PREFIX + "/" + id;
            String url = repo.getPublishedURL(id);
            
            String message = String.format("doi=%s\nurl=%s\n", doi, url);
            System.err.println(message);
            httpPost.setEntity(new StringEntity(message));
            HttpResponse response = httpClient.execute(httpPost);
            
            int sc = response.getStatusLine().getStatusCode();
            String res = EntityUtils.toString(response.getEntity());
            if ((sc / 100) != 2) {
                System.err.printf("Request failed (%s): %s\n", sc, res);
            } else {
                prov.addProperty(DCTerms.identifier, doi);
                System.err.printf("Request succeeded (%s): %s\n", sc, res);
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
        
        Repository r = new Repository("http://example.com/deposits/",null,null,Collections.EMPTY_LIST);
        
        job.execute(r, item, prov, id, Paths.get("/home/pldms/Development/Projects/2012/data.bris/dundry/working/example/"), null);
        
        System.out.println("========== prov =========");
        prov.getModel().write(System.out, "TTL");
        System.out.println("========== item =========");
        item.getModel().write(System.out, "TTL");
    }
}
