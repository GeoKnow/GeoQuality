/**
 * 
 */
package org.aksw.geoknow.assessment.polygons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vacabularies.QB;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.OrchidMapper;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory.Type;

/**
 * Compute average distance between polygons which represent the same resource
 * as a table it should be (the data in the second row is Just an example)
 * Metric 	 | Class 						| Time Stamp 					| averageDistance
 * Hausdorff | dbpedia-owl:PopulatedPlace	| Thu Feb 12 14:28:06 CET 2015	| 0.987
 * 
 * @author sherif
 *
 */
public class AveragePolygonsMetric implements GeoQualityMetric {
	private static final Logger logger = Logger.getLogger(AveragePolygonsMetric.class.getName());
	private static final double DISTANCE_THRESHOULD = 0.7;
	private static final int 	MAX_CLASS_COUNT = 1; // set to a positive number for demo
	private static final String BASE_URI = "http://www.geoknow.eu/";
	
	// Source dataset
	private String sourceEndpoint = "http://live.dbpedia.org/sparql";
	private String sourceAuthority = "http://dbpedia.org";
	private String sourceGeoPredicate =  "<http://www.w3.org/2003/01/geo/wgs84_pos#geometry>";
	private static Cache source = new HybridCache();
	
	// Target dataset
	private String targetEndpoint = "http://linkedgeodata.org/sparql";
	private String targetAuthority = "http://linkedgeodata.org";
	private String targetGeoPredicate =  "<http://geovocab.org/geometry#geometry>/<http://www.opengis.net/ont/geosparql#asWKT>";
	private static Cache target = new HybridCache();
	
	private Set<String> polygonMertices = new HashSet<String>(Arrays.asList(
			"hausdorff", 
			"geomin", 
			"geomax", 
			"geoavg", 
			"geolink", 
			"geoquinlan", 
			"geosummin", 
			"surjection", 
			"fairsurjection"));
	

	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#generateResultsDataCube(java.lang.String)
	 */
	public Model generateResultsDataCube(String endpointUrl) {
		DataCubeWriter dataCube = new DataCubeWriter();

		// DataSet
		Resource dataset = ResourceFactory.createResource(BASE_URI + "avg_poly_dataset");
		String dLabel = "Per class average polygons distances";
		String dComment = "Average distance between polygons which represent the same resource";
		String dPublisher = "AKSW, GeoKnow";
		String dDate = (new Date()).toString();
		Resource dsDef = ResourceFactory.createResource(BASE_URI + "avg_poly_data_struct_def");
		dataCube.addDataset(dataset, dLabel, dComment, dPublisher, dDate, dsDef);

		// Data Structure Definitions
		String dsLabel = "A Data Structure Definition";
		String dsComment = "A Data Structure Definition for " + dataset;
		Resource compCls = ResourceFactory.createResource(BASE_URI + "avg_poly_comp_cls");
		Resource compTime  = ResourceFactory.createResource(BASE_URI + "avg_poly_comp_time");
		Resource compMetric  = ResourceFactory.createResource(BASE_URI + "avg_poly_comp_metric");
		Resource compDist  = ResourceFactory.createResource(BASE_URI + "avg_poly_comp_dist");
		Set<Resource> dsComponents = new HashSet<Resource>(Arrays.asList(compCls, compTime, compMetric, compDist));
		dsComponents.add(ResourceFactory.createResource(BASE_URI + "avg_poly_comp_time"));
		dataCube.addDataStructureDefinition(dsDef, dsLabel, dsComment , dsComponents);

		//	Component Specifications
		String clsLabel = "Component Specification of Class";
		Property clsProperty = ResourceFactory.createProperty(BASE_URI + "avg_poly_cls_prop");
		dataCube.addDimensionProperty(clsProperty, "Class Name");
		dataCube.addDimensionSpecs(compCls, clsLabel, clsProperty);

		String timeLabel = "Component Specification of Time Stamp";
		Property timeProperty = ResourceFactory.createProperty(BASE_URI + "avg_poly_time_prop");
		dataCube.addDimensionProperty(timeProperty, "Time Stamp");
		dataCube.addDimensionSpecs(compTime, timeLabel, timeProperty);

		String metricLabel = "Component Specification of Polygon Metric";
		Property metricProperty = ResourceFactory.createProperty(BASE_URI + "avg_poly_metric_prop");
		dataCube.addDimensionProperty(timeProperty, "Polygon Metric");
		dataCube.addDimensionSpecs(compMetric, metricLabel, metricProperty);

		String distLabel = "Average Polygon Distance";
		Property distProperty = ResourceFactory.createProperty(BASE_URI + "avg_poly_dist_prop");
		dataCube.addMeasureProperty(distProperty, distLabel);
		dataCube.addMeasureSpecs(compDist, distLabel, distProperty);

		// Observations
		int i = 1, j = 1; 
		for(String pm : polygonMertices ){
			sourceEndpoint = endpointUrl;
			Map<Resource, Double> avgDists = computePerClassAverageDistance(pm);
			for(Resource c : avgDists.keySet()){
				Resource observation = ResourceFactory.createResource(BASE_URI + "avg_poly_obv_" + i + "_" + j++);
				DataCubeWriter.dataCubeModel.add(observation, RDF.type, QB.Observation);
				DataCubeWriter.dataCubeModel.add(observation, QB.dataset, dataset);
				DataCubeWriter.dataCubeModel.add(observation, clsProperty, c);
				DataCubeWriter.dataCubeModel.add(observation, timeProperty, (new Date()).toString());
				DataCubeWriter.dataCubeModel.add(observation, metricProperty, pm);
				DataCubeWriter.dataCubeModel.add(observation, distProperty, avgDists.get(c) + "");
			}
			j++;
		}
		return dataCube.getDataCubeModel();
	}

