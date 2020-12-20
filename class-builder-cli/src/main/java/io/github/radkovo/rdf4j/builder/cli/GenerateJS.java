/**
 * GenerateJS.java
 *
 * Created on 20. 12. 2020, 11:18:14 by burgetr
 */
package io.github.radkovo.rdf4j.builder.cli;

import java.io.IOException;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;

import io.github.radkovo.rdf4j.builder.js.JSMappingBuilder;

/**
 * 
 * @author burgetr
 */
public class GenerateJS
{

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        RDFFormat format = null;
        try
        {
            JSMappingBuilder gen = new JSMappingBuilder("/home/burgetr/git/fitlayout/FitLayout.github.io/ontology/render.owl", format);
            gen.setVocabName("box");
            gen.generate("/home/burgetr/git/fitlayout/PageView/src/common/");
            
        } catch (RDFParseException | IOException e) {
            e.printStackTrace();
        }
    }

}
