package uk.ac.bristol.dundry.webresources.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.simpleframework.http.*;
import org.simpleframework.http.parse.AddressParser;
import org.simpleframework.http.parse.PathParser;
import org.simpleframework.http.parse.QueryParser;
import org.simpleframework.http.session.Session;
import org.simpleframework.util.lease.LeaseException;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
class SFRequestAdapter implements Request {
    private final HttpServletRequest request;

    public SFRequestAdapter(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public boolean isKeepAlive() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Form getForm() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getParameter(String string) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Part getPart(String string) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Map getAttributes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getAttribute(Object o) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InetSocketAddress getClientAddress() {
        return new InetSocketAddress(request.getRemoteHost(), request.getRemotePort());
    }

    @Override
    public String getContent() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }

    @Override
    public ReadableByteChannel getByteChannel() throws IOException {
        return Channels.newChannel(getInputStream());
    }

    @Override
    public Session getSession() throws LeaseException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Session getSession(boolean bln) throws LeaseException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getNames() {
        return Collections.list(request.getHeaderNames());
    }

    @Override
    public int getInteger(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getDate(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Cookie getCookie(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Cookie> getCookies() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getValue(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getValues(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Locale> getLocales() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ContentType getContentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getContentLength() {
        return request.getContentLength();
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getTarget() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Address getAddress() {
        AddressParser addr = new AddressParser();
        addr.setScheme(request.getScheme());
        addr.setDomain(request.getServerName());
        addr.setPort(request.getServerPort());
        addr.setPath(request.getRequestURI());
        addr.setQuery(request.getQueryString());
        return addr;
    }

    @Override
    public Path getPath() {
        // HACK!!!
        return new PathParser("/announce");
    }

    @Override
    public Query getQuery() {
        return new QueryParser(request.getQueryString());
    }

    @Override
    public int getMajor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMinor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
