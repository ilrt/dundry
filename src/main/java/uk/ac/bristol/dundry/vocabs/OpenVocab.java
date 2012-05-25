package uk.ac.bristol.dundry.vocabs; 
import com.hp.hpl.jena.rdf.model.*;

public class OpenVocab {
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://open.vocab.org/terms/";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    // TODO: register this at open vocab when I can log in
    /** <p>Indicates the state of a resource in a workflow (e.g. in progress, published)</p> */
    public static final Property state = m_model.createProperty( "http://open.vocab.org/terms/state" );  
}
