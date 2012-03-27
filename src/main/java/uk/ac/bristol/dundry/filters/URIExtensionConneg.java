package uk.ac.bristol.dundry.filters;

import com.sun.jersey.api.container.filter.UriConnegFilter;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import uk.ac.bristol.dundry.webresources.providers.RdfMediaType;

/**
 * Configure some common extension - mediatype mappings, which allows
 * /foo.json to call /foo with type json. Makes browser debugging easier.
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class URIExtensionConneg extends UriConnegFilter {
    
    final static Map<String, MediaType> extToType;
    
    static {
        extToType = new HashMap<>();
        extToType.put("xml", MediaType.APPLICATION_XML_TYPE);
        extToType.put("json", MediaType.APPLICATION_JSON_TYPE);
        extToType.put("rdf", RdfMediaType.APPLICATION_RDF_XML_TYPE);
        extToType.put("ttl", RdfMediaType.TEXT_TURTLE_TYPE);
        extToType.put("nt", MediaType.valueOf("text/n-triples"));
        extToType.put("n3", RdfMediaType.TEXT_RDF_N3_TYPE);
    }
    
    public URIExtensionConneg() {
        super(extToType);
    }
    
}
