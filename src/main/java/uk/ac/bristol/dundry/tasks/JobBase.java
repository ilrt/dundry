package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.nio.file.Path;
import java.util.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.dao.Repository.State;
import uk.ac.bristol.dundry.vocabs.OPMV;
import uk.ac.bristol.dundry.vocabs.RepositoryVocab;

/**
 * Useful base class which should be used for all dundry tasks.
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public abstract class JobBase implements Job {
    
    private static final Logger log = LoggerFactory.getLogger(JobBase.class);
    
    public final static String REPOSITORY = "jobs-base-repository";
    public final static String ID = "jobs-base-id";
    
    /**
     * Job execution which wraps a more convenient interface for dundry, which
     * will also record when job was run and other information.
     * @param jec
     * @throws JobExecutionException 
     */
    @Override
    final public void execute(JobExecutionContext jec) throws JobExecutionException {
        JobDataMap jobData = jec.getMergedJobDataMap();
        
        
        Repository repo = (Repository) jobData.get(REPOSITORY);
        
        String id = jobData.getString(ID);
        Resource prov = ModelFactory.createDefaultModel().createResource(Repository.toInternalId(id));
        Resource item = ModelFactory.createDefaultModel().createResource(Repository.toInternalId(id));
        
        Resource task = prov.getModel().createResource();
        prov.addProperty(OPMV.wasGeneratedBy, task);
        task.addLiteral(OPMV.wasStartedAt, Calendar.getInstance());
        task.addProperty(OPMV.used, this.getClass().getCanonicalName());
        
        if (log.isDebugEnabled()) {
            log.debug("Running job {} with arguments: ID => '{}'",
                this.getClass(), jobData.getString(ID));
        }
        // If state is _not_ published location is given by deposit path
        State state = repo.getState(id);
        
        Path currentPath = (state == State.Published) ?
                repo.getPublishPathForId(id) : repo.getDepositPathForId(id);
        
        execute(repo, item, prov, id, currentPath, jobData);
        
        task.addLiteral(OPMV.wasEndedAt, Calendar.getInstance());
        
        // Update provenance (bad name) data
        if (prov.getModel().size() > 0) {
            Resource currProv = repo.getProvenanceMetadata(id);
            currProv.getModel().add(prov.getModel());
            repo.updateProvenanceMetadata(id, currProv);
        }
        
        // Update regular data
        if (item.getModel().size() > 0) {
            Resource curr = repo.getMetadata(id);
            curr.getModel().add(item.getModel());
            repo.updateMetadata(id, curr);
        }
        
        
    }
    
    /**
     * Perform some work on the repository based at root.
     * 
     * You can write data to either item or prov. The former represents this repository
     * in the publicly mutable store, the latter is only modifiable from within the
     * repository.
     * 
     * @param repo The repository. Be careful! If you're just adding data use item and prov.
     * @param item Data which will be added to the mutable repo dataset
     * @param prov Data which will be added to the externally immutable repo dataset
     * @param id Id of the repository
     * @param root Location of data in filesystem
     * @param jobData General runtime context
     */
    abstract public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData)
            throws JobExecutionException;
}
