/**
 * 
 */
package org.aksw.geoknow.assessment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author sherif
 *
 */
public class AveragePolygonsMetric implements GeoQualityMetric {

	public static List<Property> geoPredicates = new ArrayList<Property>(
			Arrays.asList(ResourceFactory.createProperty("http://www.w3.org/2003/01/geo/wgs84_pos#geometry")));

	public void addGeoPredicate(String uri){
		geoPredicates.add(ResourceFactory.createProperty(uri));
	}

	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#run(com.hp.hpl.jena.rdf.model.Model)
	 */
	public Model generateResultsDataCube(Model inputModel) {
		List<Resource> classes = getClasses(inputModel);
		for(Resource c : classes){
			List<Resource> instances = getGeoInstances(inputModel, c);
			for(Resource r : instances){
				
			}
		}
		return null;
	}



	/* (non-Javadoc)
	 * @see org.aksw.geoknow.assessment.GeoQualityMetric#run(java.lang.String)
	 */
	public Model generateResultsDataCube(String endpointUrl) {
		// TODO Auto-generated method stub
		return null;
	}


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

	public List<Resource> getGeoInstances(Model m, Resource c){
		List<Resource> results = new ArrayList<Resource>();
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

	public List<Resource> getCoordinates(Model m, Resource r){
		List<Resource> results = new ArrayList<Resource>();
		for(Property p : geoPredicates){
			String sparqlQueryString = 
					"SELECT DISTINCT ?s " +
							"{?s <" + p.toString() +"> ?o. " +
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
	 * @param args
	 * @author sherif
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
