/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import uk.ac.bristol.dundry.Util;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class FileRepository {
    
    // play it safe. radix of 36 is ideal
    static final int RADIX = Math.min(Character.MAX_RADIX, 36);
    
    private final Path root;
    
    public FileRepository(String base) {
        root = Paths.get(base);
    }
    
    public Path create(String id, Path source) throws IOException {
        Path target = Files.createDirectory(root.resolve(id));
        Util.copyDirectory(source, target);
        return target;
    }
}
