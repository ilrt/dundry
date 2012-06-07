package uk.ac.bristol.dundry.webresources;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.net.URI;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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
    
    @GET
    public Response list() {
        return Response.ok(repository.getIds()).build();
    }
    
    /**
     * Convenient way to create a deposit via a form
     * @param source
     * @param title
     * @param description
     * @return
     * @throws IOException 
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response create(
            @FormParam("title") String title,
            @FormParam("description") String description) throws IOException, SchedulerException {
                
        log.info("Create title: {} desc: {}", title, description);
        
        Model model = ModelFactory.createDefaultModel();
        Resource record = model.createResource();
        record.addProperty(DCTerms.title, title);
        record.addProperty(DCTerms.description, description);
        
        return create(record);
    }
    
    @POST
    @Consumes("application/json")
    public Response create(Resource record) throws IOException, SchedulerException {
        
        log.info("Create deposit: {}", record.getModel());
                
        // TODO: username
        String id = repository.create("unknown", record);
        
        URI createdUri = URI.create(id);
        
        return Response.created(createdUri).build();
    }
    
    @Path("{item}")
    @GET
    public Response retrieve(@PathParam("item") String item) {
        // Does item exist?
        if (!repository.hasId(item)) return Response.status(Status.NOT_FOUND).build();
        
        return Response.ok(repository.getMetadata(item)).build();
    }
    
    @Path("{item}")
    @PUT
    public Response update(@PathParam("item") String item, Resource data) {
        if (log.isDebugEnabled()) {
            log.debug("Update: {} with {}", item, data.getModel());
        }
        
        // Does item exist?
        if (!repository.hasId(item)) return Response.status(Status.NOT_FOUND).build();
        
        repository.updateMetadata(item, data);
        return Response.ok().build();
    }
    
    @Path("{item}")
    @POST
    @Consumes("application/x-www-form-urlencoded")
    public Response addContent(@PathParam("item") String item, @FormParam("source") String source) throws SchedulerException {
        log.info("Add content from source {} to {}", source, item);
        
        // Check we have something to add to
        if (!repository.hasId(item)) return Response.status(Response.Status.NOT_FOUND).build();
        
        java.nio.file.Path repoDir = repository.getPathForId(item);
        JobDetail depositTask = sourceFS.depositItem(source, item, repoDir);
        
        repository.makeDeposit(depositTask, item, source);
        
        return Response.ok().build();
    }
    
    @Path("{item}")
    @DELETE
    public Response delete(@PathParam("item") String item) {
        log.info("DELETE: {}", item);
        
        // Does item exist?
        if (!repository.hasId(item)) return Response.status(Status.NOT_FOUND).build();
        
        return Response.ok().build();
    }
}
