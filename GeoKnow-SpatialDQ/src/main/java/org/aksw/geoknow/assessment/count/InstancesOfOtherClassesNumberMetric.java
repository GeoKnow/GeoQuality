package org.aksw.geoknow.assessment.count;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class InstancesOfOtherClassesNumberMetric implements GeoQualityMetric {

    private static final String INSTANCE_CLASS = "Select distinct  ?class WHERE {?instance a ?class .}";
    private static final ParameterizedSparqlString OTHER_CLASSES = new ParameterizedSparqlString(
            "SELECT (COUNT (DISTINCT ?instance) WHERE { ?instance a ?class . ?instance a ?originClass . FILTER(!(?class = ?originClass))}");

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric4";
    public Model generateResultsDataCube(Model inputModel) {
        Model cubeData=createModel();
        Resource dataset=cubeData.createResource(NAMESPACE+"/dataset/4", QB.Dataset);
        QueryExecution qexec = QueryExecutionFactory.create(INSTANCE_CLASS,inputModel);
        QuerySolution solution=null;
        int i=0;
        for(ResultSet result=qexec.execSelect();result.hasNext();i++){
             solution = result.next();
             Resource originClass = solution.getResource("class");
             OTHER_CLASSES.setIri("originClass", originClass.getURI());
             QueryExecution execCount = QueryExecutionFactory.create(OTHER_CLASSES.asQuery(),inputModel);
             ResultSet resultCount = execCount.execSelect();
             if(resultCount.hasNext()){
                 Resource obs = cubeData.createResource(""+i, QB.Observation);
                 obs.addProperty(GK.DIM.Class, originClass);
                 obs.addProperty(GK.MEASURE.OtherClassesCount, resultCount.next().getLiteral("count"));
                 obs.addProperty(QB.dataset, dataset);
             }
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
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(STRUCTURE + "/c2", QB.ComponentSpecification);
        c2.addProperty(QB.measure, GK.MEASURE.OtherClassesCount);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Number of Other Classes", "en"));


        structure.addProperty(QB.component, c1);
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.DIM.ClassStatements);
        cubeData.add(GK.MEASURE.OtherClassesCountStatements);

        return cubeData;
    }

}
