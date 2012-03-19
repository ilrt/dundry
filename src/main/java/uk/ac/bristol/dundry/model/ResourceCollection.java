package uk.ac.bristol.dundry.model;

import com.hp.hpl.jena.rdf.model.Resource;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * A wrapper for resource collections to reify the generic collection and thus
 * help the RdfResourceMappingProvider.
 *  
 * @author Damian Steer <d.steer@bris.ac.uk>
 */
public final class ResourceCollection extends AbstractCollection<Resource> {
    private final Collection<Resource> coll;
    
    public ResourceCollection(Collection<Resource> coll) {
        this.coll = coll;
    }
    
    @Override
    public final Iterator<Resource> iterator() {
        return coll.iterator();
    }

    @Override
    public final int size() {
        return coll.size();
    }
    
}
