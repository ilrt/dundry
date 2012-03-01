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
    
    public List<String> getIds() {
        ResultSet r = mdStore.query("select distinct ?g { graph ?g { } }");
        List<String> ids = new LinkedList<>();
        while (r.hasNext()) {
            QuerySolution nxt = r.next();
            ids.add(nxt.getResource("g").getURI().replaceFirst("^repo:", ""));
        }
        return ids;
    }
    
    public String create(Path source) throws IOException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String id = baseEncoded.replace("-",""); // remove sign bits
        
        Path repoDir = fileRepo.create(id, source);
        
        Model model = ModelFactory.createDefaultModel();
        Resource subject = model.createResource("repo:" + id);
        subject.addLiteral(DCTerms.dateSubmitted, model.createTypedLiteral(Calendar.getInstance()));
        subject.addProperty(DCTerms.source, model.createResource(source.toUri().toString()));
        subject.addProperty(DCTerms.creator, "unknown");
        subject.addProperty(DCTerms.identifier, id);
        
        mdStore.create("repo:" + id, model);
        
        return id;
    }
    
    public Resource getMetadata(String id) {
        return mdStore.getDataAbout("repo:" + id).createResource("repo:" + id);
    }
}
