package uk.ac.bristol.dundry.webresources;

import javax.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("deposits")
public class Tasks {
    final static Logger log = LoggerFactory.getLogger(Tasks.class);
}
