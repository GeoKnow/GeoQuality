/**
 * 
 */
package org.aksw.geoknow.assessment.polygons;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.helper.vocabularies.QB;
import org.aksw.geoknow.helper.vocabularies.GK.DIM;
import org.aksw.geoknow.helper.vocabularies.SDMX;
import org.aksw.geoknow.helper.vocabularies.SDMX.MEASURE;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.aksw.jena_sparql_api.retry.core.QueryExecutionFactoryRetry;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.reasoner.rulesys.builtins.Max;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.HybridCache;
import de.uni_leipzig.simba.data.Mapping;
import de.uni_leipzig.simba.mapper.atomic.OrchidMapper;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.GeoHR3;
import de.uni_leipzig.simba.mapper.atomic.hausdorff.Polygon;
import de.uni_leipzig.simba.measures.pointsets.SetMeasureFactory;
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
	private static final double DISTANCE_THRESHOULD = 0.0;
	private static int MAX_CLASS_COUNT 	= 100; // set to a positive number for demo
	private static int MAX_INSTACE_COUNT 	= 1000;
	private static final String BASE_URI = "http://www.geoknow.eu/";
	

	// Source dataset
	private String sourceEndpoint;
	private String sourceAuthority;
	private String sourceGeoPredicate ;
	private static Cache source = new HybridCache();
	//	public QueryExecutionFactory sourceQEF;

	// Target dataset
	private String targetEndpoint ;
	private String targetAuthority ;
	private String targetGeoPredicate ;
	private static Cache target = new HybridCache();
	//	public QueryExecutionFactory targetQEF;

	private Set<String> polygonMertices = new HashSet<String>(Arrays.asList(
			"hausdorff", 
			"geomin", 
			"geomax",
			"geomean",
			"geoavg", 
			"geolink", 
			"geosummin", 
			"surjection", 
			"fairsurjection"
			));
	public boolean useOrchid = false;
	private ArrayList<Resource> sourceClassesList = new ArrayList<Resource>();;


	public AveragePolygonsMetric() {
		super();
		//		initializeSourceQEF();
		//		initializeTargetQEF();
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
	public AveragePolygonsMetric(
			String sourceEndpoint, 
			String sourceAuthority,
			String sourceGeoPredicate, 
			String targetEndpoint,
			String targetAuthority,
			String targetGeoPredicate) {
		this();
		this.sourceEndpoint = sourceEndpoint;
		this.sourceAuthority = sourceAuthority;
		this.sourceGeoPredicate = sourceGeoPredicate;
		this.targetEndpoint = targetEndpoint;
		this.targetAuthority = targetAuthority;
		this.targetGeoPredicate = targetGeoPredicate;
	}

	private QueryExecutionFactory initializeQEF(String endpoint) {
		QueryExecutionFactory qef = new QueryExecutionFactoryHttp(endpoint);
		qef = new QueryExecutionFactoryDelay(qef, 5000);
		qef = new QueryExecutionFactoryRetry(qef, 5, 10000);
		//	long timeToLive = 24l * 60l * 60l * 1000l; 
		//	CacheCoreEx sourceCacheBackend = null;
		//	try {
		//		sourceCacheBackend = CacheCoreH2.create("sparql", timeToLive, true);
		//	} catch (ClassNotFoundException | SQLException e) {
		//		e.printStackTrace();
		//	}
		//	CacheEx sourceCacheFrontend = new CacheExImpl(sourceCacheBackend);
		//	sourceQEF = new QueryExecutionFactoryCacheEx(sourceQEF, sourceCacheFrontend);
		qef = new QueryExecutionFactoryPaginated(qef, 900);
		return qef;
	}


	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#generateResultsDataCube(java.lang.String)
	 */
	public Model generateResultsDataCube(String endpointUrl) {
		if(!endpointUrl.equals("")){
			sourceEndpoint = endpointUrl;
		}
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
		Property clsProperty = ResourceFactory.createProperty(DIM.uri + "avg_poly_cls_prop");
		dataCube.addDimensionProperty(clsProperty, "Class Name");
		dataCube.addDimensionSpecs(compCls, clsLabel, clsProperty);

		String timeLabel = "Component Specification of Time Stamp";
		Property timeProperty = ResourceFactory.createProperty(DIM.uri + "avg_poly_time_prop");
		dataCube.addDimensionProperty(timeProperty, "Time Stamp");
		dataCube.addDimensionSpecs(compTime, timeLabel, timeProperty);

		String metricLabel = "Component Specification of Polygon Metric";
		Property metricProperty = ResourceFactory.createProperty(DIM.uri + "avg_poly_metric_prop");
		dataCube.addDimensionProperty(metricProperty, "Polygon Metric");
		dataCube.addDimensionSpecs(compMetric, metricLabel, metricProperty);

		String distLabel = "Average Polygon Distance";
		Property distProperty = ResourceFactory.createProperty(MEASURE.uri + "avg_poly_dist_prop");
		dataCube.addMeasureProperty(distProperty, distLabel);
		dataCube.addMeasureSpecs(compDist, distLabel, distProperty);

		// Observations
		int i = 1, j = 1; 
		for(String pm : polygonMertices ){
			Map<Resource, Double> avgDists = computePerClassAverageDistance(pm);
			if(avgDists.size() == 0){
				continue;
			}
			for(Resource classUri : avgDists.keySet()){
				Resource observation = ResourceFactory.createResource(BASE_URI + "avg_poly_obv_" + i + "_" + j++);
				DataCubeWriter.dataCubeModel.add(observation, RDF.type, QB.Observation);
				DataCubeWriter.dataCubeModel.add(observation, QB.dataset, dataset);
				DataCubeWriter.dataCubeModel.add(observation, clsProperty, classUri);
				DataCubeWriter.dataCubeModel.add(observation, timeProperty, (new Date()).toString());
				DataCubeWriter.dataCubeModel.add(observation, metricProperty, pm);
				DataCubeWriter.dataCubeModel.add(observation, distProperty, avgDists.get(classUri) + "");
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
//				Resource c = ResourceFactory.createResource("http://geo.linkeddata.es/ontology/Provincia") ;
//				Resource c = ResourceFactory.createResource("http://linkedgeodata.org/ontology/City") ;
	

			logger.info("Processing class: " + c.toString());
			readSourceTargetCacheLGD_vsparql(c);
			//			readSourceTargetCacheLGD(c);
			Mapping m = getMapping(distanceMetric);
			Double avg = computeAverageSimilarity(m);
			if(!avg.isNaN()){
				result.put(c, avg);	
			}
			else{
				logger.warn("NO mapping for class: " + c);
			}
			if(MAX_CLASS_COUNT > 0 && i++ == MAX_CLASS_COUNT){ // for demo
				break;
			}
		}
		System.out.println("class2avg: " +result);
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
		Type type = mapper.getTypeFromExpression(distanceMetric);
		if(useOrchid ){
			float theta = (1 / (float) DISTANCE_THRESHOULD) - 1;
			GeoHR3 orchid = new GeoHR3(theta, GeoHR3.DEFAULT_GRANULARITY, type);
			return orchid.run(sourcePolygons, targetPolygons);
		}else{
			return SetMeasureFactory.getMeasure(type).run(sourcePolygons, targetPolygons, Float.MAX_VALUE);
		}
	}


	/**
	 * @param m
	 * @return List of classes included in the source end point
	 * @author sherif
	 */
	public List<Resource> getSourceClasses(){
		//		return new ArrayList<>(Arrays.asList(ResourceFactory.createResource("http://linkedgeodata.org/ontology/City")));
		//		/*
		if(!sourceClassesList.isEmpty()){
			return sourceClassesList;
		}
		String sparqlQueryString = "SELECT DISTINCT ?c { ?s a ?c. FILTER(regex(?c, \"^" + sourceAuthority + "\"))}" ;
		if(MAX_CLASS_COUNT > 0){
			sparqlQueryString += " LIMIT " + MAX_CLASS_COUNT ;
		}
		QueryFactory.create(sparqlQueryString);
		QueryExecutionFactory sourceQEF;
		if(sourceEndpoint.contains("linkedgeodata")){ //dirty hack around
			sourceQEF = initializeQEF("http://linkedgeodata.org/sparql");
			logger.info("Querying http://linkedgeodata.org/sparql with: " + sparqlQueryString);
		}else{
			sourceQEF = initializeQEF(sourceEndpoint);
			logger.info("Querying " + sourceEndpoint  + " with: " + sparqlQueryString);
		}
		QueryExecution qe = sourceQEF.createQueryExecution(sparqlQueryString);
		ResultSet queryResults = qe.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			Resource c = qs.getResource("?c");
				sourceClassesList.add(c);
		}
		qe.close() ;
		logger.info("Found " + sourceClassesList.size() + " classes.");
		return sourceClassesList;
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
				"SELECT DISTINCT ?s ?sg ?t "
						+ "WHERE { "
						+ "?s a <" + className.toString() +"> . "
						+ "?s <" + OWL.sameAs + "> ?t . "
						+ "?s " + sourceGeoPredicate + " ?sg . "
						+ "FILTER(regex(?t, \"^" + targetAuthority + "\")) "
						+ "}";
		logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + sourceQueryString);
		//		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sourceQueryString);
		QueryExecutionFactory sourceQEF = initializeQEF(sourceEndpoint);
		QueryExecution qexec = sourceQEF.createQueryExecution(sourceQueryString);
		ResultSet queryResults = qexec.execSelect(); 
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			RDFNode s = qs.get("?s");
			RDFNode sWKT = null;
			if(qs.get("?sg").isLiteral()){
				sWKT = qs.get("?sg");
			}else{
				String sGeo = qs.get("?sg").toString();
				String swkQueryString =
						"SELECT DISTINCT ?lat ?lon "
								+ "WHERE { "
								+ "<" + sGeo + "> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . "
								+ "<" + sGeo + "> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon . "
								+ "}";
				logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + swkQueryString);
				//				QueryExecution swkExec = QueryExecutionFactory.sparqlService(sourceEndpoint, swkQueryString);
				QueryExecution swkExec = sourceQEF.createQueryExecution(swkQueryString);
				ResultSet swkResults = swkExec.execSelect(); 
				String wktStr = "";
				while(swkResults.hasNext()){
					QuerySolution swkSol = swkResults.nextSolution();
					String lat = swkSol.get("?lat").toString();
					lat = lat.toString().substring(0, lat.indexOf("^^"));
					String lon = swkSol.get("?lon").toString();
					lon = lon.substring(0, lon.indexOf("^^"));
					wktStr += (wktStr.length() == 0)? "(" + lon + " " + lat + ")" : ",(" + lon + " " + lat + ")";
				}
				wktStr = (wktStr.contains(","))? "MULTIPOINT(" + wktStr + ")" : "POINT(" + wktStr + ")";
				sWKT = ResourceFactory.createPlainLiteral(wktStr);
				System.out.println(sWKT);
			}
			RDFNode t = qs.get("?t");
			if(!t.equals(null) && !targetAuthority.equals(null) && t.toString().startsWith(targetAuthority)){
				// read target polygons
				String targetQueryString =
						"SELECT DISTINCT ?tg "
								+ "WHERE { "
								+ "<" + t.toString() +"> <" + targetGeoPredicate+ "> ?tg . "
								+ "}";
				logger.info("Querying " + targetEndpoint  + " with SPARQL: " + targetQueryString);
				//				QueryExecution targetQExec = QueryExecutionFactory.sparqlService(targetEndpoint, targetQueryString);
				QueryExecutionFactory targetQEF = initializeQEF(targetEndpoint);
				QueryExecution targetQExec = targetQEF.createQueryExecution(sourceQueryString);
				ResultSet targetQueryResults = targetQExec.execSelect();
				while(targetQueryResults.hasNext()){
					QuerySolution tqs = targetQueryResults.nextSolution();
					RDFNode tWKT = tqs.get("?tg");
					if(tWKT != null){
						source.addTriple(s.toString(), sourceGeoPredicate, sWKT.toString());
						target.addTriple(t.toString(), targetGeoPredicate, tWKT.toString());	
					}
				}
				targetQExec.close() ;
			}
		}
		qexec.close() ;
	}

	public void readSourceTargetCacheLGD_vsparql(Resource className){
		source = new HybridCache();
		target = new HybridCache();
		// read source polygons
		String sourceQueryString =
				"SELECT DISTINCT ?s ?sg ?t "
						+ "WHERE { "
						+ "?s a <" + className.toString() +"> . "
						+ "?s <" + OWL.sameAs + "> ?t . "
						+ "?s " + sourceGeoPredicate + " ?sg . "
						+ "FILTER(regex(?t, \"^" + targetAuthority + "\")) "
						+ "} limit " + MAX_INSTACE_COUNT;
		logger.info("Querying " + sourceEndpoint + " with SPARQL: " + sourceQueryString);
		//		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sourceQueryString);
//		QueryExecutionFactory sourceQEF = initializeQEF("http://linkedgeodata.org/vsparql");
		QueryExecutionFactory sourceQEF = initializeQEF(sourceEndpoint);
		QueryExecution qexec = sourceQEF.createQueryExecution(sourceQueryString);
		ResultSet queryResults = qexec.execSelect(); 
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			RDFNode s = qs.get("?s");
			RDFNode sWKT = null;
			if(qs.get("?sg").isLiteral()){
				sWKT = qs.get("?sg");
			}else{
				String sGeo = qs.get("?sg").toString();
				String swkQueryString =
						"SELECT DISTINCT ?lat ?lon "
								+ "WHERE { "
								+ "<" + sGeo + "> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . "
								+ "<" + sGeo + "> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon . "
								+ "}";
				logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + swkQueryString);
				//				QueryExecution swkExec = QueryExecutionFactory.sparqlService(sourceEndpoint, swkQueryString);
				QueryExecution swkExec = sourceQEF.createQueryExecution(swkQueryString);
				ResultSet swkResults = swkExec.execSelect(); 
				String wktStr = "";
				while(swkResults.hasNext()){
					QuerySolution swkSol = swkResults.nextSolution();
					String lat = swkSol.get("?lat").toString();
					lat = lat.toString().substring(0, lat.indexOf("^^"));
					String lon = swkSol.get("?lon").toString();
					lon = lon.substring(0, lon.indexOf("^^"));
					wktStr += (wktStr.length() == 0)? "(" + lon + " " + lat + ")" : ",(" + lon + " " + lat + ")";
				}
				wktStr = (wktStr.contains(","))? "MULTIPOINT(" + wktStr + ")" : "POINT(" + wktStr + ")";
				sWKT = ResourceFactory.createPlainLiteral(wktStr);
				System.out.println("Source WKT = " + sWKT);
			}
			RDFNode t = qs.get("?t");
			if(!t.equals(null) && !targetAuthority.equals(null) && t.toString().startsWith(targetAuthority)){
				// read target polygons
				String targetQueryString =
						"SELECT DISTINCT ?tg "
								+ "WHERE { "
								+ "<" + t.toString() +"> <" + targetGeoPredicate+ "> ?tg . "
								+ "}";
				logger.info("Querying " + targetEndpoint  + " with SPARQL: " + targetQueryString);
				//				QueryExecution targetQExec = QueryExecutionFactory.sparqlService(targetEndpoint, targetQueryString);
				QueryExecutionFactory targetQEF = initializeQEF(targetEndpoint);
				QueryExecution targetQExec = targetQEF.createQueryExecution(targetQueryString);
				ResultSet targetQueryResults = targetQExec.execSelect();
				while(targetQueryResults.hasNext()){
					QuerySolution tqs = targetQueryResults.nextSolution();
					RDFNode tWKT = tqs.get("?tg");
					if(tWKT != null){
						System.out.println("Target WKT = " + tWKT.toString());
						source.addTriple(s.toString(), sourceGeoPredicate, sWKT.toString());
						target.addTriple(t.toString(), targetGeoPredicate, tWKT.toString());	
					}
				}
				targetQExec.close() ;
			}
		}
		qexec.close() ;
	}


	public void readSourceTargetCacheLGD(Resource className){
		source = new HybridCache();
		target = new HybridCache();
		// read source polygons
		String sourceQueryString =
				"SELECT DISTINCT ?s ?sg "
						+ "WHERE { "
						+ "?s a <" + className.toString() +"> . "
						//						+ "?s <" + OWL.sameAs + "> ?t . "
						+ "?s " + sourceGeoPredicate + " ?sg . "
						//						+ "FILTER(regex(?t, \"^" + targetAuthority + "\")) "
						+ "}";
		logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + sourceQueryString);
		//		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sourceQueryString);
		QueryExecutionFactory sourceQEF = initializeQEF(sourceEndpoint);
		QueryExecution qexec = sourceQEF.createQueryExecution(sourceQueryString);
		ResultSet queryResults = qexec.execSelect(); 
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			RDFNode lgd = qs.get("?s");
			RDFNode sWKT = null;
			if(qs.get("?sg").isLiteral()){
				sWKT = qs.get("?sg");
			}else{
				String sGeo = qs.get("?sg").toString();
				String swkQueryString =
						"SELECT DISTINCT ?lat ?lon "
								+ "WHERE { "
								+ "<" + sGeo + "> <http://www.w3.org/2003/01/geo/wgs84_pos#lat> ?lat . "
								+ "<" + sGeo + "> <http://www.w3.org/2003/01/geo/wgs84_pos#long> ?lon . "
								+ "}";
				logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + swkQueryString);
				//				QueryExecution swkExec = QueryExecutionFactory.sparqlService(sourceEndpoint, swkQueryString);
				QueryExecution swkExec = sourceQEF.createQueryExecution(swkQueryString);
				ResultSet swkResults = swkExec.execSelect(); 
				String wktStr = "";
				while(swkResults.hasNext()){
					QuerySolution swkSol = swkResults.nextSolution();
					String lat = swkSol.get("?lat").toString();
					lat = lat.toString().substring(0, lat.indexOf("^^"));
					String lon = swkSol.get("?lon").toString();
					lon = lon.substring(0, lon.indexOf("^^"));
					wktStr += (wktStr.length() == 0)? "(" + lon + " " + lat + ")" : ",(" + lon + " " + lat + ")";
				}
				wktStr = (wktStr.contains(","))? "MULTIPOINT(" + wktStr + ")" : "POINT(" + wktStr + ")";
				sWKT = ResourceFactory.createPlainLiteral(wktStr);
				System.out.println(sWKT);
			}
			//			RDFNode t = qs.get("?t");
			//			if(!targetAuthority.equals(null) && t.toString().startsWith(targetAuthority)){
			// read target polygons
			String targetQueryString =
					"SELECT DISTINCT ?tg ?dbp "
							+ "WHERE { "
							+ "?dbp <" + OWL.sameAs + "> <" + lgd.toString() + "> . "
							+ "?dbp <" + targetGeoPredicate+ "> ?tg . "
							+ "}";
			logger.info("Querying " + targetEndpoint  + " with SPARQL: " + targetQueryString);
			//				QueryExecution targetQExec = QueryExecutionFactory.sparqlService(targetEndpoint, targetQueryString);
			QueryExecutionFactory targetQEF = initializeQEF(targetEndpoint);
			QueryExecution targetQExec = targetQEF.createQueryExecution(sourceQueryString);
			ResultSet targetQueryResults = targetQExec.execSelect();
			while(targetQueryResults.hasNext()){
				QuerySolution tqs = targetQueryResults.nextSolution();
				RDFNode dbp = qs.get("?dbp");
				if(dbp == null){
					break;
				}
				RDFNode tWKT = tqs.get("?tg");
				if(tWKT != null){
					source.addTriple(lgd.toString(), sourceGeoPredicate, sWKT.toString());
					target.addTriple(dbp.toString(), targetGeoPredicate, tWKT.toString());	
				}
			}
			targetQExec.close() ;
			//			}
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


	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#generateResultsDataCube(com.hp.hpl.jena.rdf.model.Model)
	 */
	@Override
	public Model generateResultsDataCube(Model inputModel) {
		// TODO Auto-generated method stub
		return null;
	}


	public static void main(String[] args) throws IOException {
		if(args.length < 2){
			logger.error("Parameters: LGD/GLD/NUTS outFile [MAX_CLASS_COUNT] [MAX_INSTACE_COUNT]");
			System.exit(1);
		}
		if(args.length > 2){
			MAX_CLASS_COUNT = Integer.parseInt(args[2]);
		}
		if(args.length > 3){
			MAX_INSTACE_COUNT = Integer.parseInt(args[3]);
		}
		try {
			PatternLayout layout = new PatternLayout("%d{dd.MM.yyyy HH:mm:ss} %-5p [%t] %l: %m%n");
			String filename = args[1].substring(0, args[0].lastIndexOf(".")) + ".log";
			System.out.println("Generating logger file: " + filename);
			FileAppender fileAppender = new FileAppender(layout, filename, false);
			fileAppender.setLayout(layout);
			logger.addAppender(fileAppender);
		} catch (Exception e) {
			logger.warn("Exception creating file appender.");
			System.out.println(e);
		}
		logger.setLevel(Level.INFO);
		
		if(args[0].equalsIgnoreCase("LGD")){
			evaluate_lgd(args);
		}else if(args[0].equalsIgnoreCase("GLD")){
			evaluate_gld(args);
		}else if(args[0].equalsIgnoreCase("NUTS")){
			evaluate_nuts(args);
		}else{
			logger.error(args[0] + " not implemented yet");
		}

	}


	/**
	 * @param args
	 * @author sherif
	 */
	private static void evaluate_nuts(String[] args) {
		// TODO Auto-generated method stub

	}


	/**
	 * @param args
	 * @author sherif
	 * @throws IOException 
	 */
	private static void evaluate_gld(String[] args) throws IOException {
		AveragePolygonsMetric m = new AveragePolygonsMetric(
				"http://geo.linkeddata.es/sparql",
				"http://geo.linkeddata.es/",
				"<http://www.w3.org/2003/01/geo/wgs84_pos#geometry>",
				"http://dbpedia.org/sparql",
				"http://dbpedia.org/",
				"http://www.w3.org/2003/01/geo/wgs84_pos#geometry");

		Model resultModel = m.generateResultsDataCube("");
		resultModel.write(System.out, "TTL");
		logger.info("Saving dataset to " + args[1] + "...");
		long starTime = System.currentTimeMillis();
		FileWriter fileWriter = new FileWriter(args[1]);
		resultModel.write(fileWriter, "TTL");
		logger.info("Saving file done in " + (System.currentTimeMillis() - starTime) +"ms.");

	}


	/**
	 * @param args
	 * @author sherif
	 * @throws IOException 
	 */
	private static void evaluate_lgd(String[] args) throws IOException {
		AveragePolygonsMetric m = new AveragePolygonsMetric(
//				"http://linkedgeodata.org/sparql",
								"http://linkedgeodata.org/vsparql",
				"http://linkedgeodata.org/ontology/",
				"<http://geovocab.org/geometry#geometry> ?x . ?x <http://www.opengis.net/ont/geosparql#asWKT>",
				"http://dbpedia.org/sparql",
				"http://dbpedia.org/",
				"http://www.w3.org/2003/01/geo/wgs84_pos#geometry");

		Model resultModel = m.generateResultsDataCube("");
		resultModel.write(System.out, "TTL");
		logger.info("Saving dataset to " + args[1] + "...");
		long starTime = System.currentTimeMillis();
		FileWriter fileWriter = new FileWriter(args[1]);
		resultModel.write(fileWriter, "TTL");
		logger.info("Saving file done in " + (System.currentTimeMillis() - starTime) +"ms.");

	}


}
