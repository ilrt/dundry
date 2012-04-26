/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.dao;

import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;

/**
 *
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public interface MetadataStore {

    void create(String graphId, Model initialContent);

    Model getData(String graphId);

    ResultSet query(String query);
    
    void replaceData(String graphId, Model content);

    void create(String toInternalId);
}
