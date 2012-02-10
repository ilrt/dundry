/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MetadataStore {
    private final Dataset store;
    
    public MetadataStore(String location) {
        store = TDBFactory.createDataset(location);
    }
    
    public Model query(String query) {
        return null;
    }
    
    public Model getDataAbout(String subject) {
        return store.getNamedModel(subject);
    }
    
    public void updateDataAbout(String subject, String update) {
        
    }

    public void create(String graphId, Model initialContent) {
        store.getNamedModel(graphId).add(initialContent);
    }
}
