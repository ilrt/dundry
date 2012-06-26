package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class FileRepository {
    
    private final Path depositRoot;
    private final Path publishRoot;
    
    public FileRepository(String depositBase, String publishBase) {
        depositRoot = Paths.get(depositBase);
        publishRoot = Paths.get(publishBase);
    }
    
    public Path create(String id) throws IOException {
        Path target = Files.createDirectory(depositRoot.resolve(id));
        return target;
    }
    
    public Path pathForId(String id) {
        return depositRoot.resolve(id);
    }
}
