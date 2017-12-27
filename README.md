# rdf4j-class-builder
RDF4J Class Builder is a tool for generating Java classes from OWL ontology definitions. For every class defined in the OWL file, it generates a Java class definition, that contains 

- Property definitions, getters and setters for every OWL property that has the given class in `owl:domain`. The property cardinality is distinguished using the `owl:Property`, `owl:FunctionalProperty` and `owl:InverseFunctionalProperty` definitions.
- The `addToModel()` and `loadFromModel()` methods that allow loading  and storing the class instatnces from and to a RDF graph represented by the RDF4J `Model` class.

Additionaly, a factory interface is generated for the whole ontology that allows implementing custom factories for creating the object instances.   

The project is inspired by and integrates with the the [RDF4J Vocabulary Builder](https://github.com/radkovo/rdf4j-vocab-builder) project.
