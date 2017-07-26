/**
 * Main.java
 *
 * Created on 26. 7. 2017, 14:46:28 by burgetr
 */
package com.github.radkovo.rdf4j.builder;

import org.eclipse.rdf4j.rio.RDFFormat;

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
            RDFFormat format = null;
            ClassBuilder cb = new ClassBuilder("/home/burgetr/git/timeline-analyzer/timeline-analyzer-core/ontology/ta.owl", format);
            
            cb.setPackageName("com.test");
            
            cb.generate("/tmp");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
