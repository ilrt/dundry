package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;

/**
 * Tell the transmission torrent daemon to serve our torrent
 * 
 * @author pldms
 */
public class TransmissionAdvertise extends JobBase {
    
    final static org.slf4j.Logger log = LoggerFactory.getLogger(TransmissionAdvertise.class);
    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        // Get torrent location
        
        // get transmission username and password
        
        try {
            Process p = new ProcessBuilder(
                    "transmission-remote",
                    "-a", "foo", "--find", root.toAbsolutePath().toString(),
                    "-n", "transmission:databris").start();
        } catch (IOException ex) {
            
        }
    }
    
}
