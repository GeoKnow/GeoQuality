package org.aksw.geoknow.assessment.count;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 *
 * This metric calculates the average number of points per classes.
 *
 *
 * @author Didier Cherix
 *         </br>
 *         R & D, Unister GmbH, Leipzig, Germany</br>
 *         This code is a part of the <a href="http://geoknow.eu/Welcome.html">GeoKnow</a> project.
 *
 */
public class AveragePointsPerClass implements GeoQualityMetric {

    private static final Logger logger = LoggerFactory.getLogger(AveragePointsPerClass.class);

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String GET_CLASSES = "SELECT distinct ?class WHERE {?x a ?class } ";

    private static final ParameterizedSparqlString COUNT_LIST = new ParameterizedSparqlString(
            "PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> "
                    + "SELECT ?instance (COUNT (DISTINCT ?member) as ?count) "
                    + "WHERE { ?instance a ?class . ?instance ?property ?list . ?list list:member ?member . } "
                    + "GROUP BY ?instance");
    private static final ParameterizedSparqlString COUNT_GEO = new ParameterizedSparqlString(
            "SELECT ?instance (COUNT (DISTINCT ?geo) as ?count) "
                    + "WHERE { ?instance a ?class . ?instance ?property ?geo .} "
                    + "GROUP BY ?instance");
    private static final ParameterizedSparqlString COUNT_GEO_TYPE = new ParameterizedSparqlString(
            "SELECT ?instance (COUNT (DISTINCT ?geo) as ?count) "
                    + "WHERE { ?instance a ?class . ?instance ?property ?geo . ?geo a ?type . } "
                    + "GROUP BY ?instance");
    private static final ParameterizedSparqlString COUNT_WKT = new ParameterizedSparqlString(
            "SELECT ?instance (COUNT (DISTINCT ?geo) as ?count) "
                    + "WHERE { ?instance a ?class . ?instance ?property ?geo . ?geo <http://www.opengis.net/ont/geosparql#asWKT> ?wkt . FILTER(strStarts(?wkt,\"POINT\")) } "
                    + "GROUP BY ?instance");

    public static void main(String[] args) throws IOException {
        // Model m = ModelFactory.createDefaultModel();
        // m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/", "TTL");
        GeoQualityMetric metric = new AveragePointsPerClass(
                new PropertyImpl("http://www.w3.org/2003/01/geo/wgs84_pos#lat_long"));
        Model r = metric.generateResultsDataCube("http://linkedgeodata.org/sparql");
        r.write(new FileWriter("datacubes/LinkedGeoData/metric5.ttl"), "TTL");
    }

    private final Property property;
    private String type = null;
    private List<String> defaultGraphs = null;
    private boolean wkt = false;

    private final String structureUri;

    public AveragePointsPerClass(Property p) {
        this.property = p;
        this.structureUri = NAMESPACE + "AveragePointsPerClassStructure";
    }

    public AveragePointsPerClass(Property p, boolean wkt) {
        this.property = p;
        this.structureUri = NAMESPACE + "AveragePointsPerClassStructure";
        this.wkt = wkt;
    }

    public AveragePointsPerClass(Property property, List<String> defaultGraphs) {
        this(property);
        this.defaultGraphs = defaultGraphs;
    }

    public AveragePointsPerClass(Property property, List<String> defaultGraphs, boolean wkt) {
        this(property, wkt);
        this.defaultGraphs = defaultGraphs;
    }

    public AveragePointsPerClass(Property p, String type) {
        this(p);
        this.type = type;
    }

    public AveragePointsPerClass(Property property, String type, List<String> defaultGraphs) {
        this(property, type);
        this.defaultGraphs = defaultGraphs;
    }


    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();
//        cubeData.createResource(NAMESPACE + "/structure/metric" + property.getLocalName(), QB.MeasureProperty);

        Resource structure = cubeData.createResource(structureUri, QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(structure + "/c1", QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Class", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Class);

        Resource c2 = cubeData.createResource(structure + "/c2", QB.ComponentSpecification);
        c2.addProperty(RDFS.label,
                cubeData.createLiteral("Component Specification of Average of " + property + " per Instance", "en"));
        c2.addProperty(QB.measure, GK.MEASURE.Average);

        structure.addProperty(QB.component, c1);
        structure.addProperty(RDFS.label,
                cubeData.createLiteral("A Data Structure Definition for Instances Number Metric", "en"));
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.MEASURE.AverageStatements);
        cubeData.add(GK.DIM.ClassStatements);
        // cubeData.commit();
        return cubeData;
    }

