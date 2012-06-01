package uk.ac.bristol.dundry.dao;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobChainingJobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.model.ResourceCollection;
import uk.ac.bristol.dundry.tasks.ChainTerminator;
import uk.ac.bristol.dundry.vocabs.OPMV;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
public class TaskManager {
    
    static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    
    private final Scheduler scheduler;
    
    public TaskManager() throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.start();
    }
    
    public /*List<JobExecutionContext>*/ List<Object> listAllTasks() throws SchedulerException {
        List<Object> stuff = new ArrayList<>();
        stuff.add("-- Executing --");
        stuff.addAll(scheduler.getCurrentlyExecutingJobs());
        stuff.add("------");
        stuff.addAll(scheduler.getJobKeys(GroupMatcher.jobGroupContains("")));
        stuff.add("---- Listeners -----");
        stuff.addAll(scheduler.getListenerManager().getJobListeners());
        return stuff;
    }
    
    public ResourceCollection listTasks(String id) throws SchedulerException {
        final Model m = ModelFactory.createDefaultModel();
        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(id));
        Collection<Resource> tasks = 
                Collections2.transform(keys, new Function<JobKey, Resource>() {

            @Override
            public Resource apply(JobKey f) {
                JobDetail detail;
                try {
                    detail = scheduler.getJobDetail(f);
                } catch (SchedulerException ex) {
                    throw new RuntimeException("Problem getting job detail", ex);
                }
                Resource job = m.createResource();
                if (detail.getDescription() != null)
                    job.addProperty(DCTerms.description, detail.getDescription());
                job.addProperty(DCTerms.title, f.getName());
                return job;
            }
        });
        
        return new ResourceCollection(tasks);
    }
    
    /**
     * 
     * @param id
     * @param jobs
     * @throws SchedulerException 
     */
    public void executeJobsInOrder(String id, List<JobDetail> jobs) throws SchedulerException {
        if (jobs.isEmpty()) {
            return;
        }
        
        // Add all defaultJobs to scheduler
        for (JobDetail jd: jobs) scheduler.addJob(jd, true);
        
        // Make a chain of these defaultJobs 
        String chainId = "chain-" + id;
        JobChainingJobListener jcl = new JobChainingJobListener(chainId);
        for (int i = 0; i < jobs.size() - 1; i++) {
            jcl.addJobChainLink(jobs.get(i).getKey(), jobs.get(i + 1).getKey());
        }
        
        // Once complete the chain listener just hangs around. This last job will
        // remove it
        JobDetail terminator = newJob(ChainTerminator.class)
                .usingJobData(ChainTerminator.LISTENERID, chainId)
                .build();
        
        // Schedule for termination
        scheduler.addJob(terminator, true);
        jcl.addJobChainLink(jobs.get(jobs.size() - 1).getKey(), terminator.getKey());
        
        // Add chain listener to scheduler
        scheduler.getListenerManager().addJobListener(jcl, EverythingMatcher.allJobs());
        
        // Start the first job!
        scheduler.triggerJob(jobs.get(0).getKey());
    }
}