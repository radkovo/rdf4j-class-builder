/**
 * ClassBuilder.java
 *
 * Created on 26. 7. 2017, 13:44:33 by burgetr
 */
package io.github.radkovo.rdf4j.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.atteo.evo.inflector.English;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class builder. It reads an OWL definition in any supported format and generates a set of Java
 * files based on the generation parametres.
 * 
 * @author burgetr
 */
public class ClassBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ClassBuilder.class);

    private static final String DEFAULT_SUPERCLASS = "com.github.radkovo.rdf4j.builder.RDFEntity";
    private static final IRI[] COMMENT_PROPERTIES = new IRI[]{RDFS.COMMENT, DCTERMS.DESCRIPTION, SKOS.DEFINITION, DC.DESCRIPTION};
    private static final IRI[] LABEL_PROPERTIES = new IRI[]{RDFS.LABEL, DCTERMS.TITLE, DC.TITLE, SKOS.PREF_LABEL, SKOS.ALT_LABEL};
    private static final IRI[] PROPERTY_PROPERTIES = new IRI[]{RDF.PROPERTY, OWL.DATATYPEPROPERTY, OWL.OBJECTPROPERTY};
    private static final Set<IRI> classPredicates;
    static {
        classPredicates = new HashSet<>();
        classPredicates.add(RDFS.CLASS);
        classPredicates.add(OWL.CLASS);
    }
    private static final Map<IRI, String> dataTypes;
    static {
        dataTypes = new HashMap<>();
        dataTypes.put(XMLSchema.BOOLEAN, "boolean");
        dataTypes.put(XMLSchema.BYTE, "byte");
        dataTypes.put(XMLSchema.DATE, "java.util.Date");
        dataTypes.put(XMLSchema.DATETIME, "java.util.Date");
        dataTypes.put(XMLSchema.DECIMAL, "float");
        dataTypes.put(XMLSchema.DOUBLE, "double");
        dataTypes.put(XMLSchema.FLOAT, "float");
        dataTypes.put(XMLSchema.INT, "int");
        dataTypes.put(XMLSchema.INTEGER, "int");
        dataTypes.put(XMLSchema.LONG, "long");
        dataTypes.put(XMLSchema.POSITIVE_INTEGER, "int");
        dataTypes.put(XMLSchema.SHORT, "short");
        dataTypes.put(XMLSchema.STRING, "String");
        dataTypes.put(XMLSchema.TIME, "java.util.Date");
        dataTypes.put(XMLSchema.ANYURI, "java.net.URL");
    }
    
    //generation parametres
    private String packageName = null;
    private String vocabPackageName = null;
    private String vocabName = null;
    private String indent = "\t";
    private String language = null;
    
    //ontology data
    private final Model model;

    
    public ClassBuilder(String filename, String format) throws IOException, RDFParseException
    {
        this(filename, format != null ? Rio.getParserFormatForMIMEType(format).orElse(null) : null);
    }
    
    public ClassBuilder(String filename, RDFFormat format) throws IOException, RDFParseException
    {
        Path file = Paths.get(filename);
        if (!Files.exists(file)) throw new FileNotFoundException(filename);

        if (format == null) {
            format = Rio.getParserFormatForFileName(filename).orElse(null);
            log.trace("detected input format from filename {}: {}", filename, format);
        }

        try (final InputStream inputStream = Files.newInputStream(file)) {
            log.trace("Loading input file");
            model = Rio.parse(inputStream, "", format);
        }
    }

    public String getPackageName()
    {
        return packageName;
    }

    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    public String getVocabPackageName()
    {
        return vocabPackageName;
    }

    public void setVocabPackageName(String vocabPackageName)
    {
        this.vocabPackageName = vocabPackageName;
    }

    public String getVocabName()
    {
        return vocabName;
    }

    public void setVocabName(String vocabName)
    {
        this.vocabName = vocabName;
    }

    public String getIndent()
    {
        return indent;
    }

    public void setIndent(String indent)
    {
        this.indent = indent;
    }

    public String getPreferredLanguage()
    {
        return language;
    }

    public void setPreferredLanguage(String language)
    {
        this.language = language;
    }
    
    //=======================================================================================================
    
    public void generate(String outputDirName) throws IOException
    {
        Path outputDir = Paths.get(outputDirName);
        generate(outputDir);
    }
    
    public void generate(Path outputDir) throws IOException
    {
        if (!Files.isDirectory(outputDir))
            throw new FileNotFoundException(outputDir.toString());
        
        //find all classes in the model
        Set<Resource> classes = findClasses();
        log.info("Found clases: {}", classes);
        
        //generate the classes
        for (Resource cres : classes)
        {
            if (cres instanceof IRI)
                generateClass((IRI) cres, outputDir);
            else
                log.warn("Skipping resource {} -- not an IRI", cres);
        }
        
        //generate factory
        generateFactory(classes, outputDir);
    }

    //=======================================================================================================
    
    public void generateFactory(Set<Resource> classes, Path outputDir) throws IOException
    {
        String fname = getFactoryName();
        File outfile = new File(outputDir.toFile(), fname + ".java");
        PrintWriter out = new PrintWriter(outfile);
        generateFactory(classes, fname, out);
        out.close();
    }
    
    public void generateFactory(Set<Resource> classes, String fname, PrintWriter out)
    {
        log.info("Generating factory interface {}", fname);
        
        //generate package
        if (getPackageName() != null)
            out.printf("package %s;\n\n", getPackageName());
        
        //imports
        out.println("import org.eclipse.rdf4j.model.IRI;");
        out.println("import com.github.radkovo.rdf4j.builder.EntityFactory;");
        out.println();
        
        //generate interface
        out.printf("public interface %s extends EntityFactory{\n", fname);
        
        //declare 'create' methods
        for (Resource cres : classes)
        {
            if (cres instanceof IRI)
            {
                String cname = getClassName((IRI) cres);
                out.printf(getIndent(1) + "public %s create%s(IRI iri);\n", cname, cname);
            }
        }
        
        //end of interface
        out.println("}");
    }
    
    private String getFactoryName()
    {
        return getVocabName() + "Factory";
    }
    
    //=======================================================================================================
    
    public void generateClass(IRI cres, Path outputDir) throws IOException
    {
        String className = getClassName(cres);
        File outfile = new File(outputDir.toFile(), className + ".java");
        PrintWriter out = new PrintWriter(outfile);
        generateClass(cres, className, out);
        out.close();
    }
    
    public void generateClass(IRI iri, String className, PrintWriter out)
    {
        log.info("Generating {}", className);
        
        //some statistics
        Set<IRI> properties = findClassProperties(iri);
        Set<IRI> revProperties = findClassProperties(iri, RDFS.RANGE); //reverse properties
        log.debug("   properties: {}", properties);
        boolean somePropertiesNotFunctional = false;
        boolean someCollections = false;
        boolean someObjects = false;
        for (IRI piri : properties)
        {
            if (!isFunctionalProperty(piri))
                somePropertiesNotFunctional = true;
            if (getPropertyClassification(piri).equals("Object"))
                someObjects = true;
            if (getPropertyClassification(piri).equals("Collection"))
                someCollections = true;
        }
        for (IRI piri : revProperties)
        {
            if (isObjectOrCollectionProperty(piri) && !isInverseFunctionalProperty(piri))
                someCollections = true;
        }
        
        //generate package
        if (getPackageName() != null)
            out.printf("package %s;\n\n", getPackageName());
        
        //imports
        if (somePropertiesNotFunctional || someCollections || someObjects)
            out.println("import java.util.Set;");
        if (someCollections)
            out.println("import java.util.HashSet;");
        out.println("import org.eclipse.rdf4j.model.IRI;");
        out.println("import org.eclipse.rdf4j.model.Model;");
        out.println("import com.github.radkovo.rdf4j.builder.EntityFactory;");
        out.println("import com.github.radkovo.rdf4j.builder.TargetModel;");
        if (getVocabPackageName() != null && getVocabName() != null)
            out.printf("import %s.%s;\n", getVocabPackageName(), getVocabName());
        out.println();

        generateJavadoc(iri, out, 0);

        //super class
        boolean derived = false;
        String superClass = DEFAULT_SUPERCLASS;
        IRI superClassIRI = getOptionalObjectIRI(model, iri, RDFS.SUBCLASSOF);
        if (superClassIRI != null)
        {
            derived = true;
            superClass = getClassName(superClassIRI);
        }
        //class definition
        out.printf("public class %s extends %s\n", className, superClass);
        out.println("{");
        
        //namespace IRI
        out.printf(getIndent(1) + "public static final IRI CLASS_IRI = vf.createIRI(\"%s\");\n\n", iri);
        
        //generate properties
        for (IRI piri : properties)
            generatePropertyDeclaration(piri, getPropertyName(piri), out);
        //reverse property declarations
        for (IRI piri : revProperties)
        {
            //log.debug("CLASS {} : {}", piri, getPropertyClassification(piri));
            if (isObjectOrCollectionProperty(piri) && !isInverseFunctionalProperty(piri))
            {
                generateReverseCollectionDeclaration(piri, getPropertyName(piri), getPropertySourceType(piri), out);
            }
        }
        out.println();
        
        //constructors
        generateConstructors(className, properties, revProperties, out);
        out.println();
        generateDefaultMethods(className, out);
        out.println();
        
        //getters and setters
        for (IRI piri : properties)
        {
            generatePropertyGetter(piri, getPropertyName(piri), out);
            out.println();
            if (isFunctionalProperty(piri)) //omit setters for non-functional properties (collections)
            {
                generatePropertySetter(piri, getPropertyName(piri), out);
                out.println();
            }
        }
        
        //adders for reverse 1:N properties
        for (IRI piri : revProperties)
        {
            if (isObjectOrCollectionProperty(piri) && !isInverseFunctionalProperty(piri))
            {
                generateRevPropertyGetterAdder(piri, getPropertyName(piri), getPropertySourceType(piri), out);
                out.println();
            }
        }
        
        //generate addToModel
        generateAddToModel(properties, revProperties, out);
        out.println();
        generateLoadFromModel(properties, out, someCollections || someObjects);
        
        //finish class definition
        out.println("}");
    }

    protected void generatePropertyDeclaration(IRI iri, String propertyName, PrintWriter out)
    {
        generateJavadoc(iri, out, 1);
        String type = getPropertyDataType(iri);
        out.printf(getIndent(1) + "private %s %s;\n", type, propertyName);
        out.println();
    }

    protected void generateReverseCollectionDeclaration(IRI iri, String propertyName, String propertyType, PrintWriter out)
    {
        out.printf(getIndent(1) + "/** Inverse collection for %s.%s. */\n", propertyType, propertyName);
        String varName = getReversePropertyName(iri);
        out.printf(getIndent(1) + "private Set<%s> %s;\n", propertyType, varName);
        out.println();
    }
    
    protected void generatePropertyGetter(IRI iri, String propertyName, PrintWriter out)
    {
        String name = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String type = getPropertyDataType(iri);
        out.printf(getIndent(1) + "public %s %s() {\n", type, name);
        out.printf(getIndent(2) + "return %s;\n", propertyName);
        out.println(getIndent(1) + "}");
    }

    protected void generatePropertySetter(IRI iri, String propertyName, PrintWriter out)
    {
        String name = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        String type = getPropertyDataType(iri);
        out.printf(getIndent(1) + "public void %s(%s %s) {\n", name, type, propertyName);
        out.printf(getIndent(2) + "this.%s = %s;\n", propertyName, propertyName);
        out.println(getIndent(1) + "}");
    }

    protected void generateRevPropertyGetterAdder(IRI iri, String propertyName, String propertyType, PrintWriter out)
    {
        String adderName = "add" + propertyType;
        String paramName = propertyType.substring(0, 1).toLowerCase() + propertyType.substring(1);
        String varName = getReversePropertyName(iri);
        String getterName = "get" + English.plural(propertyType);
        
        out.printf(getIndent(1) + "public Set<%s> %s() {\n", propertyType, getterName);
        //out.printf(getIndent(2) + "return (%s == null) ? new HashSet<>() : %s;\n", varName, varName);
        out.printf(getIndent(2) + "return %s;\n", varName);
        out.println(getIndent(1) + "}");
        out.println();
        
        out.printf(getIndent(1) + "public void %s(%s %s) {\n", adderName, propertyType, paramName);
        out.printf(getIndent(2) + "if (%s == null) %s = new HashSet<>();\n", varName, varName);
        out.printf(getIndent(2) + "%s.add(%s);\n", varName, paramName);
        if (getPropertyClassification(iri).equals("Object"))
        {
            String otherSetter = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            out.printf(getIndent(2) + "%s.%s(this);\n", paramName, otherSetter);
        }
        else
        {
            String other = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
            out.printf(getIndent(2) + "%s.%s().add(this);\n", paramName, other);
        }
        out.println(getIndent(1) + "}");
    }

    protected void generateConstructors(String className, Set<IRI> properties, Set<IRI> revProperties, PrintWriter out)
    {
        out.printf(getIndent(1) + "public %s(IRI iri) {\n", className);
        out.println(getIndent(2)+ "super(iri);");
        //Property initialization - create collections
        for (IRI piri : properties)
        {
            if (getPropertyClassification(piri).equals("Collection"))
            {
                String propertyName = getPropertyName(piri);
                String propertyType = getPropertyDataType(piri);
                out.printf(getIndent(2) + "%s = new Hash%s();\n", propertyName, propertyType);
            }
        }
        //reverse property initialization
        for (IRI piri : revProperties)
        {
            if (isObjectOrCollectionProperty(piri) && !isInverseFunctionalProperty(piri))
            {
                String propertyName = getReversePropertyName(piri);
                out.printf(getIndent(2) + "%s = new HashSet<>();\n", propertyName);
            }
        }
        
        out.println(getIndent(1)+ "}");
        /*out.println();
        
        out.printf(getIndent(1) + "public %s(Model model, IRI iri) {\n", className);
        out.println(getIndent(2)+ "super(model, iri);");
        out.println(getIndent(1)+ "}");*/
    }
    
    protected void generateDefaultMethods(String className, PrintWriter out)
    {
        out.println(getIndent(1) + "@Override");
        out.println(getIndent(1) + "public IRI getClassIRI() {");
        out.printf(getIndent(2) + "return %s.CLASS_IRI;\n", className);
        out.println(getIndent(1) + "}");
    }
    
    protected void generateAddToModel(Collection<IRI> properties, Collection<IRI> revProperties, PrintWriter out)
    {
        out.println(getIndent(1) + "@Override");
        out.println(getIndent(1) + "public void addToModel(TargetModel target) {");
        out.println(getIndent(2) + "super.addToModel(target);");
        
        for (IRI piri : properties)
        {
            out.print(getIndent(2));
            String name = getPropertyName(piri);
            String type = getPropertyClassification(piri);
            out.printf("add%s(target, %s.%s, %s);\n", type, getVocabName(), name, name);
        }
        for (IRI piri : revProperties)
        {
            if (isObjectOrCollectionProperty(piri) && !isInverseFunctionalProperty(piri))
            {
                String varName = getReversePropertyName(piri);
                out.printf(getIndent(2) + "target.addAll(%s);\n", varName);
            }
        }
        
        out.println(getIndent(1)+ "}");
    }
    
    protected void generateLoadFromModel(Collection<IRI> properties, PrintWriter out, boolean useFactory)
    {
        out.println(getIndent(1) + "@Override");
        out.printf(getIndent(1) + "public void loadFromModel(Model model, EntityFactory efactory) {\n");
        out.println(getIndent(2) + "super.loadFromModel(model, efactory);");

        if (useFactory)
        {
            out.printf(getIndent(2) + "if (!(efactory instanceof %s))\n", getFactoryName());
            out.printf(getIndent(3) + "throw new IllegalArgumentException(\"factory must be instance of %s\");\n", getFactoryName());
            out.printf(getIndent(2) + "final %s factory = (%s) efactory;\n\n", getFactoryName(), getFactoryName());
        }
        
        out.println(getIndent(2) + "final Model m = model.filter(getIRI(), null, null);");
        
        for (IRI piri : properties)
        {
            String name = getPropertyName(piri);
            String type = getPropertyClassification(piri);
            String dtype = getPropertyDataType(piri);
            if (type.equals("Value") || type.equals("Array")) //values and arrays need type specification in name
            {
                dtype = dtype.replace("[]", "");
                if (dtype.contains("."))
                    dtype = dtype.substring(dtype.lastIndexOf('.') + 1);
                dtype = dtype.substring(0, 1).toUpperCase() + dtype.substring(1);
                out.printf(getIndent(2) + "%s = load%s%s(m, %s.%s);\n", name, dtype, type, getVocabName(), name);
            }
            else if (type.equals("Object"))
            {
                out.printf(getIndent(2) + "//load object %s\n", name);
                out.printf(getIndent(2) + "final Set<IRI> %sIRIs = getObjectIRIs(m, %s.%s);\n", name, getVocabName(), name);
                out.printf(getIndent(2) + "if (!%sIRIs.isEmpty()) {\n", name);
                out.printf(getIndent(3) +     "final IRI iri = %sIRIs.iterator().next();\n", name);
                out.printf(getIndent(3) +     "%s = factory.create%s(iri);\n", name, dtype);
                out.printf(getIndent(3) +     "%s.loadFromModel(m, factory);\n", name);
                out.println(getIndent(2) + "} else {");
                out.printf(getIndent(3) +     "%s = null;\n", name);
                out.println(getIndent(2) + "}");
            }
            else if (type.equals("Collection"))
            {
                dtype = dtype.replace("Set<", "").replace(">", "");
                out.printf(getIndent(2) + "//load collection %s\n", name);
                out.printf(getIndent(2) + "final Set<IRI> %sIRIs = getObjectIRIs(m, %s.%s);\n", name, getVocabName(), name);
                out.printf(getIndent(2) + "%s = new HashSet<>();\n", name);
                out.printf(getIndent(2) + "for (IRI iri : %sIRIs) {\n", name);
                out.printf(getIndent(3) +     "%s item = factory.create%s(iri);\n", dtype, dtype);
                out.printf(getIndent(3) +     "item.loadFromModel(m, factory);\n");
                out.printf(getIndent(3) +     "%s.add(item);\n", name);
                out.println(getIndent(2) + "}");
            }
        }

        out.println(getIndent(1)+ "}");
    }
    
    protected void generateJavadoc(IRI iri, PrintWriter out, int indent)
    {
        //get class properties
        Literal oTitle = getFirstExistingObjectLiteral(model, iri, getPreferredLanguage(), LABEL_PROPERTIES);
        Literal oDescr = getFirstExistingObjectLiteral(model, iri, getPreferredLanguage(), COMMENT_PROPERTIES);
        Set<Value> oSeeAlso = model.filter(iri, RDFS.SEEALSO, null).objects();
        
        //class JavaDoc
        String ii = getIndent(indent);
        out.println(ii + "/**");
        if (oTitle != null) {
            out.printf(ii + " * %s.%n", WordUtils.wrap(oTitle.getLabel().replaceAll("\\s+", " "), 70, "\n" + ii + " * ", false));
            out.println(ii + " * <p>");
        }
        if (oDescr != null) {
            out.printf(ii + " * %s.%n", WordUtils.wrap(oDescr.getLabel().replaceAll("\\s+", " "), 70, "\n" + ii + " * ", false));
            out.println(ii + " * <p>");
        }
        out.printf(ii + " * IRI: {@code <%s>}%n", iri);
        if (!oSeeAlso.isEmpty()) {
            out.println(ii + " *");
            for (Value s : oSeeAlso) {
                if (s instanceof IRI) {
                    out.printf(ii + " * @see <a href=\"%s\">%s</a>%n", s.stringValue(), s.stringValue());
                }
            }
        }
        out.println(ii + " */");
    }
    
    
    //=======================================================================================================
    
    private Set<Resource> findClasses()
    {
        final Set<Resource> classes = new HashSet<>();
        Model types = model.filter(null, RDF.TYPE, null);
        for (Statement st : types)
        {
            if (classPredicates.contains(st.getObject()))
                classes.add(st.getSubject());
        }
        return classes;
    }
    
    private String getClassName(IRI iri)
    {
        return iri.getLocalName();
    }
    
    private Set<IRI> findClassProperties(IRI classIRI)
    {
        return findClassProperties(classIRI, RDFS.DOMAIN);
    }
    
    private Set<IRI> findClassProperties(IRI classIRI, IRI predicate)
    {
        final Set<IRI> ret = new HashSet<>();
        for (IRI pred : PROPERTY_PROPERTIES)
        {
            for (Statement st : model.filter(null, RDF.TYPE, pred))
            {
                if (st.getSubject() instanceof IRI)
                {
                    IRI firi = (IRI) st.getSubject();
                    Set<Value> domains = model.filter(firi, predicate, null).objects();
                    if (domains.contains(classIRI))
                        ret.add(firi);
                }
            }
        }
        return ret;
    }
    
    private String getPropertyName(IRI iri)
    {
        return iri.getLocalName();
    }
    
    private String getReversePropertyName(IRI iri)
    {
        final String propertyType = getPropertySourceType(iri);
        return English.plural(propertyType.substring(0, 1).toLowerCase() + propertyType.substring(1));
    }
    
    private String getPropertyDataType(IRI iri)
    {
        IRI range = getOptionalObjectIRI(model, iri, RDFS.RANGE);
        String type = "String";
        if (range != null)
        {
            if (dataTypes.containsKey(range)) //known data types
            {
                type = dataTypes.get(range);
                if (!isFunctionalProperty(iri))
                    type = type + "[]";
            }
            else if (range.getNamespace().equals(iri.getNamespace())) //local data types -- object properties
            {
                type = getClassName(range);
                if (!isFunctionalProperty(iri))
                    type = "Set<" + type + ">";
            }
        }
        return type;
    }

    private String getPropertySourceType(IRI iri)
    {
        IRI domain = getOptionalObjectIRI(model, iri, RDFS.DOMAIN);
        return domain == null ? null : getClassName(domain);
    }

    private String getPropertyClassification(IRI iri)
    {
        IRI range = getOptionalObjectIRI(model, iri, RDFS.RANGE);
        String type = "Value";
        if (range != null)
        {
            if (dataTypes.containsKey(range)) //known data types
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
    
    private boolean isFunctionalProperty(IRI iri)
    {
        Model m = model.filter(iri, RDF.TYPE, OWL.FUNCTIONALPROPERTY);
        return m.size() != 0;
    }
    
    private boolean isInverseFunctionalProperty(IRI iri)
    {
        Model m = model.filter(iri, RDF.TYPE, OWL.INVERSEFUNCTIONALPROPERTY);
        return m.size() != 0;
    }
    
    private boolean isObjectOrCollectionProperty(IRI piri)
    {
        return getPropertyClassification(piri).equals("Object")
                ||  getPropertyClassification(piri).equals("Collection");
    }
    
    private Literal getFirstExistingObjectLiteral(Model model, Resource subject, String lang, IRI... predicates)
    {
        for (IRI predicate : predicates)
        {
            Literal literal = getOptionalObjectLiteral(model, subject, predicate, lang);
            if (literal != null) { return literal; }
        }
        return null;
    }

    private Literal getOptionalObjectLiteral(Model model, Resource subject, IRI predicate, String lang)
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

    private IRI getOptionalObjectIRI(Model model, Resource subject, IRI predicate)
    {
        Set<Value> objects = model.filter(subject, predicate, null).objects();
        for (Value nextValue : objects)
        {
            if (nextValue instanceof IRI)
                return (IRI) nextValue;
        }
        return null;
    }
    
    private String getIndent(int level) 
    {
        return StringUtils.repeat(getIndent(), level);
    }
}
