/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.webresources.providers;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class RdfResourceMappingProviderTest {
    
    private RdfResourceMappingProvider get(String... confA) {
        Map<String, String> conf = new HashMap<>();
        
        for (int i = 0; i < confA.length; i+= 2) conf.put(confA[i], confA[i+1]);
        
        return new RdfResourceMappingProvider(conf);
    }
    
    private Resource makeResource(String uri) {
        Model m = ModelFactory.createDefaultModel();
        return m.createResource(uri);
    }
    
    /**
     * Test of map method, of class RdfResourceMappingProvider.
     */
    @Test
    public void testMap() throws Exception {
    }

    /**
     * Test of isWriteable method, of class RdfResourceMappingProvider.
     */
    @Test
    public void testIsWriteable() {
        RdfResourceMappingProvider i = get();
        assertTrue("Can write resources", i.isWriteable(Resource.class, null, null, null));
        assertTrue("Can write resources impls", i.isWriteable(ResourceImpl.class, null, null, null));
        assertFalse("Can write literals", i.isWriteable(Literal.class, null, null, null));
    }

    /**
     * Test of getSize method, of class RdfResourceMappingProvider.
     */
    @Test
    public void testGetSize() {
        RdfResourceMappingProvider i = get();
        assertEquals("Size is unknown", -1, i.getSize(null, null, null, null, null));
    }

    /**
     * Test of writeTo method, of class RdfResourceMappingProvider.
     */
    @Test
    public void testWriteTo() throws Exception {
        RdfResourceMappingProvider i = get();
        
        Resource r = makeResource("http://example.com/r");
        r.addLiteral(DCTerms.title, "the title");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        i.writeTo(r, null, null, null, MediaType.valueOf("application/json"), null, out);
        
        String result = new String(out.toByteArray(), "UTF-8");
        
        assertEquals("{\"item\":{\"id\":\"http:\\/\\/example.com\\/r\"}}", result);
        
        i = get("title", DCTerms.title.getURI());
        
        out = new ByteArrayOutputStream();
        
        i.writeTo(r, null, null, null, MediaType.valueOf("application/json"), null, out);
        
        result = new String(out.toByteArray(), "UTF-8");
        
        assertEquals("{\"item\":{\"id\":\"http:\\/\\/example.com\\/r\",\"title\":\"the title\"}}", result);
    }
    
    @Test
    public void testReadFrom() throws Exception {
        RdfResourceMappingProvider i = get(
                "contributor", "http://purl.org/dc/terms/contributor",
                "source", "http://purl.org/dc/terms/source",
                "title", "http://purl.org/dc/terms/title",
                "description", "http://purl.org/dc/terms/description"
                );
        InputStream in = this.getClass().getResourceAsStream("/ex1.json");
        Resource r = i.readFrom(Resource.class, null, null, MediaType.valueOf("application/json"), null, in);
        Model expected = FileManager.get().loadModel("ex1.ttl");
        
        r.getModel().write(System.err, "TTL");
        
        assertTrue("ex1 json correct", expected.isIsomorphicWith(r.getModel()));
        
        in = this.getClass().getResourceAsStream("/ex2.json");
        r = i.readFrom(Resource.class, null, null, MediaType.valueOf("application/json"), null, in);
        expected = FileManager.get().loadModel("ex2.ttl");
        assertTrue("ex2 (missing desc) json correct", expected.isIsomorphicWith(r.getModel()));
        
        in = this.getClass().getResourceAsStream("/ex3.json");
        r = i.readFrom(Resource.class, null, null, MediaType.valueOf("application/json"), null, in);
        expected = FileManager.get().loadModel("ex3.ttl");
        assertTrue("ex3 (empty id) json correct", expected.isIsomorphicWith(r.getModel()));
    }
}
