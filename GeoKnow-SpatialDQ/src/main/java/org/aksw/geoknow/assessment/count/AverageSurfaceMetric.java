package org.aksw.geoknow.assessment.count;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
import com.hp.hpl.jena.vocabulary.RDFS;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class AverageSurfaceMetric implements GeoQualityMetric {

    private static final String NAMESPACE = "http://www.geoknow.eu/data-cube/";

    private static final String STRUCTURE = NAMESPACE + "metric3";
    private Property Average;

    private String structureUri;

    private GeometryFactory geometryFactory;

    private Double firstLong;
    private Double firstLat;

    private static final String GET_AREA = "SELECT ?area WHERE { ?instance <http://linkedgeodata.org/ontology/area> ?area . }";
    private static final ParameterizedSparqlString GET_INSTANCES = new ParameterizedSparqlString(
            "SELECT distinct ?instance WHERE {?instance a ?class}");
    private static final String GET_CLASSES = "SELECT distinct ?class WHERE {?x a ?class } ";
    private static final ParameterizedSparqlString POLYGON = new ParameterizedSparqlString(
            "PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> "
                    + "prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
                    + "prefix ngeo: <http://geovocab.org/geometry#> "
                    + "SELECT ?lat ?long  "
                    + "WHERE { ?instance ngeo:exterior ?exterior . ?exterior ?property ?list . ?list list:member ?member . ?member geo:lat ?lat ; geo:long ?long . } ");
    private static final ParameterizedSparqlString MULTI_POLYGON = new ParameterizedSparqlString(
            "PREFIX list: <http://jena.hpl.hp.com/ARQ/list#> "
                    + "prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> "
                    + "prefix ngeo: <http://geovocab.org/geometry#> "
                    + "SELECT ?polygon ?lat ?long  "
                    + "WHERE { ?instance ngeo:polygonMember ?polygon . ?polygon ngeo:exterior ?exterior . ?exterior ?property ?list . ?list list:member ?member . ?member geo:lat ?lat ; geo:long ?long . } ");

    public AverageSurfaceMetric() {
        this.structureUri = NAMESPACE + "metric/averageSurface";
        this.geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
    }

    public Model generateResultsDataCube(Model inputModel) {

        Model cube = createModel();
        QueryExecution qExec = QueryExecutionFactory.create(GET_CLASSES, inputModel);
        ResultSet result = qExec.execSelect();
        int obsCount = 0;
        while (result.hasNext()) {
            double area = 0;
            int i = 0;
            Resource owlClass = result.next().get("class").asResource();
            System.out.println(owlClass);
            GET_INSTANCES.setIri("class", owlClass.getURI());

            QueryExecution qexecInstances = QueryExecutionFactory.create(GET_INSTANCES.asQuery(), inputModel);
            for (ResultSet instancesResult = qexecInstances.execSelect(); instancesResult.hasNext();) {

                QuerySolution next = instancesResult.next();
                String instance = next.get("instance").asResource().getURI();
                if (instance == null) {
                    continue;
                }
                POLYGON.setIri("instance", instance);
                QueryExecution qexecMember = QueryExecutionFactory.create(POLYGON.asQuery(), inputModel);
                StringBuilder polygonBuilder = new StringBuilder();
                firstLat = null;
                firstLong = null;
                for (ResultSet latLong = qexecMember.execSelect(); latLong.hasNext();) {
                    processPoint(latLong.next(), polygonBuilder);
                }
                if (polygonBuilder.length() > 0) {
                    area += calculateArea(polygonBuilder);
                } else {
                    area = 0;
                    polygonBuilder.setLength(0);
                    this.firstLat = null;
                    this.firstLong = null;
                    MULTI_POLYGON.setIri("instance", instance);
                    QueryExecution qexecMultiPolygon = QueryExecutionFactory
                            .create(MULTI_POLYGON.asQuery(), inputModel);
                    String polygonName = "";
                    for (ResultSet latLong = qexecMultiPolygon.execSelect(); latLong.hasNext();) {
                        QuerySolution solution = latLong.next();
                        if (!polygonName.equals(solution.get("polygon").asNode().getBlankNodeLabel())) {
                            if (polygonBuilder.length() > 0) {
                                area += calculateArea(polygonBuilder);
                            }
                            this.firstLat = null;
                            this.firstLong = null;
                            polygonBuilder.setLength(0);
                        }
                        polygonName = solution.get("polygon").asNode().getBlankNodeLabel();
                        processPoint(solution, polygonBuilder);
                    }
                }
                i++;
            }
            Resource obs = cube.createResource(structureUri + "/obs/" + obsCount, QB.Observation);
            double average = i == 0 ? 0 : area / i;
            obs.addProperty(Average, cube.createTypedLiteral(average));
            obs.addProperty(GK.DIM.Class, owlClass);
            obsCount++;
        }
        return cube;
    }

    private void processPoint(QuerySolution solution, StringBuilder builder) {
        double lat = solution.get("lat").asLiteral().getDouble();
        double longitude = solution.get("long").asLiteral().getDouble();
        builder.append(lat).append(" ").append(longitude).append(",");
        if (this.firstLat == null && this.firstLong == null) {
            this.firstLat = lat;
            this.firstLong = longitude;
        }
    }

    private double calculateArea(StringBuilder builder) {
        WKTReader reader = new WKTReader(geometryFactory);
        builder.append(this.firstLat).append(" ").append(this.firstLong);
        builder.append("))");
        try {
            System.out.println("generate polygon");
            Polygon polygon = (Polygon) reader.read("POLYGON ((" + builder.toString());
            double polygonArea = polygon.getArea();
            System.out.println("Area: " + polygonArea);
            return polygonArea;
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

    public Model generateResultsDataCube(String endpointUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    private Model createModel() {
        Model cubeData = ModelFactory.createDefaultModel();
        Average = cubeData.createProperty("http://www.geoknow.eu/data-cube/Average");
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

    public static void main(String[] args) throws IOException {
        Model m = ModelFactory.createDefaultModel();
        m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/", "TTL");
        GeoQualityMetric metric = new AverageSurfaceMetric();
        Model r = metric.generateResultsDataCube(m);
        r.write(new FileWriter("dataquality/metric3.ttl"), "TTL");
    }

}
