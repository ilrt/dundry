package uk.ac.bristol.dundry.dao;

import java.util.ArrayList;
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

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
public class TaskManager {
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
        startJobsInOrder(id, jobs);
    }
    
    public void startJobs(String id, List<Class<? extends Job>> jobs, Map<String, Object> context)
            throws SchedulerException {
        List<JobDetail> jobDetails = new ArrayList<>();
        
        // Create context for these jobs
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
        
        startJobsInOrder(id, jobDetails);
    }
    
    public void startJobsInOrder(String id, List<JobDetail> jobs) throws SchedulerException {
        if (jobs.isEmpty()) {
            return;
        }
        
        // Add all jobs to scheduler
        for (JobDetail jd: jobs) scheduler.addJob(jd, true);
        
        // Make a chain of these jobs 
        JobChainingJobListener jcl = new JobChainingJobListener("chain-" + id);
        for (int i = 0; i < jobs.size() - 1; i++) {
            jcl.addJobChainLink(jobs.get(i).getKey(), jobs.get(i + 1).getKey());
        }
        
        // Add chain listener to scheduler
        scheduler.getListenerManager().addJobListener(jcl, EverythingMatcher.allJobs());
        
        // Start the first job!
        scheduler.triggerJob(jobs.get(0).getKey());
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

