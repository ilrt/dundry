package uk.ac.bristol.dundry.dao;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.quartz.JobDetail;
import uk.ac.bristol.dundry.model.Tree;
import uk.ac.bristol.dundry.tasks.CopyTask;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobDataMap;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class FileSystemSource {
    
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
     * Deposit an item (
     * @param itemId
     * @param depositLocation
     * @return 
     */
    public JobDetail depositItem(String sourceItemId, String depositId, Path depositLocation) {
        Path sourcePath = getPath(sourceItemId);
        // Create context for these jobs
        JobDataMap jobData = new JobDataMap();
        jobData.putAll(ImmutableMap.of(CopyTask.FROM, sourcePath, CopyTask.TO, depositLocation)); 
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
        return getTreeAt(getPath(base));
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
                subDirs.add(getTreeAt(p));
            }
        }
        
        return new Tree(label, subDirs);
    } 
}
