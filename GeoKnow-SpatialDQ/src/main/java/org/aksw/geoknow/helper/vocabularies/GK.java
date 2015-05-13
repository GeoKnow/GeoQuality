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
        public static final Property Instance = property(uri, "Instance");

        public static List<Statement> InstanceTypeStatements = new ArrayList<Statement>();
        public static List<Statement> ClassStatements = new ArrayList<Statement>();
        public static List<Statement> TimeStampStatements = new ArrayList<Statement>();
        public static List<Statement> PropertyStatements = new ArrayList<Statement>();
        public static List<Statement> InstanceStatements = new ArrayList<Statement>();

        static {
            InstanceTypeStatements.add(ResourceFactory.createStatement(InstanceType, RDF.type, QB.DimensionProperty));
            ClassStatements.add(ResourceFactory.createStatement(Class, RDF.type, QB.DimensionProperty));
            TimeStampStatements.add(ResourceFactory.createStatement(TimeStamp, RDF.type, QB.DimensionProperty));
            PropertyStatements.add(ResourceFactory.createStatement(Property, RDF.type, QB.DimensionProperty));
            InstanceStatements.add(ResourceFactory.createStatement(Property, RDF.type, QB.DimensionProperty));

            InstanceTypeStatements.add(ResourceFactory.createStatement(InstanceType, RDFS.label,
                    ResourceFactory.createLangLiteral("Normal or Outlier instance", "en")));
            ClassStatements.add(ResourceFactory.createStatement(Class, RDFS.label,
                    ResourceFactory.createLangLiteral("class of instance", "en")));
            TimeStampStatements.add(ResourceFactory.createStatement(TimeStamp, RDFS.label,
                    ResourceFactory.createLangLiteral("Time Stamp", "en")));
            PropertyStatements.add(ResourceFactory.createStatement(Property, RDFS.label,
                    ResourceFactory.createLangLiteral("Property name", "en")));
            InstanceStatements.add(ResourceFactory.createStatement(Property, RDFS.label,
                    ResourceFactory.createLangLiteral("Instance name", "en")));
        }

    }

    public static class MEASURE {
        public static final String uri = "http://www.geoknow.eu/properties/measure/";
        public static final String prefix = "gk-measure";

        public static final Property InstanceCount = property(uri, "InstanceCount");
        public static final Property Average = property(uri, "Average");
        public static final Property PropertyCount = property(uri, "PropertyCount");
        public static final Property OtherClassesCount = property(uri, "OtherClasses");

        public static List<Statement> InstanceCountStatements;
        public static List<Statement> AverageStatements;
        public static List<Statement> PropertyCountStatements;
        public static List<Statement> OtherClassesCountStatements;

        static {
            InstanceCountStatements = new ArrayList<Statement>(4);
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDF.type, QB.MeasureProperty));
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.label,
                    ResourceFactory.createLangLiteral("Instance Count", "en")));
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.subPropertyOf,
                    SDMX.MEASURE.obs));
            InstanceCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.range, XSD.integer));

            AverageStatements = new ArrayList<Statement>(4);
            AverageStatements.add(ResourceFactory.createStatement(Average, RDF.type, QB.MeasureProperty));
            AverageStatements.add(ResourceFactory.createStatement(Average, RDFS.label,
                    ResourceFactory.createLangLiteral("Average", "en")));
            AverageStatements.add(ResourceFactory.createStatement(Average, RDFS.subPropertyOf,
                    SDMX.MEASURE.obs));
            AverageStatements.add(ResourceFactory.createStatement(Average, RDFS.range, XSD.decimal));

            PropertyCountStatements = new ArrayList<Statement>(4);
            PropertyCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDF.type, QB.MeasureProperty));
            PropertyCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.label,
                    ResourceFactory.createLangLiteral("Property Count", "en")));
            PropertyCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.subPropertyOf,
                    SDMX.MEASURE.obs));
            PropertyCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.range, XSD.integer));

            OtherClassesCountStatements = new ArrayList<Statement>(4);
            OtherClassesCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDF.type, QB.MeasureProperty));
            OtherClassesCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.label,
                    ResourceFactory.createLangLiteral("Other Classes Count", "en")));
            OtherClassesCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.subPropertyOf,
                    SDMX.MEASURE.obs));
            OtherClassesCountStatements.add(ResourceFactory.createStatement(InstanceCount, RDFS.range, XSD.integer));
        }
    }

}
