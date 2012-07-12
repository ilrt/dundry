package uk.ac.bristol.dundry.webresources.adapter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.simpleframework.http.ContentType;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Response;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
class SFResponseAdapter implements Response {
    private final HttpServletResponse response;

    public SFResponseAdapter(HttpServletResponse response) {
        this.response = response;
    }

    @Override
    public void setContentLength(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    @Override
    public OutputStream getOutputStream(int i) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PrintStream getPrintStream() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public PrintStream getPrintStream(int i) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WritableByteChannel getByteChannel() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public WritableByteChannel getByteChannel(int i) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void commit() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reset() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws IOException {
        response.getOutputStream().close();
    }

    @Override
    public List<String> getNames() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void add(String string, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addDate(String string, long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void set(String string, String string1) {
        response.addHeader(string, string1);
    }

    @Override
    public void set(String string, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDate(String string, long l) {
        response.addDateHeader(string, l);
    }

    @Override
    public void remove(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean contains(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getValue(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
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
    public List<String> getValues(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Cookie setCookie(Cookie cookie) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Cookie setCookie(String string, String string1) {
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
    public ContentType getContentType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTransferEncoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getContentLength() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getCode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setCode(int i) {
        response.setStatus(i);
    }

    @Override
    public String getText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setText(String string) {
        // Deprecated?
    }

    @Override
    public int getMajor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMajor(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMinor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setMinor(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
