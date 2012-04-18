package uk.ac.bristol.dundry.tasks;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clean up a job chain by removing it from the scheduler
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class ChainTerminator implements Job {
    
    final static Logger log = LoggerFactory.getLogger(ChainTerminator.class);
    
    public final static String LISTENERID = "chainterminator.listenerid";
    
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        String id = jec.getMergedJobDataMap().getString(LISTENERID);
        try {
            jec.getScheduler().getListenerManager().removeJobListener(id);
        } catch (SchedulerException ex) {
            log.error("Problem removing job chain listener {} from listener manager: {}", id, ex);
        }
    }
    
}
