package uk.ac.bristol.dundry.dao;

import java.util.List;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

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
    
    public List<JobExecutionContext> listAllTasks() throws SchedulerException {
        return scheduler.getCurrentlyExecutingJobs();
    }
    
    public void startJob(String id) throws SchedulerException {
        JobDetail job = newJob(HelloJob.class)
        .withIdentity("job-" + id, "my-group")
        .usingJobData("ID", id)
        .build();
        
        Trigger trigger = newTrigger()
        .withIdentity("trigger-" + id, "my-group")
        .startNow()           
        .build();
        
        scheduler.scheduleJob(job, trigger);
    }
    
    public static class HelloJob implements Job {

        @Override
        public void execute(JobExecutionContext jec) throws JobExecutionException {
            String name = (String) jec.getMergedJobDataMap().getString("ID");
            try {
                System.err.printf("Execute %s (%s)\n", 1, name);
                Thread.sleep(20000);
                System.err.printf("Execute %s (%s)\n", 2, name);
                Thread.sleep(60000);
                System.err.printf("Execute %s (%s)\n", 3, name);
            } catch (InterruptedException ex) {
                throw new JobExecutionException("Error sleeping" , ex);
            }
        }
        
    }
}
