/**
 * 
 */
package org.aksw.geoknow.assessment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public static List<Property> geoPredicates = new ArrayList<Property>(
			Arrays.asList(ResourceFactory.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#geometry")));

	private String sourceEndpoint = "http://live.dbpedia.org/sparql";
	private String targetEndpoint = "http://linkedgeodata.org/sparql";
	public String targetAuthority;
	private String sourceGeoPredicate =  "<http://www.w3.org/2003/01/geo/wgs84_pos#geometry>";
	private String targetGeoPredicate =  "<http://geovocab.org/geometry#geometry>/<http://www.opengis.net/ont/geosparql#asWKT>";

	private int MAX_CLASS_COUNT = 30;
	public static Cache source = new HybridCache();
	public static Cache target = new HybridCache();


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
		sourceEndpoint = endpointUrl;
		Map<Resource, Double> d = computePerClassAverageDistance("geomin", 0.7);
		System.out.println(d);
		return null;
	}

	public Map<Resource, Double> computePerClassAverageDistance(String measureType, double threshold) {
		Map<Resource, Double> result = new HashMap<Resource, Double>();
		List<Resource> classes = getSourceClasses();
		int i = 0;
		for(Resource c : classes){
			logger.info("----- " + c.toString() + " Class -----");
			readSourceTargetCache(c);
			Mapping m = getMapping(threshold, measureType);
			Double avg = computeAverageSimilarity(m);
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
		//		List<Resource> classes = getClasses(inputModel);
		//		for(Resource c : classes){
		//			List<Resource> instances = getGeoInstances(inputModel, c);
		//			for(Resource r : instances){
		//				List<Resource> sourcePointSet = getPointSet(inputModel, r);
		//				if(!sourcePointSet.isEmpty()){
		//					List<Resource> sameAsInstances = getSameAsInstances(inputModel, r);
		//					for(Resource t : sameAsInstances){
		//						List<Resource> targetPointSet = getPointSet(inputModel, t); //TODO find a way to read the target dataset
		//						if(!targetPointSet.isEmpty()){
		//							// Compute point set distance
		//						}
		//					}
		//				}
		//
		//			}
		//		}
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
		//		System.out.println(m.getSourceTargetWKT(ResourceFactory.createResource("http://dbpedia.org/ontology/AdministrativeRegion")));
		m.targetAuthority = "http://linkedgeodata.org";
		m.generateResultsDataCube("http://dbpedia.org/sparql");
		//		m.readSourceTargetCache(ResourceFactory.createResource("http://dbpedia.org/ontology/AdministrativeRegion"));
		//		System.out.println(AveragePolygonsMetric.source.toString());
		//		System.out.println(AveragePolygonsMetric.target.toString());
	}



}
