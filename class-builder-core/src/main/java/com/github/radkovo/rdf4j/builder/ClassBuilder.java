/**
 * ClassBuilder.java
 *
 * Created on 26. 7. 2017, 13:44:33 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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
 * The main class builder.
 * 
 * @author burgetr
 */
public class ClassBuilder
{
    private static final Logger log = LoggerFactory.getLogger(ClassBuilder.class);

    private static final IRI[] COMMENT_PROPERTIES = new IRI[]{RDFS.COMMENT, DCTERMS.DESCRIPTION, SKOS.DEFINITION, DC.DESCRIPTION};
    private static final IRI[] LABEL_PROPERTIES = new IRI[]{RDFS.LABEL, DCTERMS.TITLE, DC.TITLE, SKOS.PREF_LABEL, SKOS.ALT_LABEL};
    private static final Set<IRI> classPredicates;
    static {
        classPredicates = new HashSet<>();
        classPredicates.add(RDFS.CLASS);
        classPredicates.add(OWL.CLASS);
    }
    
    private String name = null;
    private String prefix = null;
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

        //import
        Set<Resource> owlOntologies = model.filter(null, RDF.TYPE, OWL.ONTOLOGY).subjects();
        if (!owlOntologies.isEmpty()) {
            setPrefix(owlOntologies.iterator().next().stringValue());
        }
        
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
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
        Set<Resource> classes = new HashSet<>();
        Model types = model.filter(null, RDF.TYPE, null);
        for (Statement st : types)
        {
            if (classPredicates.contains(st.getObject()))
                classes.add(st.getSubject());
        }
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
    
    public void generateClass(IRI cres, Path outputDir)
    {
        
    }
    
    public void generateClass(IRI cres, Path outputDir, PrintWriter out)
    {
        
    }
    
}
