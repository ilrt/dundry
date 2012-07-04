/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.google.common.collect.ImmutableMap;
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
import org.quartz.Job;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.bristol.dundry.model.ResourceCollection;
import uk.ac.bristol.dundry.tasks.DataCiteSubmit;
import uk.ac.bristol.dundry.tasks.JobBase;
import uk.ac.bristol.dundry.tasks.MoveTask;
import uk.ac.bristol.dundry.tasks.StateChanger;
import uk.ac.bristol.dundry.vocabs.RepositoryVocab;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Repository {
    
    public enum State { Created, Depositing, Deposited, Publishing, Published }
    
    static final Logger log = LoggerFactory.getLogger(Repository.class);
    
    // play it safe. radix of 36 is ideal 
    static final int RADIX = Math.min(Character.MAX_RADIX, 36);
    
    @Autowired protected TaskManager taskManager;
    private final FileRepository fileRepo;
    private final MetadataStore mdStore;
    private final List<Class<? extends Job>> defaultJobs;
    private final String publishURLBase;
    
    public Repository(String publishURLBase, FileRepository fileRepo, MetadataStore mdStore, List<String> postDepositJobClasses) {
        this.fileRepo = fileRepo;
        this.mdStore = mdStore;
        // ensure we just need to append id to base
        this.publishURLBase = publishURLBase.endsWith("/") ? publishURLBase : publishURLBase + "/";
        
        // Load up job classes
        defaultJobs = new ArrayList<>();
        for (String jobClassName: postDepositJobClasses) {
            // Try to load the class. Check it is a Job.
            try {
                Class<?> job = Repository.class.getClassLoader().loadClass(jobClassName);
                if (Job.class.isAssignableFrom(job)) defaultJobs.add((Class<? extends Job>) job);
                else log.error("Class <{}> is not a Job. Ignoring.", jobClassName);
            } catch (ClassNotFoundException ex) {
                log.error("Job class <{}> not found. Ignoring.", jobClassName);
            }
        }
    }
    
    public ResourceCollection getIds() {
        Model resultModel = ModelFactory.createDefaultModel();
        ResultSet r = mdStore.query("select distinct ?g ?state ?title ?description ?source "
                + "{ graph ?g1 { "
                + "   ?g <http://vocab.bris.ac.uk/data/repository#state> ?state "
                + "   OPTIONAL { ?g <http://purl.org/dc/terms/source> ?source } "
                + "  } "
                + "  graph ?g2 { "
                + "   ?g <http://purl.org/dc/terms/title> ?title ."
                + "   OPTIONAL { ?g <http://purl.org/dc/terms/description> ?description } "
                + "  } "
                + "}");
        List<Resource> ids = new LinkedList<>();
        while (r.hasNext()) {
            QuerySolution nxt = r.next();
            // get item and copy to resultModel
            Resource item = nxt.getResource("g").inModel(resultModel);
            item.addProperty(RDFS.label, nxt.get("title"));
            if (nxt.contains("source"))
                item.addProperty(DCTerms.source, nxt.get("source"));
            if (nxt.contains("description"))
                item.addProperty(DCTerms.description, nxt.get("description"));
            ids.add( item );
        }
        log.info("Ids is: {}", ids);
        return new ResourceCollection(ids);
    }

    public boolean hasId(String item) {
        return getProvenanceMetadata(item).hasProperty(null);
    }
     
    /**
     * Create a new deposit
     * 
     * @param creator User id who made this deposit
     * @param subject Metadata to include about the created deposit. It will be renamed once and id has been allocated.
     * @return
     * @throws IOException 
     */
    public String create(String creator, Resource subject) throws IOException, SchedulerException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String id = baseEncoded.replace("-",""); // remove sign bits
        
        // Now we have an id rename the subject
        ResourceUtils.renameResource(subject, toInternalId(id));
        
        Path repoDir = fileRepo.create(id);
        
        Resource prov = ModelFactory.createDefaultModel().createResource(toInternalId(id));
        prov.addLiteral(DCTerms.dateSubmitted, Calendar.getInstance());
        prov.addProperty(RepositoryVocab.depositor, creator);
        prov.addProperty(RepositoryVocab.state, State.Created.name());
        
        // Create mutable and immutable graphs
        mdStore.create(toInternalId(id), subject.getModel()); // often a noop
        mdStore.create(toInternalId(id) + "/prov", prov.getModel());
        
        return id;
    }
    
    /**
     * Make a deposit in the 
     * 
     * @param depositTask 
     * @param id The repository id
     * @param source An identifier for the source (will be recorded with deposit)
     */
    public void makeDeposit(JobDetail depositTask, String id, String source) throws SchedulerException {
        Resource prov = getProvenanceMetadata(id);
        
        prov.addProperty(DCTerms.source, source);
        prov.addLiteral(DCTerms.dateSubmitted, Calendar.getInstance());
        prov.removeAll(RepositoryVocab.state);
        prov.addProperty(RepositoryVocab.state, State.Depositing.name());
        
        updateProvenanceMetadata(id, prov);
        
        // Add in default post-deposit tasks
        List<JobDetail> jobDetails = new ArrayList<>();
        jobDetails.add(depositTask);
        
        // Create context for these jobs
        JobDataMap jobData = new JobDataMap();
        jobData.putAll(ImmutableMap.of(
                    JobBase.PATH, fileRepo.depositPathForId(id),
                    JobBase.REPOSITORY, this,
                    JobBase.ID, id));
        
        // Create instances from default jobs
        for (Class<? extends Job> job: defaultJobs) {
            JobDetail jobDetail = newJob(job)
                    .withIdentity(job.getName(), id)
                    .usingJobData(jobData)
                    .build();
            
            jobDetails.add(jobDetail);
        }
        
        // Add a final state changer at the end
        JobDataMap stateChangeMap = new JobDataMap();
        stateChangeMap.putAll(jobData); // standard stuff
        stateChangeMap.put(StateChanger.TO_STATE, State.Deposited.name());
        
        JobDetail stateChangeJob = newJob(StateChanger.class)
                .withIdentity("State changer", id)
                .usingJobData(stateChangeMap)
                .build();
                
        jobDetails.add(stateChangeJob);
        
        taskManager.executeJobsInOrder(id, jobDetails);
    }
    
    public void publish(String id) throws SchedulerException {
        Resource prov = getProvenanceMetadata(id);
        prov.addLiteral(DCTerms.date, Calendar.getInstance());
        prov.removeAll(RepositoryVocab.state);
        prov.addLiteral(RepositoryVocab.state, State.Publishing.name());
        updateProvenanceMetadata(id, prov);
        
        // Add in default publish tasks
        List<JobDetail> jobDetails = new ArrayList<>();
        
        // Create context for these jobs
        JobDataMap jobData = new JobDataMap();
        jobData.putAll(ImmutableMap.of(
                    JobBase.PATH, fileRepo.depositPathForId(id),
                    JobBase.REPOSITORY, this,
                    JobBase.ID, id));
        
        JobDetail registerJob = newJob(DataCiteSubmit.class)
                .withIdentity("Datacite submit", id)
                .usingJobData(jobData)
                .build();
        
        jobDetails.add(registerJob);
        
        JobDataMap moveMap = new JobDataMap();
        moveMap.putAll(ImmutableMap.of(
                MoveTask.FROM, getDepositPathForId(id),
                MoveTask.TO, getPublishPathForId(id)
                ));
        JobDetail moveJob = newJob(MoveTask.class)
                .withIdentity("Move", id)
                .usingJobData(moveMap)
                .build();
        
        jobDetails.add(moveJob);
        
        // Add a final state changer at the end
        JobDataMap stateChangeMap = new JobDataMap();
        stateChangeMap.putAll(jobData); // standard stuff
        stateChangeMap.put(StateChanger.TO_STATE, State.Published.name());
        
        JobDetail stateChangeJob = newJob(StateChanger.class)
                .withIdentity("State changer", id)
                .usingJobData(stateChangeMap)
                .build();
                
        jobDetails.add(stateChangeJob);
        
        taskManager.executeJobsInOrder(id, jobDetails);
    }
    
    public Resource getMetadata(String id) {
        String internalId = toInternalId(id);
        
        Model m = ModelFactory.createDefaultModel();
        m.add(mdStore.getData(internalId));
        m.add(mdStore.getData(internalId + "/prov"));
        return m.createResource(internalId);
    }
    
    public void updateMetadata(String id, Resource r) {
        String internalId = toInternalId(id);
        
        // Replace metadata with new information that's not in prov
        Model m = ModelFactory.createDefaultModel();
        m.add(r.getModel());
        // Remove provenance info
        m.remove(mdStore.getData(internalId + "/prov"));
        mdStore.replaceData(internalId, m);
    }
    
    public Resource getProvenanceMetadata(String id) {
        String internalId = toInternalId(id);
        
        return mdStore.getData(internalId + "/prov").createResource(internalId);
    }
    
    public void updateProvenanceMetadata(String id, Resource r) {
        String internalId = toInternalId(id);
        
        mdStore.replaceData(internalId + "/prov", r.getModel());
    }
   
    public Path getDepositPathForId(String id) {
        return fileRepo.depositPathForId(id);
    }
    
    public Path getPublishPathForId(String id) {
        return fileRepo.publishPathForId(id);
    }
    
    public String getPublishedURL(String id) {
        return publishURLBase + id;
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
