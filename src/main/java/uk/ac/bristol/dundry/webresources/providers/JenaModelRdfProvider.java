/*
 * Copyright (c) 2008, University of Bristol
 * Copyright (c) 2008, University of Manchester
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1) Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3) Neither the names of the University of Bristol and the
 *    University of Manchester nor the names of their
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package uk.ac.bristol.dundry.webresources.providers;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

/**
 * @author Mike Jones (mike.a.jones@bristol.ac.uk)
 * @version $Id: JenaModelRdfProvider.java 177 2008-05-30 13:50:59Z mike.a.jones $
 */
@Component
@Provider
@Produces({RdfMediaType.APPLICATION_RDF_XML, RdfMediaType.TEXT_RDF_N3, 
    RdfMediaType.TEXT_TURTLE, MediaType.APPLICATION_JSON, 
    MediaType.WILDCARD})
@Consumes({RdfMediaType.APPLICATION_RDF_XML, RdfMediaType.TEXT_RDF_N3})
public final class JenaModelRdfProvider implements MessageBodyWriter<Model>,
        MessageBodyReader<Model> {
    
    static { ARQ.init(); } // Ensure RIOT hooks in
    
    @Override
    public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return Model.class.isAssignableFrom(type);
    }
    
    @Override
    public long getSize(Model o, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(final Model model, final Class<?> aClass, final Type type,
                        final Annotation[] annotations, final MediaType mediaType,
                        final MultivaluedMap<String, Object> stringObjectMultivaluedMap,
                        final OutputStream outputStream) throws IOException,
            WebApplicationException {
        
        // defaults to turtle
        switch (mediaType.toString()) {
            case "application/rdf+xml":
                model.write(outputStream, "RDF/XML-ABBREV"); break;
            case "application/json":
                model.write(outputStream, "RDF/JSON"); break;
            default:
                model.write(outputStream, "TTL");
        }

    }

    // ---- Reader implememtation

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return aClass == Model.class;
    }


    @Override
    public Model readFrom(Class<Model> objectClass, Type type, Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> stringStringMultivaluedMap,
                           InputStream inputStream) throws IOException, WebApplicationException {

        Model model = ModelFactory.createDefaultModel();

        // defaults to turtle
        switch (mediaType.toString()) {
            case "application/rdf+xml":
                model.read(inputStream, "RDF/XML"); break;
            case "application/json":
                model.read(inputStream, "RDF/JSON"); break;
            default:
                model.read(inputStream, "TTL");
        }

        return model;
    }
}
