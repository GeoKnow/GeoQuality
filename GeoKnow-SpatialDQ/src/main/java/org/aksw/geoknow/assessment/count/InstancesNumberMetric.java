package org.aksw.geoknow.assessment.count;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * This metric return the number of instance for each class in the data set.
 *
 * @author d.cherix
 *
 */
public class InstancesNumberMetric implements GeoQualityMetric {

    private static final String GET_CLASSES = "SELECT ?class (count(distinct ?s) as ?count) WHERE {?s a ?class .} group by ?class";

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric1";

    public Model generateResultsDataCube(Model inputModel) {

        Model cubeData = createModel();
        Resource dataSet = cubeData.createResource(NAMESPACE + "dataset/1");

        cubeData.add(cubeData.createStatement(dataSet, QB.structure,
                cubeData.createResource(STRUCTURE)));

        QueryExecution queryExec = QueryExecutionFactory.create(GET_CLASSES,
                inputModel);
        ResultSet result = queryExec.execSelect();
        int i=0;
        while (result.hasNext()) {
            QuerySolution solution = result.next();
            Resource owlClass = solution.getResource("class");
            long instances = solution.getLiteral("count").getLong();
            Resource obs = cubeData.createResource(NAMESPACE+"observation/"+i, QB.Observation);
            obs.addLiteral(GK.MEASURE.InstanceCount, instances);
            obs.addProperty(GK.DIM.Class, owlClass);
            obs.addProperty(QB.dataset, dataSet);
            i++;
        }
        return cubeData;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();

        Resource structure = cubeData.createResource(STRUCTURE,
                QB.DataStructureDefinition);

        structure.addProperty(QB.component, GK.DIM.Class);
        structure.addProperty(RDFS.label,
                cubeData.createLiteral("A Data Structure Definition for Instances Number Metric", "en"));
        structure.addProperty(QB.component, GK.MEASURE.InstanceCount);

        cubeData.add(GK.MEASURE.InstanceCountStatements);
        cubeData.add(GK.DIM.ClassStatements);

        return cubeData;
    }

    public Model generateResultsDataCube(String endpointUrl) {

        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointUrl,
                "CONSTRUCT {?s a ?o .} WHERE {?s a ?o .}");
        Model model = qexec.execConstruct();

        return this.generateResultsDataCube(model);
    }

}
