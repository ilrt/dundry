/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.model.Tree;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class FileSystemLister {
    private final Path root;
            
    public FileSystemLister(String base) {
        root = Paths.get(base);
    }
    
    public Path getPath(String relative) {
        return root.resolve(relative);
    }
    
    public Tree<String> getTreeAt(String base) {
        return getTreeAt(root.resolve(base));
    }
    
    private Tree<String> getTreeAt(Path start) {
        String label = start.toString();
        
        if (!Files.isDirectory(start, LinkOption.NOFOLLOW_LINKS)) return new Tree(label);
        
        List<Tree<String>> subDirs = new ArrayList<>();
        try (DirectoryStream<Path> ds = 
            Files.newDirectoryStream(start)) 
        {
            for (Path p : ds) {
                subDirs.add(getTreeAt(p));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Tree(label, subDirs);
    } 
}
