/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bristol.dundry.tasks;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import java.nio.file.Path;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.bristol.dundry.dao.Repository;
import uk.ac.bristol.dundry.vocabs.RepositoryVocab;

/**
 * Task to change the state, typically at the end of a series of tasks
 * 
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public class StateChanger extends JobBase {
    
    final static Logger log = LoggerFactory.getLogger(StateChanger.class);
    
    public final static String TO_STATE = "to-state";
    
    @Override
    public void execute(Repository repo, Resource item, Resource prov, String id, Path root, JobDataMap jobData) throws JobExecutionException {
        String state = jobData.getString(TO_STATE);
        log.debug("Changing id {} to state {}", id, state);
        // We need to remove data, so updating prov won't work
        Resource provenance = repo.getProvenanceMetadata(id);
        // Remove existing state
        provenance.removeAll(RepositoryVocab.state);
        // Set new state
        provenance.addProperty(RepositoryVocab.state, state);
        repo.updateProvenanceMetadata(id, provenance);
    }
    
}
