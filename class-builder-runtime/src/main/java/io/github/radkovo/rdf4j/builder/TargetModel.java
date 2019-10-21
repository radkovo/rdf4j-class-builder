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
 * A storage target that represents a rdf4j Model
 * 
 * @author burgetr
 */
public class TargetModel
{
    private Model model;
    private Set<RDFEntity> entities;
    
    
    public TargetModel(Model model)
    {
        this.model = model;
        entities = new HashSet<>();
    }

    public Model getModel()
    {
        return model;
    }

    public Set<RDFEntity> getEntities()
    {
        return entities;
    }
    
    public void add(RDFEntity entity)
    {
        if (entities.add(entity))
        {
            entity.addToModel(this);
        }
    }
    
    public void addAll(Collection<? extends RDFEntity> entities)
    {
        for (RDFEntity e : entities)
            add(e);
    }
}
