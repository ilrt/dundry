/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.FileSystemLister;
import uk.ac.bristol.dundry.model.Tree;

/**
 * REST Web Service
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("generic")
public class GenericResource {
    
    Logger log = LoggerFactory.getLogger(GenericResource.class);
    
    @Context
    private UriInfo context;
    
    @Autowired
    private FileSystemLister fsLister;
    
    /**
     * Creates a new instance of GenericResource
     */
    public GenericResource() {
    }

    /**
     * Retrieves representation of an instance of uk.ac.bristol.dundry.GenericResource
     * @return an instance of java.lang.String
     */
    @GET
    @Produces("text/plain")
    public String getXml() {
        return "hello";
    }
    
    @Path("/list/{dir}")
    @GET
    @Produces("text/plain")
    public String listDir(@PathParam("dir") String path) {
        log.warn("Path is: {}, fsLister is: {}", path, fsLister);
        return fsLister.getTreeAt(path).toString();
    }
    
    @Path("/listj/{dir}")
    @GET
    @Produces("application/json")
    public Tree<String> listDirJ(@PathParam("dir") String path) {
        log.warn("Path is: {}, fsLister is: {}", path, fsLister);
        return fsLister.getTreeAt(path);
    }
}
