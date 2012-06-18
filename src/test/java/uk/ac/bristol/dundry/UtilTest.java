/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import java.nio.file.Paths;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class UtilTest {
    
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    
    /**
     * Test of resolve method, of class Util.
     */
    @Test
    public void testResolve() {
        Resource res = r("http://example.com/foo/bar#baz");
        // Absolute
        assertEquals(r("http://example.com/x/y"), Util.resolve(res, Paths.get("/x/y")));
        // Relative
        assertEquals(r("http://example.com/foo/x/y"), Util.resolve(res, Paths.get("x/y")));
        // Contains a bad character
        assertEquals(r("http://example.com/foo/x/a%20space/y"), Util.resolve(res, Paths.get("x/a space/y")));
        // Treats repo:123abc as repo:123abc/
        res = r("repo:1234abc");
        assertEquals(r("repo:1234abc/foo/x/y"), Util.resolve(res, Paths.get("foo/x/y")));
        // Hurls on bnode
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot resolve against a blank node");
        Util.resolve(r(null), Paths.get("x/y")); 
    }
    
    private Resource r(String uri) {
        Model m = ModelFactory.createDefaultModel();
        if (uri == null) return m.createResource();
        else return m.createResource(uri);
    }
}