	/**
	 * @param distanceMetric
	 * @return map of classes names to average distances computed using distanceMetric
	 * @author sherif
	 */
	public Map<Resource, Double> computePerClassAverageDistance(String distanceMetric) {
		Map<Resource, Double> result = new HashMap<Resource, Double>();
		List<Resource> classes = getSourceClasses();
		int i = 0;
		for(Resource c : classes){
			logger.info("Processing class: " + c.toString());
			readSourceTargetCache(c);
			Mapping m = getMapping(distanceMetric);
			Double avg = computeAverageSimilarity(m);
			if(!avg.isNaN()){
				result.put(c, avg);	
			}
			if(MAX_CLASS_COUNT > 0 && i++ == MAX_CLASS_COUNT){ // for demo
				break;
			}
		}
		return result;
	}

	public double computeAverageSimilarity(Mapping m){
		double result = 0d;
		for (String key : m.map.keySet()) {
			for (String value : m.map.get(key).keySet()) {
				result += m.map.get(key).get(value);
			}
		}
		result /= m.size();
		return result;
	}
	

	/**
	 * @param threshold
	 * @param distanceMetric
	 * @return the polygons distances computed using distanceMetric
	 * @author sherif
	 */
	public Mapping getMapping(String distanceMetric){
		//get sets of polygons from properties
		OrchidMapper mapper = new OrchidMapper();
		Set<Polygon> sourcePolygons = mapper.getPolygons(source, sourceGeoPredicate);
		Set<Polygon> targetPolygons = mapper.getPolygons(target, targetGeoPredicate);
		float theta = (1 / (float) DISTANCE_THRESHOULD) - 1;
		Type type = mapper.getTypeFromExpression(distanceMetric);
		GeoHR3 orchid = new GeoHR3(theta, GeoHR3.DEFAULT_GRANULARITY, type);
		return orchid.run(sourcePolygons, targetPolygons);
	}


	/**
	 * @param m
	 * @return List of classes included in the source end point
	 * @author sherif
	 */
	public List<Resource> getSourceClasses(){
		List<Resource> results = new ArrayList<Resource>();
		String sparqlQueryString = "SELECT DISTINCT ?c { ?s a ?c.}";
		QueryFactory.create(sparqlQueryString);
		logger.info("Querying " + sourceEndpoint  + " with: " + sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sparqlQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			Resource c = qs.getResource("?c");
			if(c.toString().startsWith(sourceAuthority)){
				results.add(c);
			}
		}
		qexec.close() ;
		return results;
	}


	public AveragePolygonsMetric() {
		super();
	}
	
	/**
	 * @param sourceEndpoint
	 * @param sourceAuthority
	 * @param sourceGeoPredicate
	 * @param targetEndpoint
	 * @param targetAuthority
	 * @param targetGeoPredicate
	 *@author sherif
	 */
	public AveragePolygonsMetric(String sourceEndpoint, String sourceAuthority,
			String sourceGeoPredicate, String targetEndpoint,
			String targetAuthority, String targetGeoPredicate) {
		super();
		this.sourceEndpoint = sourceEndpoint;
		this.sourceAuthority = sourceAuthority;
		this.sourceGeoPredicate = sourceGeoPredicate;
		this.targetEndpoint = targetEndpoint;
		this.targetAuthority = targetAuthority;
		this.targetGeoPredicate = targetGeoPredicate;
	}

