/**
 * ClassBuilder.java
 *
 * Created on 26. 7. 2017, 13:44:33 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
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
 * The main class builder.
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
    }
    
    private String packageName = null;
    private String indent = "\t";
    private String language = null;
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
        
    }

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
        
        //generate package
        if (getPackageName() != null)
            out.printf("package %s;\n\n", getPackageName());

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
        out.printf(getIndent(1) + "public static final IRI = vf.createIRI(\"%s\");\n\n", iri);
        
        //generate properties
        Set<IRI> properties = findClassProperties(iri);
        log.debug("   properties: {}", properties);
        for (IRI piri : properties)
            generatePropertyDeclaration(piri, getPropertyName(piri), out);
        out.println();
        for (IRI piri : properties)
        {
            generatePropertyGetter(piri, getPropertyName(piri), out);
            out.println();
            generatePropertySetter(piri, getPropertyName(piri), out);
            out.println();
        }
        
        //finish class definition
        out.println("}");
    }
    
    protected void generatePropertyDeclaration(IRI iri, String propertyName, PrintWriter out)
    {
        generateJavadoc(iri, out, 1);
        String type = getPropertyDataType(iri);
        out.printf(getIndent(1) + "private %s %s;\n", type, propertyName);
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
        final Set<IRI> ret = new HashSet<>();
        for (IRI pred : PROPERTY_PROPERTIES)
        {
            for (Statement st : model.filter(null, RDF.TYPE, pred))
            {
                if (st.getSubject() instanceof IRI)
                {
                    IRI firi = (IRI) st.getSubject();
                    Set<Value> domains = model.filter(firi, RDFS.DOMAIN, null).objects();
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
    
    private String getPropertyDataType(IRI iri)
    {
        IRI range = getOptionalObjectIRI(model, iri, RDFS.RANGE);
        String type = "String";
        if (range != null)
        {
            if (dataTypes.containsKey(range)) //known data types
                type = dataTypes.get(range);
            else if (range.getNamespace().equals(iri.getNamespace())) //local data types
                type = range.getLocalName();
        }
        return type;
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
