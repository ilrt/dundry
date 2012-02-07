/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.springframework.stereotype.Component;

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
    
    public Path create() throws IOException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String dirName = baseEncoded.replace("-",""); // remove sign bits
        
        return Files.createDirectory(root.resolve(dirName));
    }
    
    
}
