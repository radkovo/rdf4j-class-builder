/**
 * ClassBuilder.java
 *
 * Created on 20. 12. 2020, 10:28:55 by burgetr
 */
package io.github.radkovo.rdf4j.builder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A base class for generators. It reads an OWL definition in any supported format and provides
 * the functions for obtaining the class and property details.
 * 
 * @author burgetr
 */
public abstract class ClassBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ClassBuilder.class);

    private static final IRI[] COMMENT_PROPERTIES = new IRI[]{RDFS.COMMENT, DCTERMS.DESCRIPTION, SKOS.DEFINITION, DC.DESCRIPTION};
    private static final IRI[] LABEL_PROPERTIES = new IRI[]{RDFS.LABEL, DCTERMS.TITLE, DC.TITLE, SKOS.PREF_LABEL, SKOS.ALT_LABEL};
    private static final IRI[] PROPERTY_PROPERTIES = new IRI[]{RDF.PROPERTY, OWL.DATATYPEPROPERTY, OWL.OBJECTPROPERTY};
    private static final Set<IRI> classPredicates;
    static {
        classPredicates = new HashSet<>();
        classPredicates.add(RDFS.CLASS);
        classPredicates.add(OWL.CLASS);
    }
    
    //generation settings
    private String indent = "\t";
    private String language = null;
    private String vocabName = null;

    //ontology data
    private Model model;
    
    /**
     * Creates and empty class builder. The models may be loaded using {@link ClassBuilder#load(String, RDFFormat)}.
     */
    public ClassBuilder()
    {
    }
    
    /**
     * Creates a new class builder for the specified input file.
     * 
     * @param filename input file specification
     * @param format input file MIME type (such as application/rdf+xml) or {@code null} for automatic detection.
     * @throws IOException
     * @throws RDFParseException
     */
    public ClassBuilder(String filename, String format) throws IOException, RDFParseException
    {
        load(filename, format);
    }
    
    /**
     * Creates a new class builder for the specified input file.
     * 
     * @param filename input file specification
     * @param format input file format (see the {@link RDFFormat} constants) or {@code null} for automatic detection.
     * @throws IOException
     * @throws RDFParseException
     */
    public ClassBuilder(String filename, RDFFormat format) throws IOException, RDFParseException
    {
        load(filename, format);
    }
    
    /**
     * Loads an input model from the specified input file.
     * 
     * @param filename input file specification
     * @param format input file MIME type (such as application/rdf+xml) or {@code null} for automatic detection.
     * @throws IOException
     * @throws RDFParseException
     */
    public void load(String filename, String format) throws IOException, RDFParseException
    {
        load(filename, format != null ? Rio.getParserFormatForMIMEType(format).orElse(null) : null);
    }
    
    /**
     * Loads an input model from the specified input file.
     * 
     * @param filename input file specification
     * @param format input file format (see the {@link RDFFormat} constants) or {@code null} for automatic detection.
     * @throws IOException
     * @throws RDFParseException
     */
    public void load(String filename, RDFFormat format) throws IOException, RDFParseException
    {
        Path file = Paths.get(filename);
        if (!Files.exists(file)) throw new FileNotFoundException(filename);

        if (format == null) {
            format = Rio.getParserFormatForFileName(filename).orElse(null);
            log.trace("detected input format from filename {}: {}", filename, format);
        }

        try (final InputStream inputStream = Files.newInputStream(file)) {
            log.trace("Loading input file");
            Model newmodel = Rio.parse(inputStream, "", format);
            if (model == null)
                model = newmodel;
            else
                model.addAll(newmodel);
        }
    }

    public Model getModel()
    {
        return model;
    }

    /**
     * Gets the used indentation string.
     * @return the indentation string
     */
    public String getIndent()
    {
        return indent;
    }

    /**
     * Sets the character sequence used for indentation. Default is '\t'.
     * @param indent the indentation string
     */
    public void setIndent(String indent)
    {
        this.indent = indent;
    }

    /**
     * Gets the preferred language for RDF literals.
     * @return the RDF literals language or {@code null} when not set.
     */
    public String getPreferredLanguage()
    {
        return language;
    }

    /**
     * Sets the preferred language for RDF literals.
     * @param language the RDF literals language
     */
    public void setPreferredLanguage(String language)
    {
        this.language = language;
    }

    /**
     * Gets the name of the generated vocabulary class.
     * @return The vocabulary class name (without the package).
     */
    public String getVocabName()
    {
        return vocabName;
    }

    /**
     * Sets the name of the generated vocabulary class.
     * @param vocabName the vocabulary class name (without the package).
     */
    public void setVocabName(String vocabName)
    {
        this.vocabName = vocabName;
    }

    /**
     * Gets the mapping of known data types to string names.
     * 
     * @return A map from type IRIs to string names.
     */
    protected abstract Map<IRI, String> getDataTypes();

    /**
     * Default type to be used when rdfs:range is not defined.
     * 
     * @return a type name on the target platform (e.g. String)
     */
    protected abstract String getDefaultType();
    
    /**
     * Creates an object type name from an IRI
     * @param iri the iri of the type
     * @return the type name
     */
    protected abstract String getObjectType(IRI iri);
    
    /**
     * Creates a name of array of simple values of the given type.
     * @param type the type
     * @return the array name (e.g. int[])
     */
    protected abstract String getArrayType(String type);
    
    /**
     * Creates a name of array of objects values of the given type.
     * @param type the type
     * @return the collection name (e.g. Set&lt;Object&gt;)
     */
    protected abstract String getCollectionType(String type);
    
    //=======================================================================================================
    
    /**
     * Generates all the classes and stores them to the given output directory.
     * 
     * @param outputDirName the output directory path
     * @throws IOException
     */
    public void generate(String outputDirName) throws IOException
    {
        Path outputDir = Paths.get(outputDirName);
        generate(outputDir);
    }
    
    /**
     * Generates all the classes and stores them to the given output directory.
     * 
     * @param outputDir the output directory path
     * @throws IOException
     */
    public abstract void generate(Path outputDir) throws IOException;
    
    //=======================================================================================================

    protected Set<Resource> findClasses()
    {
        final Set<Resource> classes = new HashSet<>();
        Model types = getModel().filter(null, RDF.TYPE, null);
        for (Statement st : types)
        {
            if (classPredicates.contains(st.getObject()))
                classes.add(st.getSubject());
        }
        return classes;
    }
    
    protected String getClassName(IRI iri)
    {
        return iri.getLocalName();
    }
    
    protected Set<IRI> findClassProperties(IRI classIRI)
    {
        return findClassProperties(classIRI, RDFS.DOMAIN);
    }
    
    protected Set<IRI> findClassProperties(IRI classIRI, IRI predicate)
    {
        final Set<IRI> ret = new HashSet<>();
        for (IRI pred : PROPERTY_PROPERTIES)
        {
            for (Statement st : getModel().filter(null, RDF.TYPE, pred))
            {
                if (st.getSubject() instanceof IRI)
                {
                    IRI propertyIri = (IRI) st.getSubject();
                    Set<Value> domains = getReferencedTypes(propertyIri, predicate);
                    if (domains.contains(classIRI))
                        ret.add(propertyIri);
                }
            }
        }
        return ret;
    }
    
    /**
     * Finds all domain or range types referenced by a property specification.
     * TODO multiple domains or ranges should be treated as intersection but we have no way how
     * to treat this in Java.
     * @param propertyIri the property (owl:Property) IRI
     * @param predicate rdfs:domain or rdfs:range
     * @return a set of referenced types
     */
    protected Set<Value> getReferencedTypes(IRI propertyIri, IRI predicate)
    {
        int cnt = 0; 
        Set<Value> ret = new HashSet<>();
        for (Statement st : getModel().filter(propertyIri, predicate, null))
        {
            if (st.getObject() instanceof IRI) //IRIs of objects referenced directly
            {
                ret.add(st.getObject());
            }
            else if (st.getObject() instanceof BNode) //a blank node - check for a referenced union
            {
                //TODO many more other cases may occur here probably. We just assume an anonymous class defined by union of normal classes.
                Set<IRI> types = getUnionTypes((Resource) st.getObject());
                ret.addAll(types);
            }
            cnt++;
        }
        if (cnt > 1) {
            log.warn("Multiple specifications of {} for property {}. This may not work as expected since we can't"
                     + " handle intersections properly in the class builder.", predicate, propertyIri);
        }
        return ret;
    }
    
    /**
     * Gets the types of a class defined by a union.
     * @param subj the URI of a class defined by union
     * @return the types the union refers too.
     */
    protected Set<IRI> getUnionTypes(Resource subj)
    {
        Set<IRI> ret = new HashSet<>();
        for (Statement st : getModel().filter(subj, OWL.UNIONOF, null))
        {
            if (st.getObject() instanceof Resource)
            {
                //try to treat the union a RDF collection
                List<Value> vals = new ArrayList<>();
                RDFCollections.asValues(getModel(), (Resource) st.getObject(), vals);
                for (Value val : vals)
                {
                    if (val instanceof IRI)
                        ret.add((IRI) val);
                }
            }
        }
        return ret;
    }
    
    protected String getPropertyName(IRI iri)
    {
        return iri.getLocalName();
    }
    
    protected String getReversePropertyName(IRI iri)
    {
        final String propertyType = getPropertySourceClass(iri);
        if (propertyType != null)
            return English.plural(propertyType.substring(0, 1).toLowerCase() + propertyType.substring(1));
        else
            return null;
    }
    
    /**
     * Creates a target platform name of the type identified by the IRI.
     * @param iri
     * @return
     */
    protected String getPropertyDataType(IRI iri)
    {
        IRI range = getOptionalObjectIRI(getModel(), iri, RDFS.RANGE);
        String type = getDefaultType();
        if (range != null)
        {
            if (getDataTypes().containsKey(range)) //known data types
            {
                type = getDataTypes().get(range);
                if (!isFunctionalProperty(iri))
                    type = getArrayType(type);
            }
            else if (range.getNamespace().equals(iri.getNamespace())) //local data types -- object properties
            {
                type = getObjectType(range);
                if (!isFunctionalProperty(iri))
                    type = getCollectionType(type);
            }
        }
        return type;
    }

    protected String getPropertySourceType(IRI iri)
    {
        IRI domain = getOptionalObjectIRI(getModel(), iri, RDFS.DOMAIN);
        return domain == null ? null : getObjectType(domain);
    }

    protected String getPropertySourceClass(IRI iri)
    {
        IRI domain = getOptionalObjectIRI(getModel(), iri, RDFS.DOMAIN);
        return domain == null ? null : getClassName(domain);
    }

    protected String getPropertyClassification(IRI iri)
    {
        IRI range = getOptionalObjectIRI(getModel(), iri, RDFS.RANGE);
        String type = "Value";
        if (range != null)
        {
            if (getDataTypes().containsKey(range)) //known data types
            {
                type = "Value";
                if (!isFunctionalProperty(iri))
                    type = "Array";
            }
            else if (range.getNamespace().equals(iri.getNamespace())) //local data types -- object properties
            {
                type = "Object";
                if (!isFunctionalProperty(iri))
                    type = "Collection";
            }
        }
        return type;
    }
    
    protected boolean isFunctionalProperty(IRI iri)
    {
        Model m = getModel().filter(iri, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
        return m.size() != 0;
    }
    
    protected boolean isInverseFunctionalProperty(IRI iri)
    {
        Model m = getModel().filter(iri, RDF.TYPE, OWL.INVERSEFUNCTIONALPROPERTY);
        return m.size() != 0;
    }
    
    protected boolean isObjectOrCollectionProperty(IRI piri)
    {
        return getPropertyClassification(piri).equals("Object")
                ||  getPropertyClassification(piri).equals("Collection");
    }
    
    protected Literal getResourceTitle(IRI iri)
    {
        return getFirstExistingObjectLiteral(getModel(), iri, getPreferredLanguage(), LABEL_PROPERTIES);
    }
    
    protected Literal getResourceDescription(IRI iri)
    {
        return getFirstExistingObjectLiteral(getModel(), iri, getPreferredLanguage(), COMMENT_PROPERTIES);
    }
    
    protected Literal getFirstExistingObjectLiteral(Model model, Resource subject, String lang, IRI... predicates)
    {
        for (IRI predicate : predicates)
        {
            Literal literal = getOptionalObjectLiteral(model, subject, predicate, lang);
            if (literal != null) { return literal; }
        }
        return null;
    }

    protected Literal getOptionalObjectLiteral(Model model, Resource subject, IRI predicate, String lang)
    {
        Set<Value> objects = model.filter(subject, predicate, null).objects();

        Literal result = null;

        for (Value nextValue : objects)
        {
            if (nextValue instanceof Literal)
            {
                final Literal literal = (Literal) nextValue;
                if (result == null || (lang != null
                        && lang.equals(literal.getLanguage().orElse(null))))
                {
                    result = literal;
                }
            }
        }
        return result;
    }

    protected IRI getOptionalObjectIRI(Model model, Resource subject, IRI predicate)
    {
        Set<Value> objects = model.filter(subject, predicate, null).objects();
        for (Value nextValue : objects)
        {
            if (nextValue instanceof IRI)
                return (IRI) nextValue;
        }
        return null;
    }
    
    protected String getIndent(int level) 
    {
        return StringUtils.repeat(getIndent(), level);
    }
    
}
