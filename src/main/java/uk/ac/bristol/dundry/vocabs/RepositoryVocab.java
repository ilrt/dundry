package uk.ac.bristol.dundry.vocabs; 
import com.hp.hpl.jena.rdf.model.*;

public class RepositoryVocab {
    /** <p>The RDF model that holds the vocabulary terms</p> */
    private static Model m_model = ModelFactory.createDefaultModel();
    
    /** <p>The namespace of the vocabulary as a string</p> */
    public static final String NS = "http://vocab.bris.ac.uk/data/repository";
    
    /** <p>The namespace of the vocabulary as a string</p>
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( NS );
    
    /** <p>Indicates the state of a resource in a workflow (e.g. in progress, published)</p> */
    public static final Property state = m_model.createProperty( "http://vocab.bris.ac.uk/data/repository#state" );
    
    /** <p>The project associated with a deposit</p> */
    public static final Property project = m_model.createProperty( "http://vocab.bris.ac.uk/data/repository#project" );
    
    /** <p>The person who submitted this deposit</p> */
    public static final Property depositor = m_model.createProperty( "http://vocab.bris.ac.uk/data/repository#depositor" );
}
