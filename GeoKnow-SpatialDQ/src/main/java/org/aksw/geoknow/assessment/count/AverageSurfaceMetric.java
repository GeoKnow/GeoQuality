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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class AverageSurfaceMetric implements GeoQualityMetric {

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric3";

    private static final String GET_AREA = "SELECT ?area WHERE { ?instance <http://linkedgeodata.org/ontology/area> ?area . }";

    public Model generateResultsDataCube(Model inputModel) {
        QueryExecution qExec = QueryExecutionFactory.create(GET_AREA,inputModel);
        ResultSet result = qExec.execSelect();
        while(result.hasNext()){
            result.next().get("area");
        }
        return null;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();

        Resource structure = cubeData.createResource(STRUCTURE, QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(STRUCTURE + "/c1", QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Instance", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Instance);

        Resource c2 = cubeData.createResource(STRUCTURE + "/c2", QB.ComponentSpecification);
        c2.addProperty(QB.measure, GK.MEASURE.PropertyCount);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Number of Properties", "en"));

        structure.addProperty(QB.component, c1);
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.DIM.ClassStatements);
        cubeData.add(GK.DIM.PropertyStatements);

        return cubeData;
    }

}
