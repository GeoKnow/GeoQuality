package org.aksw.geoknow.assessment.count;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.ontology.OntModel;
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

/**
*
* This metric calculates the average number of properties per classes.
*
*
* @author Didier Cherix
* </br> R & D, Unister GmbH, Leipzig, Germany</br>
* This code is a part of the <a href="http://geoknow.eu/Welcome.html">GeoKnow</a> project.
*
*/
public class PropertiesPerInstances implements GeoQualityMetric {

    private final ParameterizedSparqlString NUMBER_OF_PROPERTIES = new ParameterizedSparqlString(
            "SELECT (COUNT(DISTINCT ?properties) as ?count) WHERE { ?s ?p ?o . ?s a ?class }");
    private final String INSTANCES = "SELECT distinct ?class WHERE { ?instance a ?class .}";
    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric2";

    public Model generateResultsDataCube(Model inputModel) {
        Model cube = createModel();
        Resource dataset = cube.createResource(NAMESPACE+"/dataset/2",QB.Dataset);
        dataset.addProperty(QB.structure, cube.createResource(STRUCTURE));

        QueryExecution qExec = QueryExecutionFactory.create(INSTANCES, inputModel);
        ResultSet result = qExec.execSelect();
        int i=0;
        while (result.hasNext()) {
            Resource owlClass = result.next().getResource("class");
            NUMBER_OF_PROPERTIES.setIri("class", owlClass.getURI());
            QueryExecution propertiesQexec = QueryExecutionFactory.create(NUMBER_OF_PROPERTIES.asQuery(),inputModel);
            ResultSet propertiesResult = propertiesQexec.execSelect();
            if(propertiesResult.hasNext()){
                System.out.println(i);
                Resource obs = cube.createResource("http://www.geoknow.eu/data-cube/metric2/observation"+i, QB.Observation);
                obs.addProperty(QB.dataset, dataset);
                obs.addProperty(GK.DIM.Class, owlClass);
                obs.addLiteral(GK.MEASURE.PropertyCount, propertiesResult.next().getLiteral("count"));
                i++;
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

//        Resource c3 = cubeData.createResource(STRUCTURE+"/c3",QB.ComponentSpecification);
//        c3.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Instance", "en"));
//        c3.addProperty(QB.dimension, GK.DIM.Instance);

        structure.addProperty(QB.component, c1);
        structure.addProperty(QB.component, c2);
//        structure.addProperty(QB.component, c3);

        cubeData.add(GK.DIM.ClassStatements);
        cubeData.add(GK.DIM.PropertyStatements);
        cubeData.add(GK.DIM.InstanceStatements);
        cubeData.add(GK.MEASURE.PropertyCountStatements);

        return cubeData;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    public static void main(String[] args) throws IOException {
        Model m = ModelFactory.createDefaultModel();
        m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/","TTL");
        GeoQualityMetric metric = new PropertiesPerInstances();
        Model r = metric.generateResultsDataCube(m);
        r.write(new FileWriter("dataquality/metric2.ttl"), "TTL");
    }

}
