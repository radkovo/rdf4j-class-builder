/**
 * RDFEntity.java
 *
 * Created on 26. 7. 2017, 13:43:26 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

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
    
    abstract public void addToModel(Model model);
    
    //=====================================================================================
    
    

}
