/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Repository {
    
    // play it safe. radix of 36 is ideal
    static final int RADIX = Math.min(Character.MAX_RADIX, 36);
    
    private final FileRepository fileRepo;
    private final MetadataStore mdStore;
    
    public Repository(FileRepository fileRepo, MetadataStore mdStore) {
        this.fileRepo = fileRepo;
        this.mdStore = mdStore;
    }
    
    public String create(Path source) throws IOException {
        // Create a random id!
        UUID randId = UUID.randomUUID();
        String baseEncoded = 
                Long.toString(randId.getMostSignificantBits(), RADIX) +
                Long.toString(randId.getLeastSignificantBits(), RADIX);
        String id = baseEncoded.replace("-",""); // remove sign bits
        
        Path repoDir = fileRepo.create(id, source);
        mdStore.create(id);
        
        return id;
    }
}
