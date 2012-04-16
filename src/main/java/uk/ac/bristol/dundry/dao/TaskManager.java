package uk.ac.bristol.dundry.dao;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobChainingJobListener;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
public class TaskManager {
    
    static final Logger log = LoggerFactory.getLogger(TaskManager.class);
    
    private final Scheduler scheduler;
    private final ArrayList<Class<? extends Job>> defaultJobs;
    
    public TaskManager(List<String> jobsClasses) throws SchedulerException {
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
    
    public void startJob(String id) throws SchedulerException {
        /*JobDetail job = newJob(HelloJob.class)
        .withIdentity("job-" + id, "my-group")
        .usingJobData("ID", id)
        .build();
        
        Trigger trigger = newTrigger()
        .withIdentity("trigger-" + id, "my-group")
        .startNow()           
        .build();
        
        scheduler.scheduleJob(job, trigger);*/
        
        List<JobDetail> jobs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JobDetail job = newJob(HelloJob.class)
                    .withIdentity(id + "-" + i, "my-group")
                    .usingJobData("ID", id)
                    .build();
            jobs.add(job);
        }
        executeJobsInOrder(id, jobs);
    }
    
    public void startJobs(String id, List<Class<? extends Job>> jobs, Map<String, Object> context)
            throws SchedulerException {
        List<JobDetail> jobDetails = new ArrayList<>();
        
        // Create context for these defaultJobs
        JobDataMap jobData = new JobDataMap();
        jobData.putAll(context);
        
        // Make the job details
        int count = 0;
        for (Class<? extends Job> job: jobs) {
            JobDetail jobDetail = newJob(job)
                    .withIdentity(job.getName() + count++, id)
                    .usingJobData(jobData)
                    .build();
            
            jobDetails.add(jobDetail);
        }
        
        executeJobsInOrder(id, jobDetails);
    }
    
    public void executeJobsInOrder(String id, List<JobDetail> jobs) throws SchedulerException {
        if (jobs.isEmpty()) {
            return;
        }
        
        // Add all defaultJobs to scheduler
        for (JobDetail jd: jobs) scheduler.addJob(jd, true);
        
        // Make a chain of these defaultJobs 
        JobChainingJobListener jcl = new JobChainingJobListener("chain-" + id);
        for (int i = 0; i < jobs.size() - 1; i++) {
            jcl.addJobChainLink(jobs.get(i).getKey(), jobs.get(i + 1).getKey());
        }
        
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
    
    public static class HelloJob implements Job {

        @Override
        public void execute(JobExecutionContext jec) throws JobExecutionException {
            String name = jec.getMergedJobDataMap().getString("ID");
            int hc = System.identityHashCode(this);
            try {
                System.err.printf("[%s] Execute %s (%s)\n", hc, 1, name);
                Thread.sleep(20000);
                System.err.printf("[%s] Execute %s (%s)\n", hc, 2, name);
            } catch (InterruptedException ex) {
                throw new JobExecutionException("Error sleeping" , ex);
            }
        }
        
    }}

