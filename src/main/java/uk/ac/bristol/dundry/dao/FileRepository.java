/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
    
    private final Path root;
    
    public FileRepository(String base) {
        root = Paths.get(base);
    }
    
    public Path create(String id) throws IOException {
        Path target = Files.createDirectory(root.resolve(id));
        return target;
    }
    
    public Path pathForId(String id) {
        return root.resolve(id);
    }
}
