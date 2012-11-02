package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermission.*;
import java.util.EnumSet;
import java.util.Set;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;

/**
 * Ensure that root is publicly readable
 *
 * @author pldms
 */
public class EnsurePublicPermissions extends JobBase {
    
    final static Logger log = LoggerFactory.getLogger(EnsurePublicPermissions.class);
    
    final static Set<PosixFilePermission> FILE_PERM = EnumSet.of(OTHERS_READ);
    final static Set<PosixFilePermission> DIR_PERM = EnumSet.of(OTHERS_READ, OTHERS_EXECUTE);

    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        try {
            log.info("Setting public permissions for <{}>", root);
            setPermissions(root, FILE_PERM, DIR_PERM);
        } catch (IOException ex) {
            throw new JobExecutionException("Problem setting public permissions", ex);
        }
    }

    public Path setPermissions(final Path root,
            final Set<PosixFilePermission> filePermissions,
            final Set<PosixFilePermission> dirPermissions) throws IOException {
        return Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                ensurePermissions(file, filePermissions);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                ensurePermissions(dir, dirPermissions);
                return FileVisitResult.CONTINUE;
            }
            
            // Ensure fileMermissions are set for file -- leave rest alone
            private void ensurePermissions(Path file, Set<PosixFilePermission> filePermissions) throws IOException {
                Set<PosixFilePermission> current = Files.getPosixFilePermissions(file);
                EnumSet<PosixFilePermission> newPerms = EnumSet.copyOf(current);
                newPerms.addAll(filePermissions);
                Files.setPosixFilePermissions(file, newPerms);
            }
        });
    }
    
    public static void main(String... args) throws JobExecutionException {
        EnsurePublicPermissions instance = new EnsurePublicPermissions();
        instance.execute(null, null, null, null, Paths.get("/tmp/check"), null);
    }
}
