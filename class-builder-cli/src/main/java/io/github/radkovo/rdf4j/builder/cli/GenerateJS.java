/**
 * GenerateJS.java
 *
 * Created on 20. 12. 2020, 11:18:14 by burgetr
 */
package io.github.radkovo.rdf4j.builder.cli;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import io.github.radkovo.rdf4j.builder.js.JSMappingBuilder;
import io.github.radkovo.rdf4j.vocab.GenerationException;

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
        try {
            CommandLineParser parser = new PosixParser();
            CommandLine cli = parser.parse(getCliOpts(), args);

            if (cli.hasOption('h')) {
                printHelp();
                return;
            }

            String[] cliArgs = cli.getArgs();
            if (cliArgs.length == 0)
                throw new ParseException("Missing input file(s)");
            
            final String cwd = System.getProperty("user.dir");
            
            RDFFormat format = Rio.getParserFormatForMIMEType(cli.getOptionValue('f', null)).orElse(null);
            String vocabName = cli.getOptionValue('v');
            String classDir = cli.hasOption('O') ? cli.getOptionValue('O') : cwd;
            String includePrefix = cli.hasOption('I') ? cli.getOptionValue('I') : "";
            
            generateFromOWL(cliArgs, format, vocabName, classDir, includePrefix);
            
        } catch (MissingOptionException e) {
            printHelp("Missing option: " + e.getMessage());
        } catch (ParseException e) {
            printHelp(e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O exception: " + e.getMessage());
        } catch (GenerationException e) {
            e.printStackTrace();
        }

    }

    private static void generateFromOWL(String[] filenames, RDFFormat format,
            String vocabName, String classDir, String includePrefix)
            throws IOException, GenerationException
    {
        //build JS mappings
        JSMappingBuilder gen = new JSMappingBuilder();
        for (String filename : filenames)
            gen.load(filename, format);
        gen.setVocabName(vocabName);
        gen.generate(classDir);
    }
    
    private static void printHelp() 
    {
        printHelp(null);
    }

    private static void printHelp(String error) 
    {
        HelpFormatter hf = new HelpFormatter();
        PrintWriter w = new PrintWriter(System.out);
        if (error != null) {
            hf.printWrapped(w, 80, error);
            w.println();
        }
        hf.printWrapped(w, 80, 12, "Usage: GenerateJS [options...] <input-file> [<input-file> ...]");
        hf.printWrapped(w, 80, 42, "  <input-file>                  the input file to read from (multiple input files are allowed)");
        hf.printOptions(w, 80, getCliOpts(), 2, 2);
        w.flush();
        w.close();
    }

    @SuppressWarnings({"static-access"})
    private static Options getCliOpts() 
    {
        Options o = new Options();

        o.addOption(OptionBuilder
                .withLongOpt("format")
                .withDescription("mime-type of the input file (will try to guess if absent)")
                .hasArgs(1)
                .withArgName("format")
                .isRequired(false)
                .create('f'));

        o.addOption(OptionBuilder
                .withLongOpt("vocab-name")
                .withDescription("vocabulary class name")
                .hasArgs(1)
                .withArgName("class-name")
                .isRequired(true)
                .create('v'));

        o.addOption(OptionBuilder
                .withLongOpt("class-dir")
                .withDescription("the output directory for the classes")
                .hasArgs(1)
                .withArgName("path")
                .isRequired(false)
                .create('O'));

        o.addOption(OptionBuilder
                .withLongOpt("include-prefix")
                .withDescription("class IRI prefix to include (classes with other prefixes will be excluded)")
                .hasArgs(1)
                .withArgName("path")
                .isRequired(false)
                .create('I'));

        o.addOption(OptionBuilder
                .withLongOpt("help")
                .withDescription("print this help")
                .isRequired(false)
                .hasArg(false)
                .create('h'));
        
        return o;
    }
    
}
