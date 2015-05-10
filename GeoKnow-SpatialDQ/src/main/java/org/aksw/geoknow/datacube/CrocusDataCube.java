
package org.aksw.geoknow.datacube;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
/**
 * Data Cubes generator of CROCUS results
 * @author Saleem
 *
 */
public class CrocusDataCube {
	public static BufferedWriter bw ; 
	public static String clsName=null;
	public static String endpoint =null; 
	public static String timeStamp =new Date().toString(); 
	public CrocusDataCube(String endpoint,String clsName, String tStmp)
	{
		CrocusDataCube.clsName = clsName;
		CrocusDataCube.endpoint=endpoint;
		CrocusDataCube.timeStamp=tStmp;
	}
	public static void main(String[] args) throws IOException, MalformedQueryException, QueryEvaluationException, RepositoryException {
		System.out.println("Cube Generation Started ...");
		CrocusDataCube.generateDataCubes("crocusResults.rdf");
      
	}
/**
 * Write CROCUS results into a file
 * @param crocusResults The results obtained by the client
 * @throws IOException
 * @throws MalformedQueryException
 * @throws QueryEvaluationException
 */
	public static void writeCROCUSResults(String crocusResults) throws IOException, MalformedQueryException, QueryEvaluationException {
			BufferedWriter bw= new BufferedWriter(new FileWriter(new File("crocusResults.rdf")));
			bw.write(crocusResults);
			bw.close();
				}
/**
 * Converst CROCUS results in to data cubes
 * @param crocusResults CROCUS results entity body
 * @throws IOException
 * @throws MalformedQueryException
 * @throws QueryEvaluationException
 * @throws RepositoryException
 */
	public static void generateDataCubes(String crocusResults) throws IOException, MalformedQueryException, QueryEvaluationException, RepositoryException {
		writeCROCUSResults(crocusResults);
		ResultsLoader.loadResults("crocusResults.rdf");
		System.out.println("CROCUS results loaded to in-memory");
		bw= new BufferedWriter(new FileWriter(new File("DataCubeResults.n3")));
		writePrefixes();
	    writeDimesnions();
	    writeObservations();
	    System.out.println("Results successfully written to DataCubeResults.n3");
		bw.close();
	}
/**
 * Write Datacubes observations
 * @throws IOException
 * @throws RepositoryException
 * @throws MalformedQueryException
 * @throws QueryEvaluationException
 */
	private static void writeObservations() throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException {
		  String cb1_2Query = getCube1_2Query();
		  TupleQuery tupleQuery = ResultsLoader.con.prepareTupleQuery(QueryLanguage.SPARQL, cb1_2Query);
		  TupleQueryResult res = tupleQuery.evaluate();
		  long obsNo = 1,outlierCount = 0, trustedCount = 0,instancePropsCount=0;
		  String trustVal, instanceClass = null, instance=null; 
		   while(res.hasNext())
		   {
			   BindingSet result = res.next();
			   trustVal = result.getValue("trust").stringValue();
			   instanceClass = result.getValue("uriType").stringValue();
			   if(trustVal.equals("true"))
				   trustedCount++;
			   else
				   outlierCount++;
			      
			       instance = result.getValue("uri").stringValue();
             //----------------------Write observations for the data cube2 =  Instance, IsOutlier, Class, TimeSamp, PropsCount ----------------
				   bw.write("<http://www.geoknow.eu/data-cube/dsd2/obs"+obsNo+"> qb:dataSet <http://www.geoknow.eu/dataset/ds2> ; \n"
						+ "	gk-dim:Instance <"+instance+"> ;\n");
					if(trustVal.equals("true"))
						bw.write( "	gk-dim:InstanceType \"Normal\" ;\n");
					else
					bw.write( "	gk-dim:InstanceType \"Outlier\" ;\n");	 
						
					instancePropsCount = getPropsCount(instance,instanceClass);
						bw.write( "	gk-dim:Class <"+instanceClass+"> ;\n"
						 + "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
						 + "	sdmx-measure:PropsCount "+instancePropsCount+" ;\n"
						 + "	a qb:Observation .\n\n");
			  // System.out.println(res.next()); 
			   obsNo++;
		   }
		   System.out.println("DataCube 2 is successfully written...");
		   //------------------------Write observations for the data cube1 =  Outlier, Class, TimeSamp, Count -------------------
		   		bw.write("<http://www.geoknow.eu/data-cube/dsd1/obs1> qb:dataSet <http://www.geoknow.eu/dataset/ds1> ; \n"
				+ "	gk-dim:InstanceType \"Outlier\";\n"
	 			+ "	gk-dim:Class <"+instanceClass+"> ;\n"
	 			+ "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
	 			+ "	sdmx-measure:InstanceCount "+outlierCount+" ;\n"
	 			+ "	a qb:Observation .\n\n");
				bw.write("<http://www.geoknow.eu/data-cube/dsd1/obs2> qb:dataSet <http://www.geoknow.eu/dataset/ds1> ; \n"
				 + "	gk-dim:InstanceType \"Normal\";\n"
				 + "	gk-dim:Class <"+instanceClass+"> ;\n"
				 + "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
				 + "	sdmx-measure:InstanceCount "+trustedCount+" ;\n"
				 + "	a qb:Observation .\n\n");
				System.out.println("DataCube 1 is successfully written...");
			//---------Write final data cube3  regarding properties stats i.e no. of subject, objects for each distinct property-------------	
				writePropsStats();
				System.out.println("DataCube 3 is successfully written...");
	}
/**
 * Write datacube no.3 about properties statistics
 * @throws IOException
 * @throws RepositoryException
 * @throws QueryEvaluationException
 * @throws MalformedQueryException
 */
	private static void writePropsStats() throws IOException, RepositoryException, QueryEvaluationException, MalformedQueryException {
		String query = getDistPrdQry();
		 String repositoryID = "example-db";
		 Repository repo = new HTTPRepository(endpoint, repositoryID);
		  repo.initialize();
		  TupleQuery tupleQuery =repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL,query);
		  TupleQueryResult res = tupleQuery.evaluate();
		  long obsNo = 1;
		  while(res.hasNext())
		   {
			  String property =res.next().getBinding("p").getValue().stringValue() ; 
			  bw.write("<http://www.geoknow.eu/data-cube/dsd3/obs"+obsNo+"> qb:dataSet <http://www.geoknow.eu/dataset/ds3> ; \n");
					  	bw.write( "	gk-dim:Property <"+property+"> ;\n");	 
						bw.write( "	gk-dim:Class <"+clsName+"> ;\n"
						 + "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
						 + "	sdmx-measure:ObjectsCount "+getObjCount(property)+" ;\n"
						 + "	sdmx-measure:SubjectsCount "+getSbjCount(property)+" ;\n"
						 + "	a qb:Observation .\n\n"	);
			  // System.out.println(res.next()); 
			   obsNo++;
		   }
		
	}
	/**
	 * Get total number of distinct subjects for given predicate
	 * @param property Predicate
	 * @return count Count 
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	private static String getSbjCount(String property) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		String count = null;
		String query = "SELECT (count(DISTINCT ?o) as ?cnt)  WHERE { ?s a <"+clsName+">. ?s <"+property+"> ?o}";
		 String repositoryID = "example-db";
		 Repository repo = new HTTPRepository(endpoint, repositoryID);
		  repo.initialize();
		  TupleQuery tupleQuery =repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL,query);
		  TupleQueryResult res = tupleQuery.evaluate();
		  while (res.hasNext())
		  {
			 count= res.next().getBinding("cnt").getValue().stringValue();
		  }
		return count;
	}
	/**
	 * Get total number of distinct objects for given predicate
	 * @param property Predicate
	 * @return count Count 
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	private static String getObjCount(String property) throws RepositoryException, QueryEvaluationException, MalformedQueryException {
		String count = null;
		String query = "SELECT (count(DISTINCT ?s) as ?cnt)  WHERE { ?s a <"+clsName+">. ?s <"+property+"> ?o}";
		 String repositoryID = "example-db";
		 Repository repo = new HTTPRepository(endpoint, repositoryID);
		  repo.initialize();
		  TupleQuery tupleQuery =repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL,query);
		  TupleQueryResult res = tupleQuery.evaluate();
		  while (res.hasNext())
		  {
			 count= res.next().getBinding("cnt").getValue().stringValue();
		  }
		return count;
	}
	/**
	 * Get distinct predicates SPARQL query
	 * @return query SPARQL query
	 */
	private static String getDistPrdQry() {
		String query = "Select DISTINCT ?p WHERE {?s a <"+clsName+">. ?s ?p ?o} ";
		return query;
	}
	/**
	 * Get the distinct no. of properties for the given instance and class
	 * @param instance Instance
	 * @param cls Class
	 * @return count Count 
	 * @throws RepositoryException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 */
	private static long getPropsCount(String instance,String cls) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		long count = 0 ;
		 String propsQry = getInstancePropsCountQuery(instance,cls);
		 String repositoryID = "example-db";
		 Repository repo = new HTTPRepository(endpoint, repositoryID);
		  repo.initialize();
		  TupleQuery tupleQuery =repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, propsQry);
		  TupleQueryResult res = tupleQuery.evaluate();
		  while(res.hasNext())
		   {
			  res.next();
			  count++;
		   }
		return count;
	}
