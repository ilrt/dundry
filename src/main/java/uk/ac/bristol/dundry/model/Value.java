package uk.ac.bristol.dundry.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A simple wrapper to serialise a single value in json and xml
 * 
 * @author pldms
 */
@XmlRootElement
public class Value {
    final Object value;
    
    // Zero-arg constructor required
    public Value() { this.value = null; }
    
    public Value(Object value) {
        this.value = value;
    }
    
    @XmlElement(type = String.class)
    public String getValue() { return value.toString(); }
}