	/**
	 * @param m
	 * @param r
	 * @return
	 * @author sherif
	 */
	public List<Resource> getSameAsInstances(Model m, Resource r){
		List<Resource> results = new ArrayList<Resource>();
		String sparqlQueryString = 
				"SELECT DISTINCT ?s " +
						"{<" + r.toString() + "> <" + OWL.sameAs + "> ?q}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			results.add(qs.getResource("?q"));
		}
		qexec.close() ;
		return results;
	}


	/**
	 * Read both source and target polygons
	 * @param className
	 * @author sherif
	 */
	public void readSourceTargetCache(Resource className){
		source = new HybridCache();
		target = new HybridCache();
		// read source polygons
		String sourceQueryString =
				"SELECT DISTINCT ?s ?sg ?t ?tg "
						+ "WHERE { "
						+ "?s a <" + className.toString() +"> . "
						+ "?s <" + OWL.sameAs + "> ?t . "
						+ "?s " + sourceGeoPredicate + " ?sg . "
						+ "}";
		logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + sourceQueryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sourceQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			RDFNode s = qs.get("?s");
			RDFNode sWKT = qs.get("?sg");
			RDFNode t = qs.get("?t");
			if(!t.equals(null) && !targetAuthority.equals(null) && t.toString().startsWith(targetAuthority)){
				// read target polygons
				String targetQueryString =
						"SELECT DISTINCT ?tg "
								+ "WHERE { "
								+ "<" + t.toString() +"> " + targetGeoPredicate+ " ?tg . "
								+ "}";
				logger.info("Querying " + targetEndpoint  + " with SPARQL: " + targetQueryString);
				QueryExecution targetQExec = QueryExecutionFactory.sparqlService(targetEndpoint, targetQueryString);
				ResultSet targetQueryResults = targetQExec.execSelect();
				while(targetQueryResults.hasNext()){
					QuerySolution tqs = targetQueryResults.nextSolution();
					RDFNode tWKT = tqs.get("?tg");
					if(!tWKT.equals(null)){
						source.addTriple(s.toString(), sourceGeoPredicate, sWKT.toString());
						target.addTriple(t.toString(), targetGeoPredicate, tWKT.toString());	
					}
				}
				targetQExec.close() ;
			}
		}
		qexec.close() ;
	}


	/**
	 * @return the sourceEndpoint
	 */
	public String getSourceEndpoint() {
		return sourceEndpoint;
	}

	/**
	 * @param sourceEndpoint the sourceEndpoint to set
	 */
	public void setSourceEndpoint(String sourceEndpoint) {
		this.sourceEndpoint = sourceEndpoint;
	}

	/**
	 * @return the targetEndpoint
	 */
	public String getTargetEndpoint() {
		return targetEndpoint;
	}

	/**
	 * @param targetEndpoint the targetEndpoint to set
	 */
	public void setTargetEndpoint(String targetEndpoint) {
		this.targetEndpoint = targetEndpoint;
	}

	/**
	 * @return the targetAuthority
	 */
	public String getTargetAuthority() {
		return targetAuthority;
	}

	/**
	 * @param targetAuthority the targetAuthority to set
	 */
	public void setTargetAuthority(String targetAuthority) {
		this.targetAuthority = targetAuthority;
	}

	/**
	 * @return the sourceGeoPredicate
	 */
	public String getSourceGeoPredicate() {
		return sourceGeoPredicate;
	}

	/**
	 * @param sourceGeoPredicate the sourceGeoPredicate to set
	 */
	public void setSourceGeoPredicate(String sourceGeoPredicate) {
		this.sourceGeoPredicate = sourceGeoPredicate;
	}

	/**
	 * @return the targetGeoPredicate
	 */
	public String getTargetGeoPredicate() {
		return targetGeoPredicate;
	}

	/**
	 * @param targetGeoPredicate the targetGeoPredicate to set
	 */
	public void setTargetGeoPredicate(String targetGeoPredicate) {
		this.targetGeoPredicate = targetGeoPredicate;
	}

	/**
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		AveragePolygonsMetric m = new AveragePolygonsMetric();
		m.targetAuthority = "http://linkedgeodata.org";
		m.generateResultsDataCube("http://dbpedia.org/sparql").write(System.out, "TTL");
//		m.readSourceTargetCache(ResourceFactory.createResource("http://dbpedia.org/ontology/AdministrativeRegion"));
//		System.out.println(AveragePolygonsMetric.source.toString());
//		System.out.println(AveragePolygonsMetric.target.toString());
	}



}