/**
 * Get SPARQL query for the no. of distinct properties of an instance
 * @param instance Instance
 * @param cls Class of instance 
 * @return query Sparql query
 */
	private static String getInstancePropsCountQuery(String instance, String cls) {
		String query = "SELECT Distinct ?p  where"
				+ "{"
				+ " <"+instance+"> a <"+cls+"> ."
				+ " <"+instance+"> ?p ?o ."
				+ "}";
		return query;
	}
/**
 * Get SPARQL query for first and second data cube
 * @return query SPARQL query
 */
	private static String getCube1_2Query() {
		String query = "SELECT Distinct ?uri ?uriType ?timeStamp ?trust where"
				+ "{"
				+ "?s <http://base.org/origin_uri> ?uri."
				+ "?s <http://base.org//origin_type> ?uriType."
				+ "?s <http://base.org/timestamp> ?timeStamp."
				+ "?s <http://base.org/trusted> ?trust."
				+ "}";
		return query;
	}
/**
 * Write dimensions of the data cubes
 * @throws IOException
 */
	private static void writeDimesnions() throws IOException {
		Date date = new Date();
		  bw.write( "<> a owl:Ontology ; \n"
		  	+ " rdfs:label \"GeoKnow Spatical Data Qaluty DataCube Knowledge Base\" ;\n"
		  	+ " dc:description \"This knowledgebase contains 3 different DataCubes with different dimensions and measures.\" .\n\n"
	//-----------------------Dataset----------------------------
		  	+ "#\n #Data Set \n # \n"
		  	+ "<http://www.geoknow.eu/dataset/ds1>  a qb:DataSet ;\n"
			+ "	       dcterms:publisher \"AKSW, GeoKnow\" ; \n"
			+ "	       rdfs:label \"DataCube1 Results:  Normal vs. Outlier Instances\" ; \n"
			+ "	       rdfs:comment \"DataCube1 Results:  Normal vs. Outlier Instances\" ; \n"
			+ "	       qb:structure <http://www.geoknow.eu/data-cube/dsd1> ;\n"
			+ "	       dcterms:date \""+date+"\". \n\n"
		  	+ "<http://www.geoknow.eu/dataset/ds2>  a qb:DataSet ;\n"
			+ "	       dcterms:publisher \"AKSW, GeoKnow\" ; \n"
			+ "	       rdfs:label \"DataCube2 Results:  Instances Information with Properties count\" ; \n"
			+ "	       rdfs:comment \"DataCube2 Results:  Instances Informaton with Properties count\" ; \n"
			+ "	       qb:structure <http://www.geoknow.eu/data-cube/dsd2> ;\n"
			+ "	       dcterms:date \""+date+"\". \n\n"		
		  	+ "<http://www.geoknow.eu/dataset/ds3>  a qb:DataSet ;\n"
			+ "	       dcterms:publisher \"AKSW, GeoKnow\" ; \n"
			+ "	       rdfs:label \"DataCube3 Results:  Properties Information\" ; \n"
			+ "	       rdfs:comment \"DataCube3 Results:  Properties Informaton\" ; \n"
			+ "	       qb:structure <http://www.geoknow.eu/data-cube/dsd3> ;\n"
			+ "	       dcterms:date \""+date+"\". \n\n"	
  //-----------------Data Cube 1 Structure Definitions-----------------------------------------
			+ "# \n# Data Structure Definitions \n # \n"
			+ "<http://www.geoknow.eu/data-cube/dsd1> a qb:DataStructureDefinition ; \n"
			+ "                                       rdfs:label   \"A Data Structure Definition\"@en ;\n"
			+ "                                       rdfs:comment  \"A Data Structure Definition for DataCube1\" ;\n"
			+ "                                       qb:component <http://www.geoknow.eu/data-cube/dsd1/c1>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd1/c2>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd1/c3>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd1/c4> . \n\n"
			+ "<http://www.geoknow.eu/data-cube/dsd2> a qb:DataStructureDefinition ; \n"
			+ "                                       rdfs:label   \"A Data Structure Definition\"@en ;\n"
			+ "                                       rdfs:comment  \"A Data Structure Definition for DataCube2\" ;\n"
			+ "                                       qb:component <http://www.geoknow.eu/data-cube/dsd2/c1>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd2/c2>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd2/c3>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd2/c4>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd2/c5> . \n\n"	
			+ "<http://www.geoknow.eu/data-cube/dsd3> a qb:DataStructureDefinition ; \n"
			+ "                                       rdfs:label   \"A Data Structure Definition\"@en ;\n"
			+ "                                       rdfs:comment  \"A Data Structure Definition for DataCube3\" ;\n"
			+ "                                       qb:component <http://www.geoknow.eu/data-cube/dsd3/c1>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd3/c2>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd3/c3>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd3/c4>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd3/c5> . \n\n"	
	//-----------------------Component Specifications-------------------------------------------------------------------
			+ " # \n #Componenet Specifications\n #\n "		
			//-------------DataCube1------------------------------------
			+ "<http://www.geoknow.eu/data-cube/dsd1/c1> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of IsOutlier \" ;\n"
			+ "                                          qb:dimension gk-dim:InstanceType . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd1/c2> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Class\" ;\n"
			+ "                                          qb:dimension gk-dim:Class . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd1/c3> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Time Stamp\" ;\n"
			+ "                                          qb:dimension gk-dim:TimeStamp . \n"	
			+ "<http://www.geoknow.eu/data-cube/dsd1/c4> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Instance\" ;\n"
			+ "                                          qb:measure sdmx-measure:InstanceCount . \n\n"				
			//-------------DataCube2------------------------------------
			+ "<http://www.geoknow.eu/data-cube/dsd2/c1> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Instance\" ;\n"
			+ "                                          qb:dimension gk-dim:Instance . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd2/c2> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of IsOutlier \" ;\n"
			+ "                                          qb:dimension gk-dim:InstanceType . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd2/c3> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Class\" ;\n"
			+ "                                          qb:dimension gk-dim:Class . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd2/c4> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Time Stamp\" ;\n"
			+ "                                          qb:dimension gk-dim:TimeStamp . \n"	
			+ "<http://www.geoknow.eu/data-cube/dsd2/c5> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of PropsCount\" ;\n"
			+ "                                          qb:measure sdmx-measure:PropsCount . \n\n"	
			//-------------DataCube3------------------------------------	
			+ "<http://www.geoknow.eu/data-cube/dsd3/c1> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Property\" ;\n"
			+ "                                          qb:dimension gk-dim:Property . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd3/c2> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Class\" ;\n"
			+ "                                          qb:dimension gk-dim:Class . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd3/c3> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Time Stamp\" ;\n"
			+ "                                          qb:dimension gk-dim:TimeStamp . \n"	
			+ "<http://www.geoknow.eu/data-cube/dsd3/c4> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of ObjectsCount\" ;\n"
			+ "                                          qb:measure sdmx-measure:ObjectsCount . \n"	
			+ "<http://www.geoknow.eu/data-cube/dsd3/c5> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of SubjectsCount\" ;\n"
			+ "                                          qb:measure sdmx-measure:SubjectsCount . \n\n"				
	//-----------------------Dimensions, Unit and Measure ---------------------------------------------------------		
			+ "### \n ## Dimensions, Unit, and Measure\n##\n"
			+ "gk-dim:InstanceType a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Normal or Outlier instance\"@en .\n"
			+ "gk-dim:Class a qb:DimensionProperty ; \n "
			+ "                 rdfs:label   \"class of instance\"@en .\n"
			+ "gk-dim:TimeStamp a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Time Stamp\"@en .\n"
			+ "sdmx-measure:InstaceCount  a qb:MeasureProperty ; \n"	
			+ "                 rdfs:label   \"Instance Count\"@en .\n"
			+ "gk-dim:Instance a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Instance\"@en .\n"
			+ "sdmx-measure:PropsCount  a qb:MeasureProperty ; \n"
			+ "                 rdfs:label   \"Properties Count\"@en .\n"
			+ "gk-dim:Property a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Property name\"@en .\n"
			+ "sdmx-measure:ObjectsCount  a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Distinct Objects Count\"@en .\n"
			+ "sdmx-measure:SubjectsCount a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Distinct Subjects Count\"@en .\n\n"
				  );
		
	}
