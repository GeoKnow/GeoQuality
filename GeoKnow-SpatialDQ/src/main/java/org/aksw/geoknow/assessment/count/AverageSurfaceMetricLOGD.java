package org.aksw.geoknow.assessment.count;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.aksw.geoknow.helper.vocabularies.QB;
import org.geotools.geometry.jts.JTSFactoryFinder;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 *
 * This metric calculates the average surface per classes.
 *
 *
 * @author Didier Cherix
 *         </br>
 *         R & D, Unister GmbH, Leipzig, Germany</br>
 *         This code is a part of the <a href="http://geoknow.eu/Welcome.html">GeoKnow</a> project.
 *
 */
public class AverageSurfaceMetricLOGD implements GeoQualityMetric {

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric3";

    private String structureUri;

    private GeometryFactory geometryFactory;

    private Double firstLong;
    private Double firstLat;

    private static final ParameterizedSparqlString GET_INSTANCES = new ParameterizedSparqlString(
            "SELECT distinct ?instance WHERE {?instance a ?class}");
    private static final String GET_CLASSES = "SELECT distinct ?class WHERE {?x a ?class } ";
    private static final ParameterizedSparqlString POLYGON = new ParameterizedSparqlString(
            "PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> "
                    + "prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
                    + "prefix ngeo: <http://geovocab.org/geometry#> "
                    + "SELECT ?geometry  "
                    + "WHERE { { ?instance <http://www.opengis.net/ont/geosparql#asWKT> ?geometry . } "
                    + "UNION { ?instance ngeo:geometry ?g . ?g <http://www.opengis.net/ont/geosparql#asWKT> ?geometry .} }");

    public AverageSurfaceMetricLOGD() {
        this.structureUri = NAMESPACE + "metric/averageSurface";
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
    }

    public Model generateResultsDataCube(Model inputModel) {

        return execute(inputModel, null);
    }

    private Model execute(Model inputModel, String endpoint) {
        Model cube = createModel();

        Resource dataset;
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        dataset = cube.createResource(GK.uri + "Average_Surface" + calendar.getTimeInMillis(), QB.Dataset);
        dataset.addLiteral(RDFS.comment, "Properties per instance");
        dataset.addLiteral(DCTerms.date, cube.createTypedLiteral(calendar));
        dataset.addLiteral(DCTerms.publisher, "R & D, Unister GmbH, Geoknow");
        dataset.addProperty(QB.structure, cube.createResource(STRUCTURE));
        if (endpoint != null) {
            dataset.addProperty(DCTerms.source, endpoint);
        }

        QueryExecution qExec;
        if (inputModel != null) {
            qExec = QueryExecutionFactory.create(GET_CLASSES, inputModel);
        } else {
            qExec = QueryExecutionFactory.sparqlService(endpoint, GET_CLASSES);
        }
        ResultSet result = qExec.execSelect();
        int obsCount = 0;
        while (result.hasNext()) {
            double area = 0;
            int i = 0;
            Resource owlClass = result.next().get("class").asResource();
            System.out.println(owlClass);
            GET_INSTANCES.setIri("class", owlClass.getURI());

            QueryExecution qexecInstances;
            if (inputModel != null) {
                qexecInstances = QueryExecutionFactory.create(GET_INSTANCES.asQuery(), inputModel);
            } else {
                qexecInstances = QueryExecutionFactory.sparqlService(endpoint, GET_INSTANCES.asQuery());
            }
            for (ResultSet instancesResult = qexecInstances.execSelect(); instancesResult.hasNext();) {

                QuerySolution next = instancesResult.next();
                String instance = next.get("instance").asResource().getURI();
                if (instance == null) {
                    continue;
                }
                POLYGON.setIri("instance", instance);
                QueryExecution qexecMember;
                if (inputModel != null) {
                    qexecMember = QueryExecutionFactory.create(POLYGON.asQuery(), inputModel);
                } else {
                    qexecMember = QueryExecutionFactory.sparqlService(endpoint, POLYGON.asQuery());
                }

                try {
                    ResultSet wkt = qexecMember.execSelect();
                    if (wkt.hasNext()) {
                        String geometry = wkt.next().get("geometry").asLiteral().getString();

                        area += calculateArea(geometry);
                    }
                } catch (Exception e) {
                    System.out.println(POLYGON.asQuery());
                    e.printStackTrace();
                }
                i++;
            }
            Resource obs = cube.createResource(structureUri + "/obs/" + obsCount, QB.Observation);
            double average = i == 0 ? 0 : area / i;
            obs.addProperty(GK.MEASURE.Average, cube.createTypedLiteral(average));
            obs.addProperty(GK.DIM.Class, owlClass);
            obs.addProperty(QB.dataset, dataset);
            obsCount++;
        }
        return cube;
    }

    private double calculateArea(String wktString) {
        WKTReader reader = new WKTReader(geometryFactory);

        try {
            Geometry geometry = reader.read(wktString);
            if (geometry instanceof Polygon) {
                double polygonArea = ((Polygon) geometry).getArea();
                System.out.println("Area: " + polygonArea);
                return polygonArea;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        return this.execute(null, endpointUrl);
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();
        Resource structure = cubeData.createResource(STRUCTURE, QB.DataStructureDefinition);

        Resource c1 = cubeData.createResource(STRUCTURE + "/c1", QB.ComponentSpecification);
        c1.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Instance", "en"));
        c1.addProperty(QB.dimension, GK.DIM.Instance);

        Resource c2 = cubeData.createResource(STRUCTURE + "/c2", QB.ComponentSpecification);
        c2.addProperty(QB.measure, GK.MEASURE.Average);
        c2.addProperty(RDFS.label, cubeData.createLiteral("Component Specification of Number of Properties", "en"));

        structure.addProperty(QB.component, c1);
        structure.addProperty(QB.component, c2);

        cubeData.add(GK.DIM.ClassStatements);
        cubeData.add(GK.DIM.PropertyStatements);
        cubeData.add(GK.MEASURE.AverageStatements);

        return cubeData;
    }

    public static void main(String[] args) throws IOException {
        // Model m = ModelFactory.createDefaultModel();
        // m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/", "TTL");
        GeoQualityMetric metric = new AverageSurfaceMetricLOGD();
        Model r = metric.generateResultsDataCube("http://akswnc3.informatik.uni-leipzig.de:8850/sparql");
        r.write(new FileWriter("datacubes/LinkedGeoData/metric3b.ttl"), "TTL");
    }

}
