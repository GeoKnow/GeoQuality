package org.aksw.geoknow.assessment.count;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * This metric return the number of instance for each class in the data set.
 *
 * @author Didier Cherix
 *         </br> R & D, Unister GmbH, Leipzig, Germany</br>
 *         This code is a part of the <a href="http://geoknow.eu/Welcome.html">GeoKnow</a> project.
 *
 */
public class InstancesNumberMetric implements GeoQualityMetric {

    private static final String GET_CLASSES = "SELECT ?class (count(distinct ?s) as ?count) WHERE {?s a ?class .} group by ?class";

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "structure/metric1";

    public Model generateResultsDataCube(Model inputModel) {

        return execute(inputModel, null);
    }

    private Model execute(Model inputModel, String endpoint) {
        Model cubeData = createModel();
        Resource dataSet;

        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        dataSet = cubeData.createResource(GK.uri + "Instance_Count_"+calendar.getTimeInMillis(), QB.Dataset);
        dataSet.addLiteral(RDFS.comment, "Number of instances for a class");
        dataSet.addLiteral(DCTerms.date, cubeData.createTypedLiteral(calendar));
        dataSet.addLiteral(DCTerms.publisher, "R & D, Unister GmbH, Geoknow");
        if (endpoint != null) {
            dataSet.addProperty(DCTerms.source, endpoint);
        }

        cubeData.add(cubeData.createStatement(dataSet, QB.structure,
                cubeData.createResource(STRUCTURE)));
        QueryExecution queryExec;
        if (inputModel != null) {
            queryExec = QueryExecutionFactory.create(GET_CLASSES,
                    inputModel);
        } else {
            queryExec = QueryExecutionFactory.sparqlService(endpoint, GET_CLASSES);
        }
        ResultSet result = queryExec.execSelect();
        int i = 0;
        while (result.hasNext()) {
            QuerySolution solution = result.next();
            RDFNode owlClass = solution.get("class");
            long instances = solution.getLiteral("count").getLong();
            Resource obs = cubeData.createResource(NAMESPACE + "observation/" + i, QB.Observation);
            obs.addLiteral(GK.MEASURE.InstanceCount, instances);
            obs.addProperty(GK.DIM.Class, cubeData.createResource(owlClass.toString()));
            obs.addProperty(QB.dataset, dataSet);
            i++;
        }
        return cubeData;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();


        Resource structure = cubeData.createResource(STRUCTURE,
                QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(STRUCTURE + "/c1", QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(STRUCTURE + "/c2", QB.ComponentSpecification);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Number of Instances", "en"));
        c2.addProperty(QB.measure, GK.MEASURE.InstanceCount);

        structure.addProperty(QB.component, c1);
        structure.addProperty(RDFS.label,
                cubeData.createLiteral("A Data Structure Definition for Instances Number Metric", "en"));
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.MEASURE.InstanceCountStatements);
        cubeData.add(GK.DIM.ClassStatements);
        return cubeData;
    }

    public Model generateResultsDataCube(String endpointUrl) {

        return this.execute(null, endpointUrl);
    }

    public static void main(String[] args) throws IOException {
        InstancesNumberMetric metric = new InstancesNumberMetric();
        Model r = metric.generateResultsDataCube("http://geo.linkeddata.es/sparql");
        r.write(new FileWriter("datacubes/GeoLinkedData/metric1.ttl"), "TTL");
    }

}
