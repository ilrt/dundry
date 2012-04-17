package uk.ac.bristol.dundry.tasks;

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
        execute((Repository) jobData.get(REPOSITORY), 
                jobData.getString(ID), (Path) jobData.get(PATH), jobData);
        // TODO: mark this as 'completed'
    }
    
    // Common case: task will go through file system, and put result in store
    // TODO: maybe it should just return a model? or augment a resource?
    abstract public void execute(Repository store, String id, Path root, JobDataMap jobData);
}
