package org.aksw.geoknow.assessment.count;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 *
 * This metric calculates the number of instances from other classes per classes.
 *
 *
 * @author Didier Cherix
 *         </br>
 *         R & D, Unister GmbH, Leipzig, Germany</br>
 *         This code is a part of the <a href="http://geoknow.eu/Welcome.html">GeoKnow</a> project.
 *
 */
public class InstancesOfOtherClassesNumberMetric implements GeoQualityMetric {

    private static final String INSTANCE_CLASS = "Select distinct  ?class WHERE {?instance a ?class .}";
    private static final ParameterizedSparqlString OTHER_CLASSES = new ParameterizedSparqlString(
            "SELECT (COUNT (DISTINCT ?instance) as ?count) WHERE { ?instance a ?class . ?instance a ?originClass . FILTER(!(?class = ?originClass))}");

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "iooc";

    public static void main(String[] args) throws IOException {
        OntModel m = ModelFactory.createOntologyModel();
        m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/", "TTL");
        GeoQualityMetric metric = new InstancesOfOtherClassesNumberMetric();
        Model r = metric.generateResultsDataCube(m);
        r.write(new FileWriter("nuts.ttl"), "TTL");
    }

    private List<String> defaultGraphs = null;

    public InstancesOfOtherClassesNumberMetric() {
        super();
    }

    public InstancesOfOtherClassesNumberMetric(List<String> defaultGraphs) {
        super();
        this.defaultGraphs = defaultGraphs;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();

        Resource structure = cubeData.createResource(STRUCTURE, QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(STRUCTURE + "/c1", QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(STRUCTURE + "/c2", QB.ComponentSpecification);
        c2.addProperty(QB.measure, GK.MEASURE.OtherClassesCount);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Number of Other Classes", "en"));

        Resource c3 = cubeData.createResource(STRUCTURE + "/c3", QB.ComponentSpecification);
        c3.addProperty(QB.dimension, GK.DIM.TimeStamp);
        c3.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Timestamp", "en"));

        structure.addProperty(QB.component, c1);
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.DIM.ClassStatements);
        cubeData.add(GK.MEASURE.OtherClassesCountStatements);
        cubeData.add(GK.DIM.TimeStampStatements);

        return cubeData;
    }

    private Model execute(Model inputModel, String endpoint) {
        Model cubeData = createModel();
        Resource dataset;
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        dataset = cubeData.createResource(GK.uri + "Instance_of_other_classes_Count", QB.Dataset);
        dataset.addLiteral(RDFS.comment, "Number of instances of other classes for a class");
        dataset.addLiteral(DCTerms.date, cubeData.createTypedLiteral(calendar));
        dataset.addLiteral(DCTerms.publisher, "R & D, Unister GmbH, Geoknow");
        dataset.addProperty(QB.structure, STRUCTURE);
        if (endpoint != null) {
            dataset.addProperty(DCTerms.source, endpoint);
        }

        QueryExecution qexec;
        if (inputModel != null) {
            qexec = QueryExecutionFactory.create(INSTANCE_CLASS, inputModel);
        } else {
            qexec = QueryExecutionFactory.sparqlService(endpoint, INSTANCE_CLASS, defaultGraphs, defaultGraphs);
        }
        QuerySolution solution = null;
        int i = 0;
        for (ResultSet result = qexec.execSelect(); result.hasNext(); i++) {
            solution = result.next();
            Resource originClass = solution.getResource("class");
            System.out.println(originClass);
            OTHER_CLASSES.setIri("originClass", originClass.getURI());
            QueryExecution execCount;
            if (inputModel != null) {
                execCount = QueryExecutionFactory.create(OTHER_CLASSES.asQuery(), inputModel);
            } else {
                execCount = QueryExecutionFactory.sparqlService(endpoint, OTHER_CLASSES.asQuery(), defaultGraphs,
                        defaultGraphs);
            }
            ResultSet resultCount = execCount.execSelect();
            if (resultCount.hasNext()) {
                Resource obs = cubeData.createResource("http://www.geoknow.eu/data-cube/metric4/observation" + i,
                        QB.Observation);
                obs.addProperty(GK.DIM.Class, originClass);
                obs.addProperty(GK.MEASURE.OtherClassesCount, resultCount.next().getLiteral("count"));
                obs.addProperty(QB.dataset, dataset);
                obs.addProperty(GK.DIM.TimeStamp, cubeData.createTypedLiteral(calendar));
            }
        }
        return cubeData;
    }

    public Model generateResultsDataCube(Model inputModel) {
        return execute(inputModel, null);
    }

    public Model generateResultsDataCube(String endpointUrl) {
        return this.execute(null, endpointUrl);
    }

}
