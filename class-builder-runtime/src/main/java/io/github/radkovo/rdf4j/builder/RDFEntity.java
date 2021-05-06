/**
 * RDFEntity.java
 *
 * Created on 26. 7. 2017, 13:43:26 by burgetr
 */
package io.github.radkovo.rdf4j.builder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

/**
 * A base class for all the generated RDF entities. It implements basic operations for
 * adding and retrieving the entities to/from a RDF graph represented by
 * a {@link TargetModel} instance.
 * 
 * @author burgetr
 */
abstract public class RDFEntity
{
    public static final ValueFactory vf = SimpleValueFactory.getInstance();
    
    private IRI iri;

    
    /**
     * Creates a new entity identified with an IRI.
     * @param iri The entity IRI.
     */
    public RDFEntity(IRI iri)
    {
        this.iri = iri;
    }
    
    /**
     * Gets the entity IRI.
     * @return The entity IRI
     */
    public IRI getIRI()
    {
        return iri;
    }
    
    //=====================================================================================
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((iri == null) ? 0 : iri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RDFEntity other = (RDFEntity) obj;
        if (iri == null)
        {
            if (other.iri != null) return false;
        }
        else if (!iri.equals(other.iri)) return false;
        return true;
    }
    
    //=====================================================================================

    /**
     * Creates a set of RDF triples describing the entity and stores the triples to a target model.
     * @param target The target model
     */
    public void addToModel(TargetModel target)
    {
        // rdf:type
        target.getModel().add(getIRI(), RDF.TYPE, getClassIRI());
        // rdfs:label
        if (getLabel() != null)
            target.getModel().add(getIRI(), RDFS.LABEL, vf.createLiteral(getLabel()));
        // additional triples are added in generated subclasses
    }
    
    /**
     * Loads the entity properties from a RDF4J model. For creating the referenced entities
     * a given factory is used.
     * @param model The source model to load the properties from
     * @param factory An entity factory for creating referenced entities while loading
     */
    public void loadFromModel(Model model, EntityFactory factory)
    {
        // retrieval is implemented in generated subclasses
    }
    
    /**
     * Gets the entity label (rdfs:label) if defined in the RDF model.
     * @return The label or {@code null} when no label is defined.
     */
    public String getLabel()
    {
        return null;
    }
    
    /**
     * Gets the IRI of the entity class.
     * @return The class IRI.
     */
    abstract public IRI getClassIRI();
    
    //=====================================================================================
    
