package uk.ac.bristol.dundry.webresources.providers;

import com.google.common.base.Function;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Maps;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Scope;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

/**
 *
 * A Configurable provider that provides a very simple-minded RDF <-> JSON
 * object mapping. Of limited use.
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class RDFJsonMapProvider implements MessageBodyWriter<Resource> {
    
    final BiMap<String, Property> keyToProperty;
    final BiMap<Property, String> propertyToKey;
    
    /**
     * Create a provider
     * 
     * @param map key to property map
     */
    public RDFJsonMapProvider(Map<String, String> keyToProp) {
        // Map string value to a property
        Map<String, Property> k2p = Maps.transformValues(keyToProp,
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
    
    private void map(Resource resource, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("item");
        writer.writeStartElement("id");
        writer.writeCharacters(resource.getURI());
        writer.writeEndElement();
        /* TODO: label */
        Iterator<Statement> si = resource.listProperties();
        while (si.hasNext()) {
            Statement s = si.next();
            if (propertyToKey.containsKey(s.getPredicate())) {
                writer.writeStartElement(propertyToKey.get(s.getPredicate()));
                if (s.getObject().isResource()) {
                    map(s.getObject().asResource(), writer);
                }
                else {
                    writer.writeCharacters(s.getObject().asLiteral().getLexicalForm());
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return type.isAssignableFrom(Resource.class);
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
            Writer outW = new OutputStreamWriter(out, "UTF-8");
            MappedNamespaceConvention con = new MappedNamespaceConvention();
            XMLStreamWriter streamWriter = new MappedXMLStreamWriter(con, outW);
            streamWriter.writeStartDocument();
            map(t, streamWriter);
            streamWriter.writeEndDocument();
            streamWriter.flush();
            streamWriter.close();
            outW.close();
        } catch (XMLStreamException ex) {
            throw new WebApplicationException(ex, Status.INTERNAL_SERVER_ERROR);
        }
    }
    
    
}
