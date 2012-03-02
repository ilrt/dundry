package uk.ac.bristol.dundry.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
@XmlRootElement(name = "deposit")
public class DepositDescription {
    private final String id;
    private final String title;
    
    public DepositDescription() {
        this.id = "";
        this.title = "";
    }
    
    public DepositDescription(String id, String title) {
        this.id = id;
        this.title = title;
    }
    
    @XmlElement
    public String getId() { return id; }
    @XmlElement
    public String getTitle() { return title; }
}
