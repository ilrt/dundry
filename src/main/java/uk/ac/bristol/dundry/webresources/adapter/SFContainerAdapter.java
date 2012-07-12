package uk.ac.bristol.dundry.webresources.adapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.simpleframework.http.core.Container;

/**
 * Make a SimpleFramework Container useable via the more familiar
 * servlet operations
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class SFContainerAdapter {
    
    public static void handle(Container container, HttpServletRequest request, HttpServletResponse response) {
        container.handle(new SFRequestAdapter(request), new SFResponseAdapter(response));
    }
}
