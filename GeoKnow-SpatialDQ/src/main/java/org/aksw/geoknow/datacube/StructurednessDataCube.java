
package org.aksw.geoknow.datacube;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import org.aksw.simba.largerdfbench.util.Structuredness;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
/**
 * Convert structuredness values into RDF data cubes
 * @author Saleem
 *
 */
public class StructurednessDataCube {
	public static BufferedWriter bw ; 
	public static String clsName=null;
	public static String endpoint =null; 
	public static String timeStamp =new Date().toString(); 
	public StructurednessDataCube(String endpoint,String clsName, String tStmp)
	{
		StructurednessDataCube.clsName = clsName;
		StructurednessDataCube.endpoint=endpoint;
		StructurednessDataCube.timeStamp=tStmp;
	}
	public static void main(String[] args) throws IOException, MalformedQueryException, QueryEvaluationException, RepositoryException {
		System.out.println("Cube Generation Started ...");
		String endpointUrl = "http://localhost:8891/sparql";
		String namedGraph = "http://localhost:8890/nuts"; 
		String abbrv = "NUTS"; //dataset abbreviation
		double structuredness = Structuredness.getStructurednessValue(endpointUrl, namedGraph);
		System.out.println("\nOverall Structuredness or Coherence: " + structuredness);
		StructurednessDataCube.generateDataCubes(abbrv,structuredness,"structurednessDataCubes.n3");
      
	}

/**
 * Converst Structuredness results in to data cubes
 * @param outputFile Output file
 * @param abbrv dataset abbreviation
 * @param structuredness  structuredness value
 * @throws IOException
 * @throws MalformedQueryException
 * @throws QueryEvaluationException
 * @throws RepositoryException
 */
	public static void generateDataCubes(String abbrv, double structuredness, String outputFile) throws IOException, MalformedQueryException, QueryEvaluationException, RepositoryException {
		
		bw= new BufferedWriter(new FileWriter(new File(outputFile)));
		writePrefixes();
	    writeDimesnions();
	    writeObservations(abbrv,structuredness);
	    System.out.println("Results successfully written to: "+outputFile);
		bw.close();
	}
/**
 * Write Datacubes observations
 * @param structuredness dataset structuredness
 * @param abbrv dataset abbreviation
 * @throws IOException
 * @throws RepositoryException
 * @throws MalformedQueryException
 * @throws QueryEvaluationException
 */
	private static void writeObservations(String abbrv, double structuredness) throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException {
		  long obsNo = 1;
		
		 for(String dsClass:Structuredness.hmCoverage.keySet())
		 {
             //----------------------Write observations for the data cube1 =   Class, TimeSamp, Coverage ----------------
				   bw.write("<http://www.geoknow.eu/data-cube/dsd1/obs"+obsNo+"> qb:dataSet <http://www.geoknow.eu/dataset/ds1> ; \n");
				   bw.write( "	gk-dim:Class <"+dsClass+"> ;\n"
						 + "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
						 + "	sdmx-measure:Coverage "+Structuredness.hmCoverage.get(dsClass)+" ;\n"
						 + "	a qb:Observation .\n\n");
			    obsNo++;
		 }
		 System.out.println("DataCube 1 is successfully written...");
		 obsNo = 1;
		 for(String dsClass:Structuredness.hmWeightedCoverage.keySet())
		 {
             //----------------------Write observations for the data cube2 =   Class, TimeSamp, WeightedCoverage ----------------
				   bw.write("<http://www.geoknow.eu/data-cube/dsd2/obs"+obsNo+"> qb:dataSet <http://www.geoknow.eu/dataset/ds2> ; \n");
							
						bw.write( "	gk-dim:Class <"+dsClass+"> ;\n"
						 + "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
						 + "	sdmx-measure:WeightedCoverage "+Structuredness.hmCoverage.get(dsClass)+" ;\n"
						 + "	a qb:Observation .\n\n");
			    obsNo++;
		 }
		   System.out.println("DataCube 2 is successfully written...");
		
		   obsNo = 1;
			//---------Write final data cube3  structuredness-------------	
		   bw.write("<http://www.geoknow.eu/data-cube/dsd3/obs"+obsNo+"> qb:dataSet <http://www.geoknow.eu/dataset/ds2> ; \n");
										
					bw.write( "	gk-dim:Dataset \""+abbrv+"\" ;\n"
					 + "	gk-dim:TimeStamp \""+timeStamp+"\" ;\n"
					 + "	sdmx-measure:Structuredness "+structuredness+" ;\n"
					 + "	a qb:Observation .\n\n");
				System.out.println("DataCube 3 is successfully written...");
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
			+ "	       rdfs:label \" Dataset Class Coverage\" ; \n"
			+ "	       rdfs:comment \"Dataset Class Coverage\" ; \n"
			+ "	       qb:structure <http://www.geoknow.eu/data-cube/dsd1> ;\n"
			+ "	       dcterms:date \""+date+"\". \n\n"
		  	+ "<http://www.geoknow.eu/dataset/ds2>  a qb:DataSet ;\n"
			+ "	       dcterms:publisher \"AKSW, GeoKnow\" ; \n"
			+ "	       rdfs:label \"Dataset Weighted Class Coverage\" ; \n"
			+ "	       rdfs:comment \"Dataset Weighted Class Coverage\" ; \n"
			+ "	       qb:structure <http://www.geoknow.eu/data-cube/dsd2> ;\n"
			+ "	       dcterms:date \""+date+"\". \n\n"		
		  	+ "<http://www.geoknow.eu/dataset/ds3>  a qb:DataSet ;\n"
			+ "	       dcterms:publisher \"AKSW, GeoKnow\" ; \n"
			+ "	       rdfs:label \"Dataset Structuredness\" ; \n"
			+ "	       rdfs:comment \"Dataset Structuredness\" ; \n"
			+ "	       qb:structure <http://www.geoknow.eu/data-cube/dsd3> ;\n"
			+ "	       dcterms:date \""+date+"\". \n\n"	
  //-----------------Data Cube 1 Structure Definitions-----------------------------------------
			+ "# \n# Data Structure Definitions \n # \n"
			+ "<http://www.geoknow.eu/data-cube/dsd1> a qb:DataStructureDefinition ; \n"
			+ "                                       rdfs:label   \"A Data Structure Definition\"@en ;\n"
			+ "                                       rdfs:comment  \"A Data Structure Definition for DataCube1\" ;\n"
			+ "                                       qb:component <http://www.geoknow.eu/data-cube/dsd1/c1>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd1/c2>, \n"
			+ "                                                     <http://www.geoknow.eu/data-cube/dsd1/c3> . \n\n"
			+ "<http://www.geoknow.eu/data-cube/dsd2> a qb:DataStructureDefinition ; \n"
			+ "                                       rdfs:label   \"A Data Structure Definition\"@en ;\n"
			+ "                                       rdfs:comment  \"A Data Structure Definition for DataCube2\" ;\n"
			+ "                                       qb:component <http://www.geoknow.eu/data-cube/dsd2/c1>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd2/c2>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd2/c3> . \n\n"
			+ "<http://www.geoknow.eu/data-cube/dsd3> a qb:DataStructureDefinition ; \n"
			+ "                                       rdfs:label   \"A Data Structure Definition\"@en ;\n"
			+ "                                       rdfs:comment  \"A Data Structure Definition for DataCube3\" ;\n"
			+ "                                       qb:component <http://www.geoknow.eu/data-cube/dsd3/c1>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd3/c2>, \n"
			+ "                                                    <http://www.geoknow.eu/data-cube/dsd3/c3> . \n\n"
	
	//-----------------------Component Specifications-------------------------------------------------------------------
			+ " # \n #Componenet Specifications\n #\n "		
			//-------------DataCube1------------------------------------
			+ "<http://www.geoknow.eu/data-cube/dsd1/c1> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Class \" ;\n"
			+ "                                          qb:dimension gk-dim:Class . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd1/c2> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Time Stamp\" ;\n"
			+ "                                          qb:dimension gk-dim:TimeStamp . \n"	
			+ "<http://www.geoknow.eu/data-cube/dsd1/c3> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Coverage\" ;\n"
			+ "                                          qb:measure sdmx-measure:Coverage . \n\n"				
			//-------------DataCube2------------------------------------
			+ "<http://www.geoknow.eu/data-cube/dsd2/c1> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Class\" ;\n"
			+ "                                          qb:dimension gk-dim:Class . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd2/c2> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Time Stamp\" ;\n"
			+ "                                          qb:dimension gk-dim:TimeStamp . \n"	
			+ "<http://www.geoknow.eu/data-cube/dsd2/c3> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Weighted Coverage\" ;\n"
			+ "                                          qb:measure sdmx-measure:WeightedCoverage . \n\n"	
			//-------------DataCube3------------------------------------	
			+ "<http://www.geoknow.eu/data-cube/dsd3/c1> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Dataset\" ;\n"
			+ "                                          qb:dimension gk-dim:Dataset . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd3/c2> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Time Stamp\" ;\n"
			+ "                                          qb:dimension gk-dim:TimeStamp . \n"
			+ "<http://www.geoknow.eu/data-cube/dsd3/c3> a qb:ComponentSpecification ; \n"
			+ "                                          rdfs:label   \"Component Specification of Structuredness\" ;\n"
			+ "                                          qb:measure sdmx-measure:Structuredness . \n\n"				
	//-----------------------Dimensions, Unit and Measure ---------------------------------------------------------		
			+ "### \n ## Dimensions, Unit, and Measure\n##\n"
			+ "gk-dim:Class a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Class of a dataset\"@en .\n"
			+ "gk-dim:TimeStamp a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Time Stamp\"@en .\n"
			+ "gk-dim:Dataset a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Dataset name\"@en .\n"
			+ "sdmx-measure:Coverage  a qb:MeasureProperty ; \n"	
			+ "                 rdfs:label   \"Class Coverage\"@en .\n"
			+ "sdmx-measure:WeightedCoverage  a qb:MeasureProperty ; \n"
			+ "                 rdfs:label   \"Class Weighted Coverage\"@en .\n"
			+ "sdmx-measure:Structuredness a qb:DimensionProperty ; \n"
			+ "                 rdfs:label   \"Dataset Structuredness\"@en .\n\n"
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
