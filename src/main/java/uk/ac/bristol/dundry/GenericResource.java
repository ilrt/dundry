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

    @Context
    private UriInfo context;
    
    @Inject
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
    @Produces("application/json")
    public Tree<String> listDir(@PathParam("dir") String path) {
        return fsLister.getTreeAt(path);
    }
}
