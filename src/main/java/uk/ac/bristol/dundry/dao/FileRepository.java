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
    private final String depositPath;
    private final String publishPath;
    
    /**
     * Create a file repository
     * deposit and publish path may include formatting characters
     * which will use the deposit's base value in calculating the path
     * @param depositPath
     * @param publishPath 
     */
    public FileRepository(String depositPath, String publishPath) {
        this.depositPath = depositPath;
        this.publishPath = publishPath; 
    }
    
    public Path create(String id, String base) throws IOException {
        Path target = Files.createDirectory(getPath(depositPath, base, id));
        return target;
    }
    
    public Path depositPathForId(String id, String base) {
        return getPath(depositPath, base, id);
    }
    
    public Path publishPathForId(String id, String base) {
        return getPath(publishPath, base, id);
    }
    
    private Path getPath(String pathFormat, String base, String id) {
        return Paths.get(String.format(pathFormat, base), id); 
   }
}
