/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDB;
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
    
    public ResultSet query(String query) {
        QueryExecution qe = QueryExecutionFactory.create(query, store);
        try {
            ResultSet r = qe.execSelect();
            return ResultSetFactory.copyResults(r);
        } finally {
            qe.close();
        }
    }
    
    public Model getDataAbout(String subject) {
        return store.getNamedModel(subject);
    }
    
    public void updateDataAbout(String subject, String update) {
        
    }

    public void create(String graphId, Model initialContent) {
        store.getNamedModel(graphId).add(initialContent);
        TDB.sync(store);
    }
}
