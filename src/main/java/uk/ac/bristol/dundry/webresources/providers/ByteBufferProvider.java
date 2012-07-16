package uk.ac.bristol.dundry.webresources.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */

@Component
@Provider
@Produces({MediaType.TEXT_PLAIN, MediaType.WILDCARD})
public class ByteBufferProvider implements MessageBodyWriter<ByteBuffer> {

    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return ByteBuffer.class.isAssignableFrom(type);
    }

    @Override
    public long getSize(ByteBuffer t, Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return t.limit();
    }

    @Override
    public void writeTo(ByteBuffer t, Class<?> type, Type type1, Annotation[] antns, MediaType mt, MultivaluedMap<String, Object> mm, OutputStream out) throws IOException, WebApplicationException {
        // Is this ok? Too heavyweight?
        WritableByteChannel channel = Channels.newChannel(out);

        channel.write(t);
    }
    
    
    
}
