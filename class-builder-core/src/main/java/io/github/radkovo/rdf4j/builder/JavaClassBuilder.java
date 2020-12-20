/**
 * ClassBuilder.java
 *
 * Created on 26. 7. 2017, 13:44:33 by burgetr
 */
package io.github.radkovo.rdf4j.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.WordUtils;
import org.atteo.evo.inflector.English;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Java class builder. It generates a set of Java
 * files based on the generation parametres.
 * 
 * @author burgetr
 */
public class JavaClassBuilder extends ClassBuilder
{
    private static final Logger log = LoggerFactory.getLogger(JavaClassBuilder.class);

    private static final String DEFAULT_SUPERCLASS = "com.github.radkovo.rdf4j.builder.RDFEntity";
    
    private static final Map<IRI, String> javaDataTypes;
    static {
        javaDataTypes = new HashMap<>();
        javaDataTypes.put(XMLSchema.BOOLEAN, "boolean");
        javaDataTypes.put(XMLSchema.BYTE, "byte");
        javaDataTypes.put(XMLSchema.DATE, "java.util.Date");
        javaDataTypes.put(XMLSchema.DATETIME, "java.util.Date");
        javaDataTypes.put(XMLSchema.DECIMAL, "float");
        javaDataTypes.put(XMLSchema.DOUBLE, "double");
        javaDataTypes.put(XMLSchema.FLOAT, "float");
        javaDataTypes.put(XMLSchema.INT, "int");
        javaDataTypes.put(XMLSchema.INTEGER, "int");
        javaDataTypes.put(XMLSchema.LONG, "long");
        javaDataTypes.put(XMLSchema.POSITIVE_INTEGER, "int");
        javaDataTypes.put(XMLSchema.SHORT, "short");
        javaDataTypes.put(XMLSchema.STRING, "String");
        javaDataTypes.put(XMLSchema.TIME, "java.util.Date");
        javaDataTypes.put(XMLSchema.ANYURI, "java.net.URL");
    }
    
    //generation parametres
    private String packageName = null;
    private String vocabPackageName = null;
    private String vocabName = null;
    
    
    public JavaClassBuilder(String filename, String format) throws IOException, RDFParseException
    {
        super(filename, format);
    }
    
    public JavaClassBuilder(String filename, RDFFormat format) throws IOException, RDFParseException
    {
        super(filename, format);
    }

    @Override
    protected Map<IRI, String> getDataTypes()
    {
        return javaDataTypes;
    }

    /**
     * Returns the target Java package name for generated classes.
     * @return The target package name or {@code null} when not specified.
     */
    public String getPackageName()
    {
        return packageName;
    }

    /**
     * Sets the target Java package name for generated classes.
     * @param packageName the package name
     */
    public void setPackageName(String packageName)
    {
        this.packageName = packageName;
    }

    /**
     * Returns the target Java package name for generated vocabularies.
     * @return The target package name or {@code null} when not specified.
     */
    public String getVocabPackageName()
    {
        return vocabPackageName;
    }

    /**
     * Sets the target Java package name for generated vocabularies.
     * @param packageName the package name
     */
    public void setVocabPackageName(String vocabPackageName)
    {
        this.vocabPackageName = vocabPackageName;
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

    //=======================================================================================================
    
    @Override
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
    
    /**
     * Generates the factory interface.
     * 
     * @param classes
     * @param outputDir
     * @throws IOException
     */
    public void generateFactory(Set<Resource> classes, Path outputDir) throws IOException
    {
        String fname = getFactoryName();
        File outfile = new File(outputDir.toFile(), fname + ".java");
        PrintWriter out = new PrintWriter(outfile);
        generateFactory(classes, fname, out);
        out.close();
    }
    
    /**
     * Generates the factory interface.
     * 
     * @param classes
     * @param fname
     * @param out
     */
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
    
    /**
     * Generates a single class.
     * 
     * @param cres class resource
     * @param outputDir output directory path
     * @throws IOException
     */
    public void generateClass(IRI cres, Path outputDir) throws IOException
    {
        String className = getClassName(cres);
        File outfile = new File(outputDir.toFile(), className + ".java");
        PrintWriter out = new PrintWriter(outfile);
        generateClass(cres, className, out);
        out.close();
    }
    
    /**
     * Generates a single class.
     * 
     * @param iri class IRI
     * @param className class name
     * @param out writer used for output
     */
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
        IRI superClassIRI = getOptionalObjectIRI(getModel(), iri, RDFS.SUBCLASSOF);
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
        Literal oTitle = getResourceTitle(iri);
        Literal oDescr = getResourceDescription(iri);
        Set<Value> oSeeAlso = getModel().filter(iri, RDFS.SEEALSO, null).objects();
        
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
    
    
}
