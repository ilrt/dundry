/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.*;
import org.springframework.stereotype.Component;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
public class FileSystemLister {
    
    public void getTreeAt(String base) {
        try (DirectoryStream<Path> ds = 
            Files.newDirectoryStream(FileSystems.getDefault().getPath(base))) 
        {
            for (Path p : ds) {
                System.out.println(p.getFileName());
                Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
}
