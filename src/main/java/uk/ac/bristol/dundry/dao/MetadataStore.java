/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.rdf.model.Model;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MetadataStore {
    
    public Model query(String query) {
        return null;
    }
    
    public Model getDataAbout(String subject) {
        return null;
    }
    
    public void updateDataAbout(String subject, String update) {
        
    }

    void create(String id) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
