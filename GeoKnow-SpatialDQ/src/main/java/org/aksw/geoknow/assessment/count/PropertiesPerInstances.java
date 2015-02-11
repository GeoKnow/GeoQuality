package org.aksw.geoknow.assessment.count;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class PropertiesPerInstances implements GeoQualityMetric {

    private final ParameterizedSparqlString NUMBER_OF_PROPERTIES = new ParameterizedSparqlString(
            "SELECT (COUNT(DISTINCT ?properties) as ?count) WHERE { ?s ?p ?o . }");
    private final String INSTANCES = "SELECT distinct ?instance ?class WHERE { ?instance a ?class .}";
    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric2";

    public Model generateResultsDataCube(Model inputModel) {
        Model cube = createModel();
        Resource dataset = cube.createResource(NAMESPACE+"/dataset/2",QB.Dataset);
        dataset.addProperty(QB.structure, cube.createResource(STRUCTURE));

        QueryExecution qExec = QueryExecutionFactory.create(INSTANCES);
        ResultSet result = qExec.execSelect();
        while (result.hasNext()) {
            Resource instance = result.next().getResource("instance");
            Resource Class = result.next().getResource("class");
            NUMBER_OF_PROPERTIES.setIri("s", instance.getURI());
            QueryExecution propertiesQexec = QueryExecutionFactory.create(NUMBER_OF_PROPERTIES.asQuery());
            ResultSet propertiesResult = propertiesQexec.execSelect();
            if(propertiesResult.hasNext()){
                Resource obs = cube.createResource("", QB.Observation);
                obs.addProperty(QB.dataset, dataset);
                obs.addProperty(GK.DIM.Instance, instance);
                obs.addProperty(GK.DIM.Class, Class);
                obs.addLiteral(GK.MEASURE.PropertyCount, propertiesResult.next().getLiteral("count"));
            }
        }
        return cube;
    }

    private Model createModel() {
        Model cubeData= ModelFactory.createDefaultModel();

        Resource structure = cubeData.createResource(STRUCTURE, QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(STRUCTURE+"/c1",QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(STRUCTURE+"/c2",QB.ComponentSpecification);
        c2.addProperty(QB.measure, GK.MEASURE.PropertyCount);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Number of Properties", "en"));

        Resource c3 = cubeData.createResource(STRUCTURE+"/c3",QB.ComponentSpecification);
        c3.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Instance", "en"));
        c3.addProperty(QB.dimension, GK.DIM.Instance);

        structure.addProperty(QB.component, c1);
        structure.addProperty(QB.component, c2);
        structure.addProperty(QB.component, c3);

        cubeData.add(GK.DIM.ClassStatements);
        cubeData.add(GK.DIM.PropertyStatements);
        cubeData.add(GK.DIM.InstanceStatements);

        return cubeData;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        // TODO Auto-generated method stub
        return null;
    }

}
