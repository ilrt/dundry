package uk.ac.bristol.dundry.dao;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.model.Tree;
import uk.ac.bristol.dundry.tasks.CopyTask;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class FileSystemSource {
    
    final static Logger log = LoggerFactory.getLogger(FileSystemSource.class);
    
    public final static Tree<String> NONE = new Tree<>();
    
    private final Path root;
    
    public FileSystemSource(String base) {
        root = Paths.get(base);
    }
    
    /**
     * Resolve path relative to the root of this source
     * It considers '/' to be the root
     * @param relative
     * @return 
     */
    public Path getPath(String relative) {
        // Remove leading '/'
        String relativised = relative.replaceFirst("^/+", "");
        
        Path relPath = root.resolve(relativised).normalize();
        
        // Is the path now below root?
        if (!relPath.startsWith(root)) {
            throw new RuntimeException("Path " + relative + " not under root");
        }
        return relPath;
    }
    
    /**
     * Deposit item(s) in deposit location
     * @param sourceItemIds sources 
     * @param repository deposit id
     * @param depositLocation target
     * @return Job that will copy items across
     */
    public JobDetail depositItem(List<String> sourceItemIds, String depositId, Path depositLocation) {
        // Encode source paths for job, separated by '\n'
        StringBuilder sourcePaths = new StringBuilder();
        boolean first = true;
        for (String sourceItemId: sourceItemIds) {
            Path sourcePath = getPath(sourceItemId).toAbsolutePath();
            if (!first) sourcePaths.append("\n");
            else first = false;
            sourcePaths.append(sourcePath.toString());
        }
        
        // Create context for these jobs
        JobDataMap jobData = new JobDataMap();
        jobData.putAll(
                ImmutableMap.of(
                    CopyTask.FROM, sourcePaths.toString(),
                    CopyTask.TO, depositLocation.toAbsolutePath().toString())); 
        return newJob(CopyTask.class)
                    .withIdentity(CopyTask.class.getName(), depositId)
                    .usingJobData(jobData)
                    .build();
    }
    
    /**
     * Get a path relative to the root of this source
     * @param path
     * @return 
     */
    public Path getRelativePath(Path path) {
        return root.relativize(path);
    }
    
    /**
     * Get a file tree rooted at root/base
     * @param base
     * @return
     * @throws IOException 
     */
    public Tree<String> getTreeAt(String base) throws IOException {
        Path path = getPath(base);
        log.debug("Get path contents for <{}> ({})", base, path);
        return getTreeAt(path);
    }
    
    /**
     * Get a file tree rooted at root/base
     * @param start
     * @return
     * @throws IOException 
     */
    private Tree<String> getTreeAt(Path start) throws IOException {
        // This will happen if you try to get a non-existent root
        // (unless the directory listing goes stale?)
        if (!Files.exists(start)) return NONE;
        
        String label = getRelativePath(start).toString();
        
        if (!Files.isDirectory(start, LinkOption.NOFOLLOW_LINKS)) return new Tree(label);
        
        List<Tree<String>> subDirs = new ArrayList<>();
        try (DirectoryStream<Path> ds = 
            Files.newDirectoryStream(start)) 
        {
            for (Path p : ds) {
                // Check here in case we have a sym link to nothingness
                if (Files.exists(p)) subDirs.add(getTreeAt(p));
            }
        }
        
        return new Tree(label, subDirs);
    }
    
    public static void main(String... args) throws IOException {
        FileSystemSource instance = new FileSystemSource("test-fs");
        Path p = instance.getPath("/RDSF_MV/nfs3-exports/marfc-cregan-2011/ACRC_Test_Area/export/Data-Bris");
        System.out.println(p);
        Tree<String> t = instance.getTreeAt("/RDSF_MV/nfs3-exports/marfc-cregan-2011/ACRC_Test_Area/export/Data-Bris");
        System.out.println(t);
    }
}
