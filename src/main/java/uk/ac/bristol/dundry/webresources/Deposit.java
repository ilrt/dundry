package uk.ac.bristol.dundry.webresources;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.Util;
import uk.ac.bristol.dundry.dao.FileRepository;
import uk.ac.bristol.dundry.dao.FileSystemSource;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("deposits")
public class Deposit {
    
    final static Logger log = LoggerFactory.getLogger(Deposit.class);
    
    @Autowired Repository repository;
    @Autowired FileSystemSource sourceFS;
    
    @Path("/")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create(@FormParam("source") String source) throws IOException {
        
        log.info("Create deposit: {}", source);
        
        java.nio.file.Path fromDir = sourceFS.getPath(source);
        
        String id = repository.create(fromDir);
        
        URI createdUri = URI.create(id);
        
        return Response.created(createdUri).entity("Created and copied").build();
    }
    
    @Path("/")
    @POST
    @Consumes("application/json")
    public Response create(JSONObject source) throws IOException {
        
        log.info("Create deposit: {}", source);
        
        /*java.nio.file.Path fromDir = sourceFS.getPath(source);
        
        String id = repository.create(fromDir);*/
        
        URI createdUri = URI.create("1234");
        
        return Response.created(createdUri).build();
    }
    
    @Path("/{item}")
    @GET
    public Response retrieve(@PathParam("item") String item) {
        return Response.ok(repository.getMetadata(item).getModel()).build();
    }
    
}
