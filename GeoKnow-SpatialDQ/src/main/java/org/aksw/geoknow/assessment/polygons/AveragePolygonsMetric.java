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
import com.hp.hpl.jena.rdf.model.Literal;
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
 * @author sherif
 *
 */
public class AveragePolygonsMetric implements GeoQualityMetric {
	private static final Logger logger = Logger.getLogger(AveragePolygonsMetric.class.getName());

	private static final double DISTANCE_THRESHOULD = 0.7;
	private int MAX_CLASS_COUNT = 30;

	public static List<Property> geoPredicates = new ArrayList<Property>(
			Arrays.asList(ResourceFactory.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#geometry")));

	private String sourceEndpoint = "http://live.dbpedia.org/sparql";
	private String targetEndpoint = "http://linkedgeodata.org/sparql";
	public  String targetAuthority;
	private String sourceGeoPredicate =  "<http://www.w3.org/2003/01/geo/wgs84_pos#geometry>";
	private String targetGeoPredicate =  "<http://geovocab.org/geometry#geometry>/<http://www.opengis.net/ont/geosparql#asWKT>";
	private static Cache source = new HybridCache();
	private static Cache target = new HybridCache();

	public String baseUri = "http://www.geoknow.eu/";

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

	/**
	 * @return the geoPredicates
	 */
	public static List<Property> getGeoPredicates() {
		return geoPredicates;
	}

	/**
	 * @param geoPredicates the geoPredicates to set
	 */
	public static void setGeoPredicates(List<Property> geoPredicates) {
		AveragePolygonsMetric.geoPredicates = geoPredicates;
	}

	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#generateResultsDataCube(java.lang.String)
	 */
	public Model generateResultsDataCube(String endpointUrl) {
		DataCubeWriter dataCube = new DataCubeWriter();

		// DataSet
		//	<http://www.geoknow.eu/dataset/ds1> a qb:DataSet ;
		//		dcterms:publisher "AKSW, GeoKnow" ;
		//		rdfs:label "DataCube1 Results: Normal vs. Outlier Instances" ;
		//		rdfs:comment "DataCube1 Results: Normal vs. Outlier Instances" ;
		//		qb:structure <http://www.geoknow.eu/data-cube/dsd1> ;
		//		dcterms:date "Mon Jun 30 16:37:52 CEST 2014". 
		Resource dataset = ResourceFactory.createResource(baseUri + "avg_poly_dataset");
		String dLabel = "Per class average polygons distances";
		String dComment = "Average distance between polygons which represent the same resource";
		String dPublisher = "AKSW, GeoKnow";
		String dDate = (new Date()).toString();
		Resource dsDef = ResourceFactory.createResource(baseUri + "avg_poly_data_struct_def");
		dataCube.addDataset(dataset, dLabel, dComment, dPublisher, dDate, dsDef);

		// Data Structure Definitions
		//	<http://www.geoknow.eu/data-cube/dsd1> a qb:DataStructureDefinition ;
		//		rdfs:label "A Data Structure Definition"@en ;
		//		rdfs:comment "A Data Structure Definition for DataCube1" ;
		//		qb:component <http://www.geoknow.eu/data-cube/dsd1/c1>,
		//		<http://www.geoknow.eu/data-cube/dsd1/c2>,
		//		<http://www.geoknow.eu/data-cube/dsd1/c3>,
		//		<http://www.geoknow.eu/data-cube/dsd1/c4> .
		String dsLabel = "A Data Structure Definition";
		String dsComment = "A Data Structure Definition for " + dataset;
		Resource compCls = ResourceFactory.createResource(baseUri + "avg_poly_comp_cls");
		Resource compTime  = ResourceFactory.createResource(baseUri + "avg_poly_comp_time");
		Resource compDist  = ResourceFactory.createResource(baseUri + "avg_poly_comp_dist");
		Set<Resource> dsComponents = new HashSet<Resource>(Arrays.asList(compCls, compTime,compDist));
		dsComponents.add(ResourceFactory.createResource(baseUri + "avg_poly_comp_time"));
		dataCube.addDataStructureDefinition(dsDef, dsLabel, dsComment , dsComponents);

		//	Component Specifications
		// <http://www.geoknow.eu/data-cube/dsd1/c1> a qb:ComponentSpecification ;
		//		 rdfs:label "Component Specification of IsOutlier " ;
		//		 qb:dimension gk-dim:InstanceType . 
		String clsLabel = "Component Specification of Class";
		Property clsProperty = ResourceFactory.createProperty(baseUri + "avg_poly_cls_prop");
		dataCube.addDimensionProperty(clsProperty, "Class Name");
		dataCube.addDimensionSpecs(compCls, clsLabel, clsProperty);

		String timeLabel = "Component Specification of Time Stamp";
		Property timeProperty = ResourceFactory.createProperty(baseUri + "avg_poly_time_prop");
		dataCube.addDimensionProperty(timeProperty, "Time Stamp");
		dataCube.addDimensionSpecs(compTime, timeLabel, timeProperty);

		String metricLabel = "Component Specification of Polygon Metric";
		Property metricProperty = ResourceFactory.createProperty(baseUri + "avg_poly_metric_prop");
		dataCube.addDimensionProperty(timeProperty, "Polygon Metric");
		dataCube.addDimensionSpecs(compTime, metricLabel, metricProperty);

		// <http://www.geoknow.eu/data-cube/dsd1/c4> a qb:ComponentSpecification ;
		//		rdfs:label "Component Specification of Instance" ;
		//		qb:measure sdmx-measure:InstanceCount .
		String distLabel = "Average Polygon Distance";
		Property distProperty = ResourceFactory.createProperty(baseUri + "avg_poly_dist_prop");
		dataCube.addMeasureProperty(distProperty, distLabel);
		dataCube.addMeasureSpecs(compDist, distLabel, distProperty);

		// Observations
		int i = 1, j = 1; 
		for(String pm : polygonMertices ){
			sourceEndpoint = endpointUrl;
			Map<Resource, Double> avgDists = computePerClassAverageDistance(pm);
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + avgDists.size());System.out.println(avgDists);
			for(Resource c : avgDists.keySet()){
				Resource observation = ResourceFactory.createResource(baseUri + "avg_poly_obv_" + i + "_" + j++);
				DataCubeWriter.dataCubeModel.add(observation, RDF.type, QB.Observation);
				DataCubeWriter.dataCubeModel.add(observation, QB.dataset, dataset);
				DataCubeWriter.dataCubeModel.add(observation, clsProperty, c);
				DataCubeWriter.dataCubeModel.add(observation, timeProperty, (new Date()).toString());
				DataCubeWriter.dataCubeModel.add(observation, metricProperty, pm);
				DataCubeWriter.dataCubeModel.add(observation, distProperty, avgDists.get(pm) + "");
			}
			j++;
		}
		return dataCube.getDataCubeModel();
	}

	public Map<Resource, Double> computePerClassAverageDistance(String measureType) {
		Map<Resource, Double> result = new HashMap<Resource, Double>();
		List<Resource> classes = getSourceClasses();
		int i = 0;
		for(Resource c : classes){
			logger.info("----- " + c.toString() + " Class -----");
			readSourceTargetCache(c);
			Mapping m = getMapping(DISTANCE_THRESHOULD, measureType);
			Double avg = computeAverageSimilarity(m);
			System.out.println(avg);
			if(!avg.isNaN()){
				result.put(c, avg);	
			}
			if(i++ == MAX_CLASS_COUNT){
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


	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#run(com.hp.hpl.jena.rdf.model.Model)
	 */
	public Model generateResultsDataCube(Model inputModel) {
		return null;
	}

	public Mapping getMapping(double threshold, String expression){
		//get sets of polygons from properties

		OrchidMapper mapper = new OrchidMapper();
		//		mapper.getMapping(source, target, "", "", expression, threshold);
		Set<Polygon> sourcePolygons = mapper.getPolygons(source, sourceGeoPredicate);
		Set<Polygon> targetPolygons = mapper.getPolygons(target, targetGeoPredicate);
		float theta = (1 / (float) threshold) - 1;
		Type type = mapper.getTypeFromExpression(expression);
		GeoHR3 orchid = new GeoHR3(theta, GeoHR3.DEFAULT_GRANULARITY, type);
		return orchid.run(sourcePolygons, targetPolygons);
	}




	/**
	 * @param m
	 * @return List of classes included in the input model m
	 * @author sherif
	 */
	public List<Resource> getClasses(Model m){
		List<Resource> results = new ArrayList<Resource>();
		String sparqlQueryString = "SELECT DISTINCT ?c { ?s a ?c.}";
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			results.add(qs.getResource("?c"));
		}
		qexec.close() ;
		return results;
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

		logger.info("Querying " + sourceEndpoint  + " with SPARQL: " + sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sparqlQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			results.add(qs.getResource("?c"));
		}
		qexec.close() ;
		return results;
	}

	/**
	 * @param m
	 * @param c
	 * @return set of Geo-instances from class c within the input model m,
	 * Geo-instances are all instance with at least one geoPredicates
	 * @author sherif
	 */
	public Set<Resource> getGeoInstances(Model m, Resource c){
		Set<Resource> results = new HashSet<Resource>();
		for(Property p : geoPredicates){
			String sparqlQueryString = 
					"SELECT DISTINCT ?s " +
							"{ ?s a <" + c.toString() + ">. " +
							"?s <" + p.toString() +"> ?o. " +
							"?s <" + OWL.sameAs + "> ?q}";
			QueryFactory.create(sparqlQueryString);
			QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
			ResultSet queryResults = qexec.execSelect();
			while(queryResults.hasNext()){
				QuerySolution qs = queryResults.nextSolution();
				results.add(qs.getResource("?s"));
			}
			qexec.close() ;
		}
		return results;
	}

	/**
	 * @param m
	 * @param r
	 * @return
	 * @author sherif
	 */
	public List<Resource> getPointSet(Model m, Resource r){
		List<Resource> results = new ArrayList<Resource>();
		for(Property p : geoPredicates){
			String sparqlQueryString = 
					"SELECT DISTINCT ?o " +
							"{<" + r.toString() + "> <" + p.toString() +"> ?o. }";
			QueryFactory.create(sparqlQueryString);
			QueryExecution qexec = QueryExecutionFactory.create(sparqlQueryString, m);
			ResultSet queryResults = qexec.execSelect();
			while(queryResults.hasNext()){
				QuerySolution qs = queryResults.nextSolution();
				results.add(qs.getResource("?o"));
			}
			qexec.close() ;
			if(results.size() > 0){
				return results;
			}
		}
		return results;
	}

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


	public Map<Literal,Literal> getSourceTargetWKT(Resource className){
		Map<Literal,Literal> results = new HashMap<Literal, Literal>();
		String sparqlQueryString =
				"SELECT DISTINCT ?s ?sg ?t ?tg "
						+ "WHERE { "
						+ "?s a <" + className.toString() +"> . "
						+ "?s <" + OWL.sameAs + "> ?t . ";
		for(Property p : geoPredicates){
			sparqlQueryString += "{ ?s <" + p.toString()+ "> ?sg. } UNION ";
		}
		sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length()-6);
		for(Property p : geoPredicates){
			sparqlQueryString += "{ ?t <" + p.toString()+ "> ?tg. } UNION ";
		}
		sparqlQueryString = sparqlQueryString.substring(0, sparqlQueryString.length()-6);
		sparqlQueryString += " }";
		System.out.println(sparqlQueryString);
		logger.info("Reading class  " + className + " from " + sourceEndpoint  + " using SPARQL: " + sparqlQueryString);
		QueryFactory.create(sparqlQueryString);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(sourceEndpoint, sparqlQueryString);
		ResultSet queryResults = qexec.execSelect();
		while(queryResults.hasNext()){
			QuerySolution qs = queryResults.nextSolution();
			System.out.println(qs.getResource("?s").toString() + qs.getLiteral("?sg"));
			System.out.println(qs.getResource("?t").toString() + qs.getLiteral("?tg"));
			results.put(qs.getLiteral("?sg"), qs.getLiteral("?tg"));
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
						+ "?s " + sourceGeoPredicate+ " ?sg . "
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
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		AveragePolygonsMetric m = new AveragePolygonsMetric();
		System.out.println(m.getSourceTargetWKT(ResourceFactory.createResource("http://dbpedia.org/ontology/AdministrativeRegion")));
		m.targetAuthority = "http://linkedgeodata.org";
		m.generateResultsDataCube("http://dbpedia.org/sparql").write(System.out, "TTL");
//		m.readSourceTargetCache(ResourceFactory.createResource("http://dbpedia.org/ontology/AdministrativeRegion"));
//		System.out.println(AveragePolygonsMetric.source.toString());
//		System.out.println(AveragePolygonsMetric.target.toString());
	}



}
