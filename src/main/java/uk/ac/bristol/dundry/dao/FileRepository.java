package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import static java.nio.file.attribute.PosixFilePermissions.*;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class FileRepository {

    final static Logger log = LoggerFactory.getLogger(FileRepository.class);
    // If we have to create the publish and deposit dirs, these are the permissions
    final static FileAttribute<Set<PosixFilePermission>> PUBLISH_ATTRS = asFileAttribute(fromString("rwxrwxr-x"));
    final static FileAttribute<Set<PosixFilePermission>> DEPOSIT_ATTRS = asFileAttribute(fromString("rwxrwx---"));
    private final String depositPath;
    private final String publishPath;

    /**
     * Create a file repository deposit and publish path may include formatting
     * characters which will use the deposit's base value in calculating the
     * path
     *
     * @param depositPath
     * @param publishPath
     */
    public FileRepository(String depositPath, String publishPath) {
        this.depositPath = depositPath;
        this.publishPath = publishPath;
    }

    public Path create(String id, String base) throws IOException {
        Path target = Files.createDirectory(getPath(depositPath, base, id, DEPOSIT_ATTRS));
        return target;
    }

    public Path depositPathForId(String id, String base) {
        return getPath(depositPath, base, id, DEPOSIT_ATTRS);
    }

    public Path publishPathForId(String id, String base) {
        return getPath(publishPath, base, id, PUBLISH_ATTRS);
    }

    private Path getPath(String pathFormat, String base, String id,
            FileAttribute<Set<PosixFilePermission>> permissions) {
        // Ensure base dir exists -- this is siginificant when using formatting
        // strings in the path format. The dir may be something like
        // /{basedir}/repo/deposit
        Path baseDir = Paths.get(String.format(pathFormat, base));
        if (!Files.exists(baseDir)) {
            log.info("<{}> does not exist. Creating.", baseDir);
            try {
                Files.createDirectory(baseDir, permissions);
            } catch (IOException ex) {
                log.error("Issue creating <{}>, {}", baseDir, ex.getLocalizedMessage());
                throw new RuntimeException("Issue creating " + baseDir, ex);
            }
        }

        return baseDir.resolve(id);
    }
}
