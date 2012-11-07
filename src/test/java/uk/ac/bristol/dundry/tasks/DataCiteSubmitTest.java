package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
//import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

/**
 *
 * @author pldms
 */
public class DataCiteSubmitTest {
    
    private static Schema schema;
    private static XMLOutputFactory xof;
    
    @BeforeClass
    public static void setUpXML() throws SAXException, MalformedURLException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // use local version to avoid network
        schema = factory.newSchema(DataCiteSubmitTest.class.getClassLoader().getResource("datacite/metadata.xsd"));
        //schema = factory.newSchema(new URL("http://schema.datacite.org/meta/kernel-2.2/metadata.xsd"));
        xof = XMLOutputFactory.newInstance();
    }

    @Test
    public void testToDataCite() throws Exception {
        String doi = "10.999/test";
        DataCiteSubmit instance = new DataCiteSubmit();
        
        Model data = FileManager.get().loadModel("datacite/data.rdf");
        
        StringWriter out = new StringWriter();
        XMLStreamWriter writer = xof.createXMLStreamWriter(out);
                //new IndentingXMLStreamWriter(xof.createXMLStreamWriter(out));
        
        instance.toDataCite(data.getResource("http://example.com/res"), doi, writer);
                
        // Validate
        Validator validator = schema.newValidator();
        try {
            validator.validate(new StreamSource(new StringReader(out.toString())));
        } catch (Exception e) {
            System.err.println(out.toString());
            throw e;
        }
        
        assertTrue("validated", true);
    }
}
