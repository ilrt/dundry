package uk.ac.bristol.dundry.tasks;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task to copy from one directory to another. That's it.
 * 
 * @author pldms
 */
public class MoveTask implements Job {
    
    final static Logger log = LoggerFactory.getLogger(MoveTask.class);

    public final static String FROM = "move-task-from";
    public final static String TO = "move-task-to";
    
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        JobDataMap jobData = jec.getMergedJobDataMap();
        String from = jobData.getString(FROM);
        String to = jobData.getString(TO);
        try {
            log.debug("Move <{}> to <{}>", Paths.get(from), Paths.get(to));
            Files.move(Paths.get(from), Paths.get(to));
        } catch (IOException ex) {
            throw new JobExecutionException("Move failed", ex);
        }
    }
}
