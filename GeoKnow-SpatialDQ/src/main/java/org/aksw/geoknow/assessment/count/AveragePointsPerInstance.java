package org.aksw.geoknow.assessment.count;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 *
 * This metric calculates the average number of points per classes.
 *
 *
 * @author Didier Cherix
 * </br> R & D, Unister GmbH, Leipzig, Germany</br>
 * This code is a part of the <a href="http://geoknow.eu/Welcome.html">GeoKnow</a> project.
 *
 */
public class AveragePointsPerInstance implements GeoQualityMetric {

    private final Property property;
    private Property Average;
    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";
    private static final String GET_CLASSES = "SELECT distinct ?class WHERE {?x a ?class } ";
    private static final ParameterizedSparqlString COUNT = new ParameterizedSparqlString(
            "PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> "
            + "SELECT ?instance (COUNT (DISTINCT ?member) as ?count) "
            + "WHERE { ?instance a ?class . ?instance ?property ?list . ?list list:member ?member . } "
            + "GROUP BY ?instance");

    private final String structureUri;

    public AveragePointsPerInstance(Property p) {
        this.property = p;
        this.structureUri = NAMESPACE + "metric/" + property.hashCode();
    }

    public Model generateResultsDataCube(Model inputModel) {
        Model cube = createModel();
        int obsCount = 0;
        QueryExecution queryExec = QueryExecutionFactory.create(GET_CLASSES,
                inputModel);

        for ( ResultSet result = queryExec.execSelect(); result.hasNext();) {
            QuerySolution solution = result.next();
            Resource owlClass = solution.getResource("class");
            System.out.println(owlClass);
            double sum = 0;
            double i = 0;
            COUNT.setIri("class", owlClass.getURI());
            COUNT.setIri("property", property.getURI());
            QueryExecution execCount = QueryExecutionFactory.create(COUNT.asQuery(),inputModel);
           for( ResultSet resultCount = execCount.execSelect(); resultCount.hasNext();){
                QuerySolution next = resultCount.next();
                System.out.println(next);
                sum += next.get("count").asLiteral().getInt();
                i++;
            }
            Resource obs = cube.createResource(structureUri + "/obs/" + obsCount, QB.Observation);
            double average = i == 0 ? 0 : sum / i;
            obs.addProperty(Average, cube.createTypedLiteral(average));
            obs.addProperty(GK.DIM.Class, owlClass);
            obsCount++;
        }
        return cube;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();
        Average = cubeData.createProperty("http://www.geoknow.eu/data-cube/Average");
        cubeData.createResource(NAMESPACE + "/structure/metric" + property.hashCode(), QB.MeasureProperty);

        Resource structure = cubeData.createResource(structureUri, QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(structure + "/c1", QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(structure + "/c2", QB.ComponentSpecification);
        c2.addProperty(RDFS.label,
                cubeData.createLiteral("Component Specification of Average of " + property + " per Instance", "en"));
        c2.addProperty(QB.measure, Average);

        structure.addProperty(QB.component, c1);
        structure.addProperty(RDFS.label,
                cubeData.createLiteral("A Data Structure Definition for Instances Number Metric", "en"));
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.MEASURE.InstanceCountStatements);
        cubeData.add(GK.DIM.ClassStatements);

        return cubeData;
    }

    public static void main(String[] args) throws IOException {
        Model m = ModelFactory.createDefaultModel();
        m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/", "TTL");
        GeoQualityMetric metric = new AveragePointsPerInstance(m.createProperty("http://geovocab.org/geometry#posList"));
        Model r = metric.generateResultsDataCube(m);
        r.write(new FileWriter("dataquality/metric5.ttl"), "TTL");
    }

}
