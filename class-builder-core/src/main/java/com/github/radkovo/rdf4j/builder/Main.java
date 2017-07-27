/**
 * Main.java
 *
 * Created on 26. 7. 2017, 14:46:28 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

import java.nio.file.Paths;

import org.eclipse.rdf4j.rio.RDFFormat;

import com.github.tkurz.sesame.vocab.VocabBuilder;

/**
 * 
 * @author burgetr
 */
public class Main
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            String filename = "/home/burgetr/git/timeline-analyzer/timeline-analyzer-core/ontology/ta.owl";
            RDFFormat format = null; //auto
            String vocabName = "TA";
            String vocabDir = "/home/burgetr/git/timeline-analyzer/timeline-analyzer-core/src/main/java/cz/vutbr/fit/ta/ontology/vocabulary";
            String vocabPackage = "cz.vutbr.fit.ta.ontology.vocabulary";
            
            String classDir = "/home/burgetr/git/timeline-analyzer/timeline-analyzer-core/src/main/java/cz/vutbr/fit/ta/ontology";
            String classPackage = "cz.vutbr.fit.ta.ontology";
            
            //build vocabulary
            VocabBuilder vb = new VocabBuilder(filename, format);
            vb.setPackageName(vocabPackage);
            vb.generate(Paths.get(vocabDir, vocabName + ".java"));
            
            //build classes
            ClassBuilder cb = new ClassBuilder(filename, format);
            cb.setPackageName(classPackage);
            cb.generate(classDir);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
