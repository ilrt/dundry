/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.DatasetImpl;
import uk.ac.bristol.dundry.dao.MetadataStore;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class MetadataStoreDS implements MetadataStore {
    private final Dataset store;
    
    public MetadataStoreDS(Dataset store) { this.store = store; }
    
    @Override
    public void create(String graphId) {
        // nothing to do here. Created on demand.
    }
    
    @Override
    public void create(String graphId, Model initialContent) {
        store.getNamedModel(graphId).add(initialContent);
    }

    @Override
    public Model getData(String graphId) {
        return ModelFactory.createDefaultModel().add(store.getNamedModel(graphId));
    }

    @Override
    public ResultSet query(String query) {
        QueryExecution qe = QueryExecutionFactory.create(query, store);
        try {
            ResultSet r = qe.execSelect();
            return ResultSetFactory.copyResults(r);
        } finally {
            qe.close();
        }
    }

    @Override
    public void replaceData(String graphId, Model content) {
        store.getNamedModel(graphId).removeAll().add(content);
    }
    
}
