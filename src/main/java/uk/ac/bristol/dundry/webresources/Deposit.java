package uk.ac.bristol.dundry.webresources;

import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.bristol.dundry.dao.FileSystemSource;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.model.ListDepositDescriptions;

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
    
    @GET
    public Response list() {
        return Response.ok(new ListDepositDescriptions(repository.getIds())).build();
    }
    
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create(
            @FormParam("source") String source,
            @FormParam("title") String title,
            @FormParam("description") String description) throws IOException {
                
        log.info("Create deposit: {} title: {} desc: {}", 
                new String[]{ source, title, description });
        
        java.nio.file.Path fromDir = sourceFS.getPath(source);
        
        String id = repository.create(fromDir, title, description, "unknown");
        
        URI createdUri = URI.create(id);
        
        return Response.created(createdUri).build();
    }
    
    @POST
    @Consumes("application/json")
    public Response create(JSONObject source) throws IOException, JSONException {
        
        log.info("Create deposit: {}", source);
        
        JSONObject depositStub = source.getJSONObject("deposit");
        
        return create(depositStub.getString("source"),
                depositStub.getString("title"),
                depositStub.getString("description"));
    }
    
    @Path("{item}")
    @GET
    public Response retrieve(@PathParam("item") String item) {
        return Response.ok(repository.getMetadata(item)).build();
    }
    
    @Path("{item}")
    @PUT
    public Response update(@PathParam("item") String item, Resource data) {
        if (log.isDebugEnabled()) {
            log.debug("Update: {} with {}", item, data.getModel());
        }
        repository.updateMetadata(item, data);
        return Response.ok().build();
    }
    
    @Path("{item}")
    @DELETE
    public Response delete(@PathParam("item") String item) {
        log.info("DELETE: {}", item);
        return Response.ok().build();
    }
}
