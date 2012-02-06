package uk.ac.bristol.dundry.webresources;

import java.io.IOException;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.FileRepository;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Component
@Path("deposits")
public class Deposit {
    
    @Autowired FileRepository repository;
    
    @Path("/create")
    @POST
    public Response create(@QueryParam("source") String source) throws IOException {
        
        java.nio.file.Path dir = repository.create();
        
        
        URI createdUri;
        
        return Response.created(createdUri).entity("Created and copied").build();;
    }
    
    @Path("/items/{item}")
    @GET
    public Response retrieve(@PathParam("item") String item) {
        return null;
    }
    
    
    
}
