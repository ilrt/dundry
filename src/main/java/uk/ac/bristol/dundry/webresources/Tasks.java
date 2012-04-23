package uk.ac.bristol.dundry.webresources;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.TaskManager;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("tasks")
public class Tasks {
    final static Logger log = LoggerFactory.getLogger(Tasks.class);
    
    @Autowired TaskManager taskManager;
    private int idNo = 0;
    
    @GET
    public Response list() throws SchedulerException {
        StringBuilder r = new StringBuilder("Tasks:\n\n");
        for (Object i: taskManager.listAllTasks()) {
            r.append(i);
            r.append("\n");
        }
        return Response.ok(r.toString()).build();
    }
}
