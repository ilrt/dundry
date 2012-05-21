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
public class CopyTask implements Job {
    
    final static Logger log = LoggerFactory.getLogger(CopyTask.class);

    public final static String FROM = "copy-task-from";
    public final static String TO = "copy-task-to";
    
    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        JobDataMap jobData = jec.getMergedJobDataMap();
        Path from = (Path) jobData.get(FROM);
        Path to = (Path) jobData.get(FROM);
        try {
            copyDirectory(from, to);
        } catch (IOException ex) {
            throw new JobExecutionException("Copy failed", ex);
        }
    }
        
    /**
     * Recursively copy a directory into another directory.
     * Copying a to b will result in b/a...
     * @param from Source
     * @param to Destination
     * @return
     * @throws IOException 
     */
    protected Path copyDirectory(final Path from, final Path to) throws IOException {
        
        log.info("Copy {} to {}", from, to);
        
        // We relativise paths to the parent of from
        // So /ex/a/b becomes /ex/a and we copy b directory
        final Path parent = from.getParent();
        
        try {
            return Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
                                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path rel = parent.relativize(file);
                    log.trace("Visit {}", file);
                    Files.copy(file, to.resolve(rel), StandardCopyOption.COPY_ATTRIBUTES);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path rel = parent.relativize(dir);
                    log.trace("Visit dir {}", dir);
                    log.trace("Create dir {}", to.resolve(rel));
                    Files.createDirectory(to.resolve(rel));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            // Clean up?
            throw ex;
        }
    }
}
