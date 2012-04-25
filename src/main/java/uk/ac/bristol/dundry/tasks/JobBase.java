package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.nio.file.Path;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public abstract class JobBase implements Job {
    
    public final static String REPOSITORY = "jobs-base-repository";
    public final static String ID = "jobs-base-id";
    public final static String PATH = "jobs-base-path";
    
    @Override
    final public void execute(JobExecutionContext jec) throws JobExecutionException {
        JobDataMap jobData = jec.getMergedJobDataMap();
        Repository repo = (Repository) jobData.get(REPOSITORY);
        String id = jobData.getString(ID);
        Resource prov = ModelFactory.createDefaultModel().createResource(id);
        Resource item = ModelFactory.createDefaultModel().createResource(id);
                
        execute(repo, 
                jobData.getString(ID), (Path) jobData.get(PATH), jobData);
        // TODO: mark this as 'completed'
        
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
    
    // Common case: task will go through file system, and put result in store
    // TODO: maybe it should just return a model? or augment a resource?
    abstract public void execute(Repository store, String id, Path root, JobDataMap jobData);
}
