package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import com.hp.hpl.jena.sparql.lib.DatasetLib;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.DCTerms;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openjena.riot.RiotLoader;
import static org.mockito.Mockito.*;
import org.openjena.riot.Lang;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class RepositoryTest {
    private final Repository instance;
    
    public RepositoryTest() throws IOException {
        InputStream in = 
                RepositoryTest.class.getClassLoader().getResourceAsStream("repository/ds.trig");
        DatasetGraph ds = DatasetLib.createDatasetGraphMem();
        RiotLoader.read(in, ds, Lang.TRIG, "http://example.com/ds");
        MetadataStore mStore = new MetadataStoreDS(DatasetImpl.wrap(ds));
        FileRepository fr = mock(FileRepository.class);
        when(fr.create(anyString(), any(Path.class))).thenReturn(Paths.get("bar"));
        instance = new Repository(fr, mStore);
        instance.taskManager = mock(TaskManager.class);
    }
    
    private Model model(int i) {
        return FileManager.get().loadModel("repository/" + i + ".ttl");
    }
    
    /**
     * Test of getIds method, of class Repository.
     */
    @Test
    public void testGetIds() {
    }

    /**
     * Test of create method, of class Repository.
     */ 
    @Test
    public void testCreate() throws Exception {
        Model m = ModelFactory.createDefaultModel();
        Resource foo = m.createResource("http://example.com/temp");
        String result = instance.create(Paths.get("foo"), "creator", foo);
        
        // Check we got a result
        assertNotNull(result);
        
        // Check start tasks was called
        verify(instance.taskManager).startTasks(anyString(), anyMapOf(String.class, Object.class), any(Class.class));
    }

    /**
     * Test of getMetadata method, of class Repository.
     */
    @Test
    public void testGetMetadata() {
        Resource md = instance.getMetadata("1");
        assertEquals("repo:1", md.getURI());
        assertTrue(model(1).isIsomorphicWith(md.getModel()));
    }
    
    @Test
    public void testGetMetadataIsMerged() {
        Resource md = instance.getMetadata("2");
        assertTrue(model(2).isIsomorphicWith(md.getModel()));
    }
    
    /**
     * Test of updateMetadata method, of class Repository.
     */
    @Test
    public void testUpdateMetadata() {
        Resource md = instance.getMetadata("2");
        md.addProperty(DCTerms.abstract_, "abs 2");
        instance.updateMetadata("2", md);
        
        md = instance.getMetadata("2");
        
        assertTrue(md.hasProperty(DCTerms.abstract_, "abs 2"));
    }
    
    @Test
    public void testUpdateMetadataProvImmutable() {
        Resource md = instance.getMetadata("2");
        
        md.removeAll(DCTerms.title); // should stick
        md.removeAll(DCTerms.description); // shouldn't
        
        instance.updateMetadata("2", md);
        
        md = instance.getMetadata("2");
        
        assertFalse(md.hasProperty(DCTerms.title));
        assertTrue(md.hasProperty(DCTerms.description));
    }
    
    /**
     * Test of toExternalId method, of class Repository.
     */
    @Test
    public void testToExternalId() {
    }

    /**
     * Test of toInternalId method, of class Repository.
     */
    @Test
    public void testToInternalId() {
    }
}
