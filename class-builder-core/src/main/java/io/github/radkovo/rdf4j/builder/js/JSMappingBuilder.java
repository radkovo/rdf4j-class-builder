/**
 * JSMappingBuilder.java
 *
 * Created on 20. 12. 2020, 10:23:33 by burgetr
 */
package io.github.radkovo.rdf4j.builder.js;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.radkovo.rdf4j.builder.ClassBuilder;

/**
 * 
 * @author burgetr
 */
public class JSMappingBuilder extends ClassBuilder
{
    private static final Logger log = LoggerFactory.getLogger(JSMappingBuilder.class);

    private static final String DEFAULT_SUPERCLASS = "Object";
    
    private static final Map<IRI, String> jsDataTypes;
    static {
        jsDataTypes = new HashMap<>();
        jsDataTypes.put(XMLSchema.BOOLEAN, "boolean");
        jsDataTypes.put(XMLSchema.BYTE, "int");
        jsDataTypes.put(XMLSchema.DATE, "date");
        jsDataTypes.put(XMLSchema.DATETIME, "date");
        jsDataTypes.put(XMLSchema.DECIMAL, "float");
        jsDataTypes.put(XMLSchema.DOUBLE, "float");
        jsDataTypes.put(XMLSchema.FLOAT, "float");
        jsDataTypes.put(XMLSchema.INT, "int");
        jsDataTypes.put(XMLSchema.INTEGER, "int");
        jsDataTypes.put(XMLSchema.LONG, "int");
        jsDataTypes.put(XMLSchema.POSITIVE_INTEGER, "int");
        jsDataTypes.put(XMLSchema.SHORT, "int");
        jsDataTypes.put(XMLSchema.STRING, "string");
        jsDataTypes.put(XMLSchema.TIME, "date");
        jsDataTypes.put(XMLSchema.ANYURI, "string");
    }
    
    
    public JSMappingBuilder()
    {
        super();
    }

    public JSMappingBuilder(String filename, String format) throws IOException, RDFParseException
    {
        super(filename, format);
    }

    public JSMappingBuilder(String filename, RDFFormat format) throws IOException, RDFParseException
    {
        super(filename, format);
    }

    @Override
    protected Map<IRI, String> getDataTypes()
    {
        return jsDataTypes;
    }

    @Override
    protected String getDefaultType()
    {
        return "object";
    }

    @Override
    protected String getObjectType(IRI iri)
    {
        return "object<" + iri.toString() + ">";
    }

    @Override
    protected String getArrayType(String type)
    {
        return type + "[]";
    }

    @Override
    protected String getCollectionType(String type)
    {
        return type + "[]";
    }

    @Override
    public void generate(Path outputDir) throws IOException
    {
        if (!Files.isDirectory(outputDir))
            throw new FileNotFoundException(outputDir.toString());
        
        //find all classes in the model
        List<IRI> classes = sortClasses(findClasses());
        log.info("Found clases: {}", classes);
        
        generateMappers(classes, outputDir);
    }
    
    private void generateMappers(List<IRI> classes, Path outputDir) throws IOException
    {
        File outfile = new File(outputDir.toFile(), getVocabName() + "Mappers.js");
        PrintWriter out = new PrintWriter(outfile);
        
        //imports
        out.println();
        out.println("import ObjectCreator from './objectcreator.js';");
        out.println();
        
        //generate mappers
        for (IRI cres : classes)
        {
            generateMapper((IRI) cres, out);
        }
        
        //creator registry
        generateRegistry(classes, out);
        
        out.close();
    }

    private void generateRegistry(List<IRI> classes, PrintWriter out)
    {
        out.println("export const registry = {");
        
        boolean first = true;
        for (IRI cres : classes) {
            if (!first)
                out.println(',');
            out.printf(getIndent(1) + "'%s': new %sCreator()", cres.toString(), getClassName(cres));
            first = false;
        }
        out.println();
        out.println("};");
        out.println();
    }
    
    private void generateMapper(IRI iri, PrintWriter out)
    {
        final String className = getClassName(iri);
        Set<IRI> properties = findClassProperties(iri);
        log.debug("   properties: {}", properties);
        
        //super class
        String superClass = DEFAULT_SUPERCLASS;
        final IRI superClassIRI = getOptionalObjectIRI(getModel(), iri, RDFS.SUBCLASSOF);
        if (superClassIRI != null)
        {
            superClass = getClassName(superClassIRI);
        }
        //class definition
        out.printf("class %sCreator extends %sCreator {\n", className, superClass);
        
        //constructor with external mappings
        out.printf(getIndent(1) + "constructor() {\n");
        out.printf(getIndent(2) + "super();\n");
        
        //own mappings
        out.printf(getIndent(2) + "this.addMapping({\n");
        for (IRI piri : properties)
        {
            /*if (!isObjectOrCollectionProperty(piri))
            {
                generatePropertyMapping(piri, getPropertyName(piri), out);
            }*/
            generatePropertyMapping(piri, getPropertyName(piri), out);
        }
        out.printf(getIndent(2) + "});\n");
        //end constructor
        out.printf(getIndent(1) + "}\n");
        
        out.println("}\n");
    }
    
    private void generatePropertyMapping(IRI iri, String propertyName, PrintWriter out)
    {
        String type = getPropertyDataType(iri);
        out.printf(getIndent(3) + "%s: { name: '%s', type: '%s' },\n", propertyName, iri.toString(), type);
    }
    
    private List<IRI> sortClasses(Set<Resource> classes)
    {
        List<IRI> ret = new ArrayList<>();
        for (Resource cres : classes)
        {
            if (cres instanceof IRI)
            {
                ret.add((IRI) cres);
            }
            else
            {
                log.warn("Skipping resource {} -- not an IRI", cres);
            }
        }
        Collections.sort(ret, new SuperclassComparator()); //move superclasses first
        return ret;
    }

    class SuperclassComparator implements Comparator<IRI>
    {
        @Override
        public int compare(IRI c1, IRI c2)
        {
            final IRI superC1 = getOptionalObjectIRI(getModel(), c1, RDFS.SUBCLASSOF);
            final IRI superC2 = getOptionalObjectIRI(getModel(), c2, RDFS.SUBCLASSOF);
            if (superC1 == null && superC2 != null) 
            {
                return -1; //c1 has no superclass, c2 has one
            }
            else if (superC1 != null && superC2 == null) 
            {
                return 1; //c2 has no superclass, c1 has one
            }
            else if (c2.equals(superC1)) 
            { 
                return 1; //c2 is superclass of c1
            } 
            else if (c1.equals(superC2))
            { 
                return -1; //c1 is superclass of c2
            } 
            else 
            {
                return c1.getLocalName().compareTo(c2.getLocalName()); //use lexical order
            }
        }
    }
    
}