    private Model execute(Model inputModel, QueryExecution queryExec, String endpoint) {
        int obsCount = 0;
        Model cube = createModel();

        Resource dataset;
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        dataset = cube.createResource(GK.uri + "AveragePoints", QB.Dataset);
        dataset.addLiteral(RDFS.comment, "Points per class");
        dataset.addLiteral(DCTerms.date, cube.createTypedLiteral(calendar));
        dataset.addLiteral(DCTerms.publisher, "R & D, Unister GmbH, Geoknow");
        dataset.addProperty(QB.structure, cube.createResource(structureUri));
        if (endpoint != null) {
            dataset.addProperty(DCTerms.source, endpoint);
        }

        for (ResultSet result = queryExec.execSelect(); result.hasNext();) {
            QuerySolution solution = result.next();
            Resource owlClass = solution.getResource("class");
            logger.info("Proccesing class {} with list", owlClass);
            double sum = 0;
            double i = 0;
            COUNT_LIST.setIri("class", owlClass.getURI());
            COUNT_LIST.setIri("property", property.getURI());
            QueryExecution execCount = null;
            if (inputModel != null) {
                execCount = QueryExecutionFactory.create(COUNT_LIST.asQuery(), inputModel);
            } else {
                execCount = QueryExecutionFactory.sparqlService(endpoint, COUNT_LIST.asQuery(), defaultGraphs,
                        defaultGraphs);
            }
            for (ResultSet resultCount = execCount.execSelect(); resultCount.hasNext();) {
                QuerySolution next = resultCount.next();
                System.out.println(next);
                sum += next.get("count").asLiteral().getInt();
                i++;
            }
            Resource obs = cube.createResource(structureUri + "/obs/" + obsCount, QB.Observation);
            double average = i == 0 ? 0 : sum / i;
            if (average == 0) {
                logger.info("Proccesing class {} with geometry", owlClass);
                sum = 0;
                i = 0;
                Query query;
                if (type != null) {
                    COUNT_GEO_TYPE.setIri("class", owlClass.getURI());
                    COUNT_GEO_TYPE.setIri("property", property.getURI());
                    COUNT_GEO_TYPE.setIri("type", type);
                    query = COUNT_GEO_TYPE.asQuery();
                } else if (wkt) {
                    COUNT_WKT.setIri("property", property.getURI());
                    query = COUNT_WKT.asQuery();
                } else {
                    COUNT_GEO.setIri("class", owlClass.getURI());
                    COUNT_GEO.setIri("property", property.getURI());
                    query = COUNT_GEO.asQuery();
                }
                execCount = null;
                if (inputModel != null) {
                    execCount = QueryExecutionFactory.create(query, inputModel);
                } else {
                    execCount = QueryExecutionFactory.sparqlService(endpoint, query, defaultGraphs, defaultGraphs);
                }
                for (ResultSet resultCount = execCount.execSelect(); resultCount.hasNext();) {
                    QuerySolution next = resultCount.next();
                    sum += next.get("count").asLiteral().getInt();
                    i++;
                }
            }
            average = i == 0 ? 0 : sum / i;

            obs.addProperty(GK.MEASURE.Average, cube.createTypedLiteral(average));
            obs.addProperty(GK.DIM.Class, owlClass);
            obs.addProperty(QB.dataset, dataset);
            obsCount++;
        }
        return cube;
    }

    public Model generateResultsDataCube(Model inputModel) {

        QueryExecution queryExec = QueryExecutionFactory.create(GET_CLASSES,
                inputModel);

        return execute(inputModel, queryExec, null);
    }

    public Model generateResultsDataCube(String endpointUrl) {
        QueryExecution queryExec = QueryExecutionFactory.sparqlService(endpointUrl, GET_CLASSES, defaultGraphs,
                defaultGraphs);

        return execute(null, queryExec, endpointUrl);
    }

}
