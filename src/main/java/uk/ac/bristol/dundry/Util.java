package uk.ac.bristol.dundry;

import com.hp.hpl.jena.iri.IRI;
import com.hp.hpl.jena.iri.IRIFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class Util {
    
    final static Logger log = LoggerFactory.getLogger(Util.class);
    
    final static IRIFactory IRIFACTORY = IRIFactory.jenaImplementation();
    
    final static Pattern REPO_URL_PATTERN = Pattern.compile("repo:[a-zA-Z0-9]+");
    
    /**
     * Resolve a path relative to a resource.
     * 
     * For example http://example.com/foo/ and a/b would return a resource ...foo/a/b
     * repo:123abc is treated specially, in that we add an '/' on the end.
     * @param a Labelled resource to resolve against
     * @param b Path to resolve
     * @return 
     */
    public static Resource resolve(Resource a, Path b) {
        // if a is blank, then complain!
        if (a.isAnon()) throw new IllegalArgumentException("Cannot resolve against a blank node");
                
        // Encode path elements carefully
        String encPath = b.toString().replaceAll(" ", "%20");
        
        String iri = a.getURI();
        
        // If we have a top level repo resource, add a '/' on the end
        if (REPO_URL_PATTERN.matcher(iri).matches()) iri = iri + "/";
        
        IRI resolved = IRIFACTORY.construct(iri).resolve(encPath);
        return a.getModel().createResource(resolved.toString());
    }
}
