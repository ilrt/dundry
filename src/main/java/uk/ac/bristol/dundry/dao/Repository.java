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
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.model.ResourceCollection;

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
    
    public ResourceCollection getIds() {
        Model resultModel = ModelFactory.createDefaultModel();
        ResultSet r = mdStore.query("select distinct ?g ?title ?description ?source "
                + "{ graph ?g1 { "
                + "?g <http://purl.org/dc/terms/title> ?title ;"
                + "   <http://purl.org/dc/terms/source> ?source . "
                + "OPTIONAL { ?g <http://purl.org/dc/terms/description> ?description } "
                + "} }");
        List<Resource> ids = new LinkedList<>();
        while (r.hasNext()) {
            QuerySolution nxt = r.next();
            // get item and copy to resultModel
            Resource item = nxt.getResource("g").inModel(resultModel);
            item.addLiteral(RDFS.label, nxt.get("title"));
            item.addLiteral(DCTerms.source, nxt.get("source"));
            if (nxt.contains("description"))
                item.addLiteral(DCTerms.description, nxt.get("description"));
            ids.add( item );
        }
        log.info("Ids is: {}", ids);
        return new ResourceCollection(ids);
    }
    
    /**
     * Deposit source into the repository
     * 
     * @param source Path to the root of the file(s) to deposit
     * @param creator User id who made this deposit
     * @param subject Metadata to include about the created deposit. It will be renamed once and id has been allocated.
     * @return
     * @throws IOException 
     */
    public String create(Path source, String creator, Resource subject) throws IOException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String id = baseEncoded.replace("-",""); // remove sign bits
        
        // Now we have an id rename the subject
        ResourceUtils.renameResource(subject, toInternalId(id));
        
        Path repoDir = fileRepo.create(id, source);
        
        subject.addLiteral(DCTerms.dateSubmitted, subject.getModel().createTypedLiteral(Calendar.getInstance()));
        subject.addLiteral(DCTerms.source, source.toAbsolutePath().toString());
        subject.addProperty(DCTerms.creator, creator);
        
        mdStore.create(toInternalId(id), subject.getModel());
        
        return id;
    }
    
    public Resource getMetadata(String id) {
        return mdStore.getDataAbout(toInternalId(id)).createResource(toInternalId(id));
    }
    
    public void updateMetadata(String id, Resource r) {
        mdStore.replaceData(toInternalId(id), r.getModel());
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
        if (id.startsWith("http://") || id.startsWith("file://")) return id;
        else return "repo:" + id;
    }
}
