package uk.ac.bristol.dundry.webresources.providers;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.*;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;

/**
 *
 * A Configurable provider that provides a very simple-minded RDF <-> JSON
 * object mapping. Of limited use.
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Provider
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class RdfResourceMappingProvider 
    implements MessageBodyWriter<Resource>, MessageBodyReader<Resource> {
    
    final static Logger log = LoggerFactory.getLogger(RdfResourceMappingProvider.class);
    
    final static XMLOutputFactory OUT_FAC = XMLOutputFactory.newInstance();
    final static XMLInputFactory IN_FAC = XMLInputFactory.newInstance();
    
    final BiMap<String, Property> keyToProperty;
    final BiMap<Property, String> propertyToKey;
    
    /**
     * Create a provider
     * 
     * @param map key to property map
     */
    public RdfResourceMappingProvider(Map<String, String> keyToProp) {
        
        Map<String, String> keyToPropDefaults = new HashMap<>();
        
        // Default label mapping
        keyToPropDefaults.put("label", RDFS.label.getURI());
        
        // Add given mappings in
        keyToPropDefaults.putAll(keyToProp);
        
        // Map string value to a property
        Map<String, Property> k2p = Maps.transformValues(keyToPropDefaults,
                 new Function<String, Property>() {

            @Override
            public Property apply(String f) {
                return ResourceFactory.createProperty(f);
            }
                    
                });
        
        // Create a bimap from that map (so we can look up either way)
        keyToProperty = ImmutableBiMap.copyOf(k2p);
        propertyToKey = keyToProperty.inverse();
    }
    
    /**
     * Write a resource and properties to a simple XML / JSON format,
     * mapping properties to keys as it goes.
     * Unmappable properties are lost.
     * @param resource Resource to write
     * @param writer The XML / JSON writer
     * @throws XMLStreamException 
     */
    protected void map(Resource resource, XMLStreamWriter writer) throws XMLStreamException {
        // If present, write the id of the resource as id
        if (resource.isURIResource()) {
            writer.writeStartElement("id");
            // TODO: Not convinced toExternalId should be here. We ought to be passing
            // around relative uris. Hmm. Hmm.
            writer.writeCharacters(Repository.toExternalId(resource.getURI()));
            writer.writeEndElement();
        }
        // If present, write the label of this resource
        if (resource.hasProperty(RDFS.label)) {
            writer.writeStartElement("label");
            writer.writeCharacters(resource.getProperty(RDFS.label).getLiteral().getLexicalForm());
            writer.writeEndElement();
        }
        // Now loop through each statement, mapping as we go...
        Iterator<Statement> si = resource.listProperties();
        while (si.hasNext()) {
            Statement s = si.next();
            // If we have a mapping for this property create and entry for key
            if (propertyToKey.containsKey(s.getPredicate())) {
                writer.writeStartElement(propertyToKey.get(s.getPredicate()));
                // If the value is a resource recurse
                if (s.getObject().isResource()) {
                    map(s.getObject().asResource(), writer);
                }
                // Otherwise write the value as a string (limited :-()
                else {
                    writer.writeCharacters(s.getObject().asLiteral().getLexicalForm());
                }
                writer.writeEndElement();
            }
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return Resource.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(Resource t, Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return -1;
    }

    @Override
    public void writeTo(Resource t, Class<?> type, Type type1, Annotation[] antns,
        MediaType mt, MultivaluedMap<String, Object> mm, OutputStream out) 
            throws IOException, WebApplicationException {
        try {
            XMLStreamWriter streamWriter = getWriterFor(mt, out);
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement("item");
            map(t, streamWriter);
            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
            streamWriter.flush();
            streamWriter.close();
        } catch (XMLStreamException ex) {
            throw new WebApplicationException(ex, Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Return an 'XML' writer suitable for the mimetype. May return a fake
     * XML writer ;-)
     * 
     * @param mimeType
     * @param out
     * @return
     * @throws XMLStreamException 
     */
    protected XMLStreamWriter getWriterFor(MediaType mimeType, OutputStream out) 
            throws XMLStreamException, UnsupportedEncodingException {
        switch (mimeType.toString()) {
            case "application/json":
                Writer outW = new OutputStreamWriter(out, "UTF-8");
                MappedNamespaceConvention con = new MappedNamespaceConvention();
                return new MappedXMLStreamWriter(con, outW);
            default:
                return OUT_FAC.createXMLStreamWriter(out);
        }
    }
    
    protected XMLStreamReader getReaderFor(MediaType mimeType, InputStream in) 
            throws XMLStreamException, JSONException, IOException {
        switch (mimeType.toString()) {
            case "application/json":
                // This seems too complex. IS -> String -> JSONObject -> XSR
                String jsonIn = CharStreams.toString(new InputStreamReader( in, "UTF-8" ) );
                JSONObject o = new JSONObject(jsonIn);
                MappedNamespaceConvention con = new MappedNamespaceConvention();
                return new MappedXMLStreamReader(o, con);
            default:
                return IN_FAC.createXMLStreamReader(in);
        }
    }
    
    @Override
    public boolean isReadable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return Resource.class.isAssignableFrom(type);
    }

    @Override
    public Resource readFrom(Class<Resource> type, Type type1, Annotation[] antns, 
        MediaType mt, MultivaluedMap<String, String> mm, InputStream in)
            throws IOException, WebApplicationException {
        try {
            XMLStreamReader streamReader = getReaderFor(mt, in);
            return parse(streamReader);
        } catch (XMLStreamException | JSONException ex) {
            throw new WebApplicationException(ex, Status.INTERNAL_SERVER_ERROR);
        }
    }

    private Resource parse(XMLStreamReader reader) throws XMLStreamException {
        
        Model m = ModelFactory.createDefaultModel();
        
        // Move to the root object
        reader.nextTag();
        
        return (Resource) parseRDFNode(reader, m);
    }

    private RDFNode parseRDFNode(XMLStreamReader reader, Model m) throws XMLStreamException {
        String id = null;
        StringBuilder content = null;
        List<Attribute> properties = null;
        int level = 0;
        while (reader.hasNext()) {
            int et = reader.next();
            if (XMLStreamReader.CHARACTERS == et) {
                if (properties != null)
                    log.warn("Characters found unexpectedly. Mixed content. Continuing...");
                if (content == null) content = new StringBuilder();
                content.append(reader.getText());
                //System.err.println("Chars: " + reader.getText());
            }
            else if (XMLStreamReader.START_ELEMENT == et) {
                if (content != null)
                    log.warn("Tag found unexpectedly. Mixed content. Continuing...");
                if ("id".equals(reader.getLocalName())) {
                    // id is special
                    id = parseRDFNode(reader, m).asLiteral().getLexicalForm();
                }
                else {
                    String name = reader.getLocalName();
                    if (!keyToProperty.containsKey(name)) {
                        // SKIP...
                        level++;
                    } else {
                        if (properties == null) properties = new LinkedList<>();
                        
                        //System.err.println("START: " + reader.getLocalName());
                        Attribute a = 
                                new Attribute(
                                    keyToProperty.get(name),
                                    parseRDFNode(reader, m));
                        //System.err.printf("Add property: <%s> <%s>\n", a.predicate, a.value);
                        properties.add(a);
                    }
                }
            }
            else if (XMLStreamReader.END_ELEMENT == et) {
                //System.err.println("END: " + level + " " + reader.getLocalName());
                level--;
                if (level < 0) break;
            }
        }
        
        //if (content != null && properties != null) {
            //System.err.println("ARGH: mixed " + id);
        //}
        
        if (content != null) {
            //System.err.println("return literal " + content);
            return m.createLiteral(content.toString());
        }
        
        Resource r = (id == null) ? 
                m.createResource() : 
                m.createResource(Repository.toInternalId(id));
        
        if (properties != null) {
            for (Attribute a: properties) {
                r.addProperty(a.predicate, a.value);
            }
        }
        
        return r;
    }
    
    static class Attribute {
        final Property predicate;
        final RDFNode value;
        
        public Attribute(Property predicate, RDFNode value) {
            this.predicate = predicate;
            this.value = value;
        }
    }
}