    public void addValue(TargetModel target, IRI propertyIRI, String value)
    {
        if (value != null)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addValue(TargetModel target, IRI propertyIRI, int value)
    {
        target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addValue(TargetModel target, IRI propertyIRI, float value)
    {
        target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addValue(TargetModel target, IRI propertyIRI, double value)
    {
        target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addValue(TargetModel target, IRI propertyIRI, boolean value)
    {
        target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addValue(TargetModel target, IRI propertyIRI, Date value)
    {
        target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addValue(TargetModel target, IRI propertyIRI, URL value)
    {
        target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value.toString()));
    }

    public void addArray(TargetModel target, IRI propertyIRI, String[] values)
    {
        for (String value : values)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addArray(TargetModel target, IRI propertyIRI, int[] values)
    {
        for (int value : values)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addArray(TargetModel target, IRI propertyIRI, float[] values)
    {
        for (float value : values)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addArray(TargetModel target, IRI propertyIRI, double[] values)
    {
        for (double value : values)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addArray(TargetModel target, IRI propertyIRI, Date[] values)
    {
        for (Date value : values)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    public void addArray(TargetModel target, IRI propertyIRI, URL[] values)
    {
        for (URL value : values)
            target.getModel().add(getIRI(), propertyIRI, vf.createLiteral(value.toString(), XMLSchema.ANYURI));
    }

    public void addObject(TargetModel target, IRI propertyIRI, RDFEntity obj)
    {
        target.getModel().add(getIRI(), propertyIRI, obj.getIRI());
        target.add(obj);
    }

    public void addCollection(TargetModel target, IRI propertyIRI, Collection<? extends RDFEntity> col)
    {
        for (RDFEntity entity : col)
        {
            target.getModel().add(getIRI(), propertyIRI, entity.getIRI());
            target.add(entity);
        }
    }

    //=====================================================================================
    
    protected String[] loadStringArray(Model m, IRI pred)
    {
        Model stm = m.filter(null, pred, null);
        String[] ret = new String[stm.size()];
        int i = 0;
        for (Statement st : stm)
        {
            final Value val = st.getObject();
            if (val instanceof Literal)
                ret[i] = val.stringValue();
            i++;
        }
        return ret;
    }

    protected int[] loadIntArray(Model m, IRI pred)
    {
        Model stm = m.filter(null, pred, null);
        int[] ret = new int[stm.size()];
        int i = 0;
        for (Statement st : stm)
        {
            final Value val = st.getObject();
            if (val instanceof Literal)
                ret[i] = ((Literal) val).intValue();
            i++;
        }
        return ret;
    }

    protected float[] loadFloatArray(Model m, IRI pred)
    {
        Model stm = m.filter(null, pred, null);
        float[] ret = new float[stm.size()];
        int i = 0;
        for (Statement st : stm)
        {
            final Value val = st.getObject();
            if (val instanceof Literal)
                ret[i] = ((Literal) val).floatValue();
            i++;
        }
        return ret;
    }

    protected double[] loadDoubleArray(Model m, IRI pred)
    {
        Model stm = m.filter(null, pred, null);
        double[] ret = new double[stm.size()];
        int i = 0;
        for (Statement st : stm)
        {
            final Value val = st.getObject();
            if (val instanceof Literal)
                ret[i] = ((Literal) val).doubleValue();
            i++;
        }
        return ret;
    }

    protected Date[] loadDateArray(Model m, IRI pred)
    {
        Model stm = m.filter(null, pred, null);
        Date[] ret = new Date[stm.size()];
        int i = 0;
        for (Statement st : stm)
        {
            final Value val = st.getObject();
            if (val instanceof Literal)
                ret[i] = ((Literal) val).calendarValue().toGregorianCalendar().getTime();
            i++;
        }
        return ret;
    }

    protected URL[] loadURLArray(Model m, IRI pred)
    {
        Model stm = m.filter(null, pred, null);
        URL[] ret = new URL[stm.size()];
        int i = 0;
        for (Statement st : stm)
        {
            final Value val = st.getObject();
            if (val instanceof Literal)
            {
                try
                {
                    ret[i] = new URL(((Literal) val).stringValue());
                } catch (MalformedURLException e) {
                    //ignored
                }
            }
            i++;
        }
        return ret;
    }

    protected String loadStringValue(Model m, IRI pred)
    {
        String[] vals = loadStringArray(m, pred);
        return vals.length == 0 ? null : vals[0];
    }
    
    protected int loadIntValue(Model m, IRI pred)
    {
        int[] vals = loadIntArray(m, pred);
        return vals.length == 0 ? 0 : vals[0];
    }

    protected float loadFloatValue(Model m, IRI pred)
    {
        float[] vals = loadFloatArray(m, pred);
        return vals.length == 0 ? 0 : vals[0];
    }

    protected double loadDoubleValue(Model m, IRI pred)
    {
        double[] vals = loadDoubleArray(m, pred);
        return vals.length == 0 ? 0 : vals[0];
    }

    protected Date loadDateValue(Model m, IRI pred)
    {
        Date[] vals = loadDateArray(m, pred);
        return vals.length == 0 ? null : vals[0];
    }
    
    protected URL loadURLValue(Model m, IRI pred)
    {
        URL[] vals = loadURLArray(m, pred);
        return vals.length == 0 ? null : vals[0];
    }
    
    protected Set<IRI> getObjectIRIs(Model m, IRI predicate)
    {
        return Models.objectIRIs(m.filter(null, predicate, null));
    }

}
