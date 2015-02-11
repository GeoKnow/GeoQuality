package org.aksw.geoknow.helper.vocabularies;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class GK {

    public static final String uri = "http://www.geoknow.eu/";
    public static final String prefix = "gk";

    private static Property property(String name) {
        return property(uri, name);
    }

    private static Property property(String namespace, String name) {
        Property result = ResourceFactory.createProperty(namespace + name);
        return result;
    }

    private static final Resource resource(String namespace, String local) {
        return ResourceFactory.createResource(namespace + local);
    }

    private static final Resource resource(String local) {
        return resource(uri, local);
    }

    public static class DIM {

        public static final String uri = "http://www.geoknow.eu/properties/";
        public static final String prefix = "gk-dim";

        public static final Property InstanceType = property(uri, "InstanceType");
        public static final Property Class = property(uri, "Class");
        public static final Property TimeStamp = property(uri, "TimeStamp");
        public static final Property Property = property(uri, "Property");

        public static List<Statement> InstanceTypeStatements = new ArrayList<Statement>();
        public static List<Statement> ClassStatements = new ArrayList<Statement>();
        public static List<Statement> TimeStampStatements = new ArrayList<Statement>();
        public static List<Statement> PropertyStatements = new ArrayList<Statement>();

        static {
            InstanceTypeStatements.add(ResourceFactory.createStatement(InstanceType, RDF.type, QB.DimensionProperty));
            ClassStatements.add(ResourceFactory.createStatement(Class, RDF.type, QB.DimensionProperty));
            TimeStampStatements.add(ResourceFactory.createStatement(TimeStamp, RDF.type, QB.DimensionProperty));
            PropertyStatements.add(ResourceFactory.createStatement(Property, RDF.type, QB.DimensionProperty));
            InstanceTypeStatements.add(ResourceFactory.createStatement(InstanceType, RDFS.label,
                    ResourceFactory.createLangLiteral("Normal or Outlier instance", "en")));
            ClassStatements.add(ResourceFactory.createStatement(Class, RDFS.label,
                    ResourceFactory.createLangLiteral("class of instance", "en")));
            TimeStampStatements.add(ResourceFactory.createStatement(TimeStamp, RDFS.label,
                    ResourceFactory.createLangLiteral("Time Stamp", "en")));
            PropertyStatements.add(ResourceFactory.createStatement(Property, RDFS.label,
                    ResourceFactory.createLangLiteral("Property name", "en")));
        }

    }

    public static class MEASURE {
        public static final String uri = "http://www.geoknow.eu/properties/measure/";
        public static final String prefix = "gk-measure";

        public static final Property InstanceCount = property(uri, "InstanceCount");

        public static List<Statement> InstanceCountStatements;

        static {
            InstanceCountStatements = new ArrayList<Statement>(4);
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDF.type, QB.MeasureProperty));
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.label,
                    ResourceFactory.createLangLiteral("Instance Count", "en")));
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.subPropertyOf,
                    SDMX.MEASURE.obs));
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.range, XSD.integer));
        }
    }

}
