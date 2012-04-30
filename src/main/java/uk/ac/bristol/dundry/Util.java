package uk.ac.bristol.dundry;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import javax.ws.rs.core.UriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Util {
    
    final static Logger log = LoggerFactory.getLogger(Util.class);
    
    final static IRIFactory IRIFACTORY = IRIFactory.iriImplementation();
    
    /**
     * Resolve a path relative to a resource.
     * 
     * For example http://example.com/foo/ and a/b would return a resource ...foo/a/b
     * @param a Labelled resource to resolve against
     * @param b Path to resolve
     * @return 
     */
    public static Resource resolve(Resource a, Path b) {
        // if a is blank, then complain!
        if (a.isAnon()) throw new IllegalArgumentException("Cannot resolve against a blank node");
        
        // Encode path elements carefully
        String encPath = b.toString().replaceAll(" ", "%20");
        
        IRI resolved = IRIFACTORY.construct(a.getURI()).resolve(encPath);
        return a.getModel().createResource(resolved.toString());
    }
}
