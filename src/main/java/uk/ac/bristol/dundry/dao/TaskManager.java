package uk.ac.bristol.dundry.dao;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
public class TaskManager {
    private final Scheduler scheduler;
    
    public TaskManager() throws SchedulerException {
        this.scheduler = StdSchedulerFactory.getDefaultScheduler();
    }
    
    public void listAllTasks() {
    }
}
