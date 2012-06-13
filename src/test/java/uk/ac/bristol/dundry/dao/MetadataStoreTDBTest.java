/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.google.common.io.Files;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import java.io.File;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MetadataStoreTDBTest {
    private MetadataStoreTDB instance;
    private File tmpDir;
    
    @Before
    public void setUp() {
        tmpDir = Files.createTempDir();
        instance = new MetadataStoreTDB(tmpDir.getAbsolutePath());
    }
    
    @After
    public void tearDown() {
        instance.store.close();
        // Delete tmpDir
    }

    /**
     * Test of create method, of class MetadataStoreTDB.
     */
    @Test
    public void testCreate_String() {
    }

    /**
     * Test of create method, of class MetadataStoreTDB.
     */
    @Test
    public void testCreate_String_Model() {
    }

    /**
     * Test of replaceData method, of class MetadataStoreTDB.
     */
    @Test
    public void testReplaceData() {
        Model m = ModelFactory.createDefaultModel();
        m.add(RDF.first, RDF.value, "1st");
        instance.create("1", m);
        
        assertEquals(1, instance.getData("1").size());
        
        m = instance.getData("1");
        m.removeAll(RDF.first, null, null);
        
        assertEquals(0, m.size());
        
        instance.replaceData("1", m);
        
        assertEquals(0, instance.getData("1").size());
    }

    /**
     * Test of getData method, of class MetadataStoreTDB.
     */
    @Test
    public void testGetData() {
    }

    /**
     * Test of query method, of class MetadataStoreTDB.
     */
    @Test
    public void testQuery() {
    }
}
