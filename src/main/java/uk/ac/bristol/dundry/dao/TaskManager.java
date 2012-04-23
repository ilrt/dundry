package uk.ac.bristol.dundry.dao;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.quartz.JobBuilder.newJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobChainingJobListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.tasks.ChainTerminator;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class TaskManager {
    
    static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    
    private final Scheduler scheduler;
    private final ArrayList<Class<? extends Job>> defaultJobs;
    
    public TaskManager(List<String> jobsClasses )throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        this.scheduler.start();
        
        // Load up job classes
        defaultJobs = new ArrayList<>();
        for (String jobClassName: jobsClasses) {
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
    
    /**
     * Begin a series of tasks. If provided it will start with the provided tasks,
     * then run the standard tasks configured at startup.
     * @param id The deposit id associated with this task
     * @param context An execution context for the tasks
     * @param jobs Jobs to run before default jobs.
     */
    public void startTasks(String id, Map<String, ? extends Object> context, 
            Class<? extends Job>... jobs) throws SchedulerException {
        
        List<JobDetail> jobDetails = new ArrayList<>();
        
        // Create context for these jobs
        JobDataMap jobData = new JobDataMap();
        jobData.putAll(context);
        
        // Iterate through provided jobs, then defaults
        for (Class<? extends Job> job: Iterables.concat(Arrays.asList(jobs), defaultJobs)) {
            JobDetail jobDetail = newJob(job)
                    .withIdentity(job.getName(), id)
                    .usingJobData(jobData)
                    .build();
            
            jobDetails.add(jobDetail);
        }
        
        executeJobsInOrder(id, jobDetails);
    }
}