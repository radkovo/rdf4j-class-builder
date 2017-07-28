/**
 * RDFEntity.java
 *
 * Created on 26. 7. 2017, 13:43:26 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

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
    
    public IRI getIRI()
    {
        return iri;
    }
    
    //=====================================================================================

    abstract public IRI createIRI();
    
    abstract public void addToModel(Model model);
    
    abstract public void loadFromModel(Model model, EntityFactory factory);
    
    //=====================================================================================
    
    protected void addValue(Model model, IRI propertyIRI, String value)
    {
        model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addValue(Model model, IRI propertyIRI, int value)
    {
        model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addValue(Model model, IRI propertyIRI, float value)
    {
        model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addValue(Model model, IRI propertyIRI, double value)
    {
        model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addValue(Model model, IRI propertyIRI, Date value)
    {
        model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addArray(Model model, IRI propertyIRI, String[] values)
    {
        for (String value : values)
            model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addArray(Model model, IRI propertyIRI, int[] values)
    {
        for (int value : values)
            model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addArray(Model model, IRI propertyIRI, float[] values)
    {
        for (float value : values)
            model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addArray(Model model, IRI propertyIRI, double[] values)
    {
        for (double value : values)
            model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addArray(Model model, IRI propertyIRI, Date[] values)
    {
        for (Date value : values)
            model.add(getIRI(), propertyIRI, vf.createLiteral(value));
    }

    protected void addObject(Model model, IRI propertyIRI, RDFEntity obj)
    {
        model.add(getIRI(), propertyIRI, obj.getIRI());
    }

    protected void addCollection(Model model, IRI propertyIRI, Collection<? extends RDFEntity> col)
    {
        for (RDFEntity entity : col)
            model.add(getIRI(), propertyIRI, entity.getIRI());
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
    
    protected Set<IRI> getObjectIRIs(Model m, IRI predicate)
    {
        return Models.objectIRIs(m.filter(null, predicate, null));
    }

}
