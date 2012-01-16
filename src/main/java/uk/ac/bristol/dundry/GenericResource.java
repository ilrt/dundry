/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.springframework.stereotype.Component;

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
}
