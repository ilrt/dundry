package uk.ac.bristol.dundry.model;

import java.util.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Wrap a list for the purposes of serialisation
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@XmlRootElement(name = "list")
public class ListWrapper {
    private final List l;
    
    public ListWrapper() { this.l = Collections.EMPTY_LIST; }
    
    public ListWrapper(List l) { this.l = l; }
    
    @XmlElement(name = "item")
    public List getData() { return l; }
    
}
