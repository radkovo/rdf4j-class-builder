/**
 * RDFEntity.java
 *
 * Created on 26. 7. 2017, 13:43:26 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * A base class for all generated RDF entities.
 * 
 * @author burgetr
 */
abstract public class RDFEntity
{
    public static final ValueFactory vf = SimpleValueFactory.getInstance();
    
    private IRI iri;

    
    public RDFEntity(IRI iri)
    {
        this.iri = iri;
    }
    
    public RDFEntity(Model model, IRI iri)
    {
        this.iri = iri;
        loadFromModel(model);
    }
    
    public IRI getIRI()
    {
        return iri;
    }
    
    //=====================================================================================

    abstract public IRI createIRI();
    
    abstract public void addToModel(Model model);
    
    abstract public void loadFromModel(Model model);
    
    //=====================================================================================
    
    

}
