# RDF4J Class Builder
(c) 2017-2019 Radek Burget (burgetr@fit.vutbr.cz)

RDF4J Class Builder is a tool for generating Java classes from OWL ontology definitions. For every class defined in the OWL file, it generates a Java class definition, that contains 

- Property definitions, getters and setters for every OWL property that has the given class in `owl:domain`. The property cardinality is distinguished using the `owl:Property`, `owl:FunctionalProperty` and `owl:InverseFunctionalProperty` definitions.
- The `addToModel()` and `loadFromModel()` methods that allow loading  and storing the class instatnces from and to a RDF graph represented by the RDF4J `Model` class.

Additionaly, a factory interface is generated for the whole ontology that allows implementing custom factories for creating the object instances.   

The project is inspired by and integrates with the the [RDF4J Vocabulary Builder](https://github.com/radkovo/rdf4j-vocab-builder) project.

## Installation

RDF4J Class Builder is distributed as platform-independent runnable jar archive:

- Download the runnable archive `ClassBuilder.jar` from [Releases](https://github.com/radkovo/rdf4j-class-builder/releases)
- Run the following command in the command prompt (depending on your operating system):\
  `java -jar ClassBuilder.jar`

You should see a list of available parametres.

## Usage

RDF4J Class Builder takes an OWL definition file in any format (RDF/XML, Turtle, ...) as the input. It generates two separate components:

- A single *vocabulary class* (using the built-in [RDF4J Vocabulary Builder](https://github.com/radkovo/rdf4j-vocab-builder)). The vocabulary class defines the URI constants for all the classes and predicates defined in the OWL source.
- Individual java *class definitions* (as described above) for all the classes defined in the OWL source.

For both the vocabulary and the class definitions, their java package and the target folder may be specified via the command line as follows:

```
ClassBuilder [options...] <input-file>
  <input-file>                  the input file to read from
  -f,--format <format>          mime-type of the input file (will try to guess
                                if absent)
  -v,--vocab-name <class-name>  vocabulary class name
  -o,--vocab-dir <path>         the output directory for the vocabulary (current
                                directory when absent)
  -O,--class-dir <path>         the output directory for the classes (vocabulary
                                directory when absent)
  -p,--vocab-package <package>  vocabulary package declaration (will use default
                                (empty) package if absent)
  -P,--class-package <package>  class package declaration (will use the
                                vocabulary package if absent)
```

Example usage:

```shell
java -jar ClassBuilder.jar \
	info.owl \
	-v INFO \
	-o src/com/example/vocabulary \
	-p com.example.vocabulary \
	-O src/com/example/ontology \
	-P com.example.ontology \
```

## Building

RDF4J Class Builder may be build from the sources by maven. After cloning the source repository, use `mvn package` for building and packaging all the components.

## Acknowledgements

*This work was supported by the Ministry of the Interior of the Czech Republic as a part of the project Integrated platform for analysis of digital data from security incidents VI20172020062.*
