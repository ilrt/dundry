/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MetadataStoreTDB implements MetadataStore {
    
    protected final Dataset store;
    
    public MetadataStoreTDB(String location) {
        store = TDBFactory.createDataset(location);
    }

    @Override
    public void create(String graphId, Model initialContent) {
        store.begin(ReadWrite.WRITE);
        try {
            store.getNamedModel(graphId).add(initialContent);
            store.commit();
        } finally {
            store.end();
        }
    }
    
    @Override
    public void replaceData(String graphId, Model model) {
        store.begin(ReadWrite.WRITE);
        try {
            store.getNamedModel(graphId).removeAll().add(model);
            store.commit();
        } finally {
            store.end();
        }
    }
    @Override
    public Model getData(String graphId) {
        return store.getNamedModel(graphId);
    }

    @Override
    public ResultSet query(String query) {
        QueryExecution qe = QueryExecutionFactory.create(query, store);
        try {
            store.begin(ReadWrite.READ);
            ResultSet r = qe.execSelect();
            return ResultSetFactory.copyResults(r);
        } finally {
            qe.close();
            store.end();
        }
    }
}