/**
 * Write initial prefixes 
 * @throws IOException
 */
	private static void writePrefixes() throws IOException {
			String prefixes = "@prefix sdmx-measure: <http://purl.org/linked-data/sdmx/2009/measure#>. \n"
				+ "@prefix sdmx-attribute: <http://purl.org/linked-data/sdmx/2009/attribute#>. \n"
				+ "@prefix sdmx-concept: <http://purl.org/linked-data/sdmx/2009/concept#>. \n"
				+ "@prefix qb: <http://purl.org/linked-data/cube#>. \n"
				+ "@prefix sdmx-code: <http://purl.org/linked-data/sdmx/2009/code#>.\n" 
				+ "@prefix dcterms: <http://purl.org/dc/terms/>. \n"
				+ "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.\n" 
				+ "@prefix dc: <http://purl.org/dc/elements/1.1/>. \n"
				+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema>. \n"
				+ "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.\n" 
				+ "@prefix sdmx-metadata: <http://purl.org/linked-data/sdmx/2009/metadata#>.\n" 
				+ "@prefix sdmx: <http://purl.org/linked-data/sdmx#>. \n"
				+ "@prefix sdmx-dimension: <http://purl.org/linked-data/sdmx/2009/dimension#>.\n" 
				+ "@prefix sdmx-subject: <http://purl.org/linked-data/sdmx/2009/subject#>. \n"
				+ "@prefix gk-dim: <http://www.geoknow.eu/properties/>. \n"
				+ "@prefix owl: <http://www.w3.org/2002/07/owl#>. \n"
				+ "@prefix gk-measure: <http://www.geoknow.eu/measure/>. \n\n";
		     bw.write(prefixes);
		
	}


}
