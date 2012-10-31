package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermissions.*;
import java.util.Set;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;

/**
 * Sym link root under a location 
 * @author pldms
 */
public class SymLinkTask extends JobBase {
    
    final static Logger log = LoggerFactory.getLogger(SymLinkTask.class);
    
    // Liberal permission for link
    //final static FileAttribute<Set<PosixFilePermission>> PERMISSION = asFileAttribute(fromString("rwxrwxr-x"));
    
    public final static String LINK_BASE = "symlink.base";
    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        String base = jobData.getString(LINK_BASE);
        // Link has same name as root, but under link base
        Path link = Paths.get(base).resolve(root.getFileName());
        Path target = root.toAbsolutePath(); // play it safe here
        log.debug("Link <{}> to <{}>", target, link);
        try {
            Files.createSymbolicLink(link, target); //, PERMISSION);
        } catch (IOException ex) {
            log.error("Issue linking " + target + " to " + link, ex);
        }
    }
    
}
