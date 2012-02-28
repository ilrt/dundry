/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.webresources;

import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.FileSystemSource;
import uk.ac.bristol.dundry.model.Tree;

/**
 * REST Web Service
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("sources")
public class Source {
    
    Logger log = LoggerFactory.getLogger(Source.class);
    
    @Context
    private UriInfo context;
    
    @Autowired
    private FileSystemSource fsLister;
    
    /**
     * Creates a new instance of Source
     */
    public Source() {
    }
    
    @Path("/{dir: .+}")
    @GET
    public Response listDir(@PathParam("dir") String path) throws IOException {
        log.info("List source path: {}", path);
        
        if (path.contains("..")) // No reason to allow these, and dangerous
            return Response.status(Response.Status.BAD_REQUEST).
                    entity("We don't accept relative paths").build();
        Tree<String> listing = fsLister.getTreeAt(path);
        if (listing == FileSystemSource.NONE)
            return Response.status(Response.Status.NOT_FOUND).build(); 
        else
            return Response.ok(listing).build();
    }
}
