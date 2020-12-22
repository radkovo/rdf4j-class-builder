/**
 * Main.java
 *
 * Created on 17. 10. 2019, 20:26:28 by burgetr
 */
package io.github.radkovo.rdf4j.builder.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

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

import io.github.radkovo.rdf4j.builder.JavaClassBuilder;
import io.github.radkovo.rdf4j.vocab.GenerationException;
import io.github.radkovo.rdf4j.vocab.VocabBuilder;


/**
 * The command line interface class.
 * 
 * @author burgetr
 */
public class Main
{
    
    /**
     * Command line processing method.
     * 
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
            String vocabDir = cli.hasOption('o') ? cli.getOptionValue('o') : cwd;
            String vocabPackage = cli.hasOption('p') ? cli.getOptionValue('p') : "";
            String classDir = cli.hasOption('O') ? cli.getOptionValue('O') : vocabDir;
            String classPackage = cli.hasOption('P') ? cli.getOptionValue('P') : vocabPackage;
            
            generateFromOWL(cliArgs, format, vocabName, vocabDir, vocabPackage, classDir, classPackage);
            
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
            String vocabName, String vocabDir, String vocabPackage,
            String classDir, String classPackage)
            throws IOException, GenerationException
    {
        //build vocabularies
        for (String filename : filenames)
        {
            VocabBuilder vb = new VocabBuilder(filename, format);
            vb.setPackageName(vocabPackage);
            vb.generate(Paths.get(vocabDir, vocabName + ".java"));
        }
        
        //build classes
        JavaClassBuilder cb = new JavaClassBuilder();
        for (String filename : filenames)
            cb.load(filename, format);
        cb.setPackageName(classPackage);
        cb.setVocabPackageName(vocabPackage);
        cb.setVocabName(vocabName);
        cb.generate(classDir);
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
        hf.printWrapped(w, 80, 12, "Usage: ClassBuilder [options...] <input-file>");
        hf.printWrapped(w, 80, 42, "  <input-file>                  the input file to read from");
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
                .withLongOpt("vocab-package")
                .withDescription("vocabulary package declaration (will use default (empty) package if absent)")
                .hasArgs(1)
                .withArgName("package")
                .isRequired(false)
                .create('p'));

        o.addOption(OptionBuilder
                .withLongOpt("class-package")
                .withDescription("class package declaration (will use the vocabulary package if absent)")
                .hasArgs(1)
                .withArgName("package")
                .isRequired(false)
                .create('P'));

        o.addOption(OptionBuilder
                .withLongOpt("vocab-dir")
                .withDescription("the output directory for the vocabulary (current directory when absent)")
                .hasArgs(1)
                .withArgName("path")
                .isRequired(false)
                .create('o'));

        o.addOption(OptionBuilder
                .withLongOpt("class-dir")
                .withDescription("the output directory for the classes (vocabulary directory when absent)")
                .hasArgs(1)
                .withArgName("path")
                .isRequired(false)
                .create('O'));

        o.addOption(OptionBuilder
                .withLongOpt("help")
                .withDescription("print this help")
                .isRequired(false)
                .hasArg(false)
                .create('h'));
        
        return o;
    }
}
