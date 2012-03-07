/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.model.DepositDescription;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Repository {
    
    static final Logger log = LoggerFactory.getLogger(Repository.class);
    
    // play it safe. radix of 36 is ideal
    static final int RADIX = Math.min(Character.MAX_RADIX, 36);
    
    private final FileRepository fileRepo;
    private final MetadataStore mdStore;
    
    public Repository(FileRepository fileRepo, MetadataStore mdStore) {
        this.fileRepo = fileRepo;
        this.mdStore = mdStore;
    }
    
    public List<DepositDescription> getIds() {
        ResultSet r = mdStore.query("select distinct ?g ?title { graph ?g1 { ?g <http://purl.org/dc/terms/title> ?title } }");
        List<DepositDescription> ids = new LinkedList<>();
        while (r.hasNext()) {
            QuerySolution nxt = r.next();
            ids.add( new DepositDescription(
                        toExternalId(nxt.getResource("g").getURI()),
                        nxt.getLiteral("title").getLexicalForm()
                    ) );
        }
        log.info("Ids is: {}", ids);
        return ids;
    }
    
    public String create(Path source, String title, String description, String creator) throws IOException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String id = baseEncoded.replace("-",""); // remove sign bits
        
        Path repoDir = fileRepo.create(id, source);
        
        Model model = ModelFactory.createDefaultModel();
        Resource subject = model.createResource(toInternalId(id));
        subject.addLiteral(DCTerms.dateSubmitted, model.createTypedLiteral(Calendar.getInstance()));
        subject.addProperty(DCTerms.source, model.createResource(source.toUri().toString()));
        subject.addProperty(DCTerms.creator, creator);
        subject.addProperty(DCTerms.identifier, id);
        subject.addProperty(DCTerms.title, title);
        if (description != null) subject.addProperty(DCTerms.description, description);
        
        mdStore.create(toInternalId(id), model);
        
        return id;
    }
    
    public Resource getMetadata(String id) {
        return mdStore.getDataAbout(toInternalId(id)).createResource(toInternalId(id));
    }
    
    /**
     * Takes an id and makes it suitable for external use by stripping off
     * leading 'repo:' if present
     * @param uri
     * @return An un-repo'd string
     */
    public static String toExternalId(String uri) {
        if (uri.startsWith("repo:")) return uri.substring(5);
        else return uri;
    }
    
    /**
     * Make an internal uri from id
     * @param id
     * @return 
     */
    public static String toInternalId(String id) {
        return "repo:" + id;
    }
}
