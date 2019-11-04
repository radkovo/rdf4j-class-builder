/**
 * TargetModel.java
 *
 * Created on 24. 6. 2019, 11:10:33 by burgetr
 */
package io.github.radkovo.rdf4j.builder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;

/**
 * An abstraction of a target model. It encapsulates a RDF4J {@link Model} and
 * implements adding the entities to the model. While adding the entities,
 * duplicities are checked and duplicate additions of an equal entity are
 * ignored.
 * 
 * @author burgetr
 */
public class TargetModel
{
    private Model model;
    private Set<RDFEntity> entities;
    
    
    /**
     * Creates a target encapsulating a given model.
     * @param model The RDF4J Model to be used for storage.
     */
    public TargetModel(Model model)
    {
        this.model = model;
        entities = new HashSet<>();
    }

    /**
     * Returns the RDF4J model used for storage.
     * @return The RDF4J model
     */
    public Model getModel()
    {
        return model;
    }

    /**
     * Gets all the entities that have been already stored.
     * @return A set of stored RDF4J entities.
     */
    public Set<RDFEntity> getEntities()
    {
        return entities;
    }
    
    /**
     * Adds a new entity to the model. If the entity has been already stored,
     * the addition has no efect.
     * @param entity The entity to add.
     */
    public void add(RDFEntity entity)
    {
        if (entities.add(entity))
        {
            entity.addToModel(this);
        }
    }
    
    /**
     * Adds a collection of entities to the model. The entities that have already
     * stored are ignored.
     * @param entities A collection of entities to add.
     */
    public void addAll(Collection<? extends RDFEntity> entities)
    {
        for (RDFEntity e : entities)
            add(e);
    }
}
