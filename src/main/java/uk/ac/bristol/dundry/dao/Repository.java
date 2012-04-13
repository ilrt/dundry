/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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
import java.util.*;
import java.util.logging.Level;
import org.quartz.Job;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.bristol.dundry.model.ResourceCollection;
import uk.ac.bristol.dundry.tasks.CopyTask;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Repository {
    
    static final Logger log = LoggerFactory.getLogger(Repository.class);
    
    // play it safe. radix of 36 is ideal
    static final int RADIX = Math.min(Character.MAX_RADIX, 36);
    
    @Autowired private TaskManager taskManager;
    private final FileRepository fileRepo;
    private final MetadataStore mdStore;
    private final List<Class<? extends Job>> jobs;
    
    public Repository(FileRepository fileRepo, MetadataStore mdStore, List<String> jobsClasses) {
        this.fileRepo = fileRepo;
        this.mdStore = mdStore;
        
        // Load up job classes
        jobs = new ArrayList<>();
        for (String jobClassName: jobsClasses) {
            // Try to load the class. Check it is a Job.
            try {
                Class<?> job = Repository.class.getClassLoader().loadClass(jobClassName);
                if (Job.class.isAssignableFrom(job)) jobs.add((Class<? extends Job>) job);
                else log.error("Class <{}> is not a Job. Ignoring.", jobClassName);
            } catch (ClassNotFoundException ex) {
                log.error("Job class <{}> not found. Ignoring.", jobClassName);
            }
        }
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
            item.addProperty(RDFS.label, nxt.get("title"));
            item.addProperty(DCTerms.source, nxt.get("source"));
            if (nxt.contains("description"))
                item.addProperty(DCTerms.description, nxt.get("description"));
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
    public String create(Path source, String creator, Resource subject) throws IOException, SchedulerException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String id = baseEncoded.replace("-",""); // remove sign bits
        
        // Now we have an id rename the subject
        ResourceUtils.renameResource(subject, toInternalId(id));
        
        Path repoDir = fileRepo.create(id, source);
        
        subject.addLiteral(DCTerms.dateSubmitted, Calendar.getInstance());
        subject.addLiteral(DCTerms.source, source.toAbsolutePath().toString());
        subject.addProperty(DCTerms.creator, creator);
        
        mdStore.create(toInternalId(id), subject.getModel());
        
        startTasks(id, CopyTask.class, CopyTask.FROM, source, CopyTask.TO, repoDir);
        
        return id;
    }
    
    /**
     * Begin the post-creation tasks.
     * 
     * @param loadJob First task to get get data into the repository
     * @param settings 
     */
    private void startTasks(String id, Class<? extends Job> loadJob, Object... settings) throws SchedulerException {
        Map<String, Object> context = new HashMap<>();
        
        for (int i = 0; i < settings.length; i++) {
            context.put((String) settings[i], settings[i+1]);
        }
        
        List<Class<? extends Job>> allJobs = new ArrayList<>();
        
        allJobs.add(loadJob);
        allJobs.addAll(jobs);
        
        taskManager.startJobs(id, allJobs, context);
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
