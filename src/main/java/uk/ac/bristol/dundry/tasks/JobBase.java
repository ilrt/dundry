package uk.ac.bristol.dundry.tasks;

import java.nio.file.Path;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import uk.ac.bristol.dundry.dao.MetadataStore;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public abstract class JobBase implements Job {

    @Override
    final public void execute(JobExecutionContext jec) throws JobExecutionException {
        JobDataMap jobData = jec.getMergedJobDataMap();
        execute((MetadataStore) jobData.get("store"), 
                jobData.getString("id"), (Path) jobData.get("path"), jobData);
        // TODO: mark this as 'completed'
    }
    
    // Common case: task will go through file system, and put result in store
    // TODO: maybe it should just return a model? or augment a resource?
    abstract public void execute(MetadataStore store, String id, Path root, JobDataMap jobData);
    
    abstract public String getName();
    abstract public String getDescription();
}
