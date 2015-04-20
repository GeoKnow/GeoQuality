/**
 * 
 */
package org.aksw.geoknow.assessment.polygons;

import java.util.Map;
import java.util.Set;

import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * @author sherif
 *
 */
public class DataCubeWriter {
	public static Model dataCubeModel = ModelFactory.createDefaultModel();
	
	public Model getDataCubeModel(){
		dataCubeModel.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		dataCubeModel.setNsPrefix("qb", "http://purl.org/linked-data/cube#");
		dataCubeModel.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
		dataCubeModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		dataCubeModel.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
		dataCubeModel.setNsPrefix("gk", "http://www.geoknow.eu/");
		dataCubeModel.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		
		return dataCubeModel;
	}
	//	Data Set 
	//	<http://www.geoknow.eu/dataset/ds1> a qb:DataSet ;
	//		dcterms:publisher "AKSW, GeoKnow" ;
	//		rdfs:label "DataCube1 Results: Normal vs. Outlier Instances" ;
	//		rdfs:comment "DataCube1 Results: Normal vs. Outlier Instances" ;
	//		qb:structure <http://www.geoknow.eu/data-cube/dsd1> ;
	//		dcterms:date "Mon Jun 30 16:37:52 CEST 2014". 

	public void addDataset(Resource dataset, String label, String comment, String publisher, String date, Resource structure){
		dataCubeModel.add(dataset, RDF.type, QB.Dataset);
		dataCubeModel.add(dataset, DCTerms.publisher, publisher);
		dataCubeModel.add(dataset, RDFS.label, label);
		dataCubeModel.add(dataset, RDFS.comment, comment);
		dataCubeModel.add(dataset, QB.structure, structure);
		dataCubeModel.add(dataset, DCTerms.date, date);
	}

	//	Data Structure Definitions
	//	<http://www.geoknow.eu/data-cube/dsd1> a qb:DataStructureDefinition ;
	//		rdfs:label "A Data Structure Definition"@en ;
	//		rdfs:comment "A Data Structure Definition for DataCube1" ;
	//		qb:component <http://www.geoknow.eu/data-cube/dsd1/c1>,
	//				<http://www.geoknow.eu/data-cube/dsd1/c2>,
	//				<http://www.geoknow.eu/data-cube/dsd1/c3>,
	//				<http://www.geoknow.eu/data-cube/dsd1/c4> .

	public void addDataStructureDefinition(Resource dataStructure, String label, String comment, Set<Resource> components){
		dataCubeModel.add(dataStructure, RDF.type, QB.DataStructureDefinition);
		dataCubeModel.add(dataStructure, RDFS.label, label);
		dataCubeModel.add(dataStructure, RDFS.comment, comment);
		for(Resource component : components){
			dataCubeModel.add(dataStructure, QB.component, component);
		}
	}

	//	Component Specifications
	//	 <http://www.geoknow.eu/data-cube/dsd1/c1> a qb:ComponentSpecification ;
	//		 rdfs:label "Component Specification of IsOutlier " ;
	//		 qb:dimension gk-dim:InstanceType . 

	public void addDimensionSpecs(Resource component, String label, Resource dimensionProperty){
		dataCubeModel.add(component, RDF.type, QB.ComponentSpecification);
		dataCubeModel.add(component, RDFS.label, label);
		dataCubeModel.add(component, QB.dimension, dimensionProperty);
	}

	//	<http://www.geoknow.eu/data-cube/dsd1/c4> a qb:ComponentSpecification ;
	//		rdfs:label "Component Specification of Instance" ;
	//		qb:measure sdmx-measure:InstanceCount . 

	public void addMeasureSpecs(Resource component, String label, Resource measureProperty){
		dataCubeModel.add(component, RDF.type, QB.ComponentSpecification);
		dataCubeModel.add(component, RDFS.label, label);
		dataCubeModel.add(component, QB.measure, measureProperty);
	}

	//	Dimensions, Unit, and Measure
	//	gk-dim:InstanceType a 			qb:DimensionProperty ;
	//						rdfs:label "Normal or Outlier instance"@en .
	public void addDimensionProperty(Property dimension, String label){
		dataCubeModel.add(dimension, RDF.type, QB.DimensionProperty);
		dataCubeModel.add(dimension, RDFS.label, label);
	}

	//	sdmx-measure:InstaceCount a qb:MeasureProperty ;
	//			rdfs:label "Instance Count"@en .
	public void addMeasureProperty(Property measure, String label){
		dataCubeModel.add(measure, RDF.type, QB.MeasureProperty);
		dataCubeModel.add(measure, RDFS.label, label);
	}
	
//	<http://www.geoknow.eu/data-cube/dsd2/obs1> qb:dataSet <http://www.geoknow.eu/dataset/ds2> ;
//		gk-dim:Instance <http://linkedgeodata.org/triplify/node1039036534> ;
//		gk-dim:InstanceType "Normal" ;
//		gk-dim:Class <http://linkedgeodata.org/ontology/ReceptionArea> ;
//		gk-dim:TimeStamp "Mon Jun 30 16:37:49 CEST 2014" ;
//		sdmx-measure:PropsCount 13 ;
//		a qb:Observation .
	public void addObservation(Resource observation, Resource dataset, Map<Property, RDFNode> dimensions, Map<Property, RDFNode> measures){
		dataCubeModel.add(observation, RDF.type, QB.Observation);
		dataCubeModel.add(observation, QB.dataset, dataset);
		for(Property d : dimensions.keySet()){
			dataCubeModel.add(observation, d, dimensions.get(d));
		}
		for(Property m : measures.keySet()){
			dataCubeModel.add(observation, m, measures.get(m));
		}
	}

//	public void addDimensionObservation(Resource observation, Resource dataset, Property dimension, RDFNode value){
//		dataCubeModel.add(observation, RDF.type, QB.Observation);
//		dataCubeModel.add(observation, QB.dataset, dataset);
//		dataCubeModel.add(observation, dimension, value);
//	}
//	
//	public void addDimensionObservation(Resource observation, Resource dataset, Property dimension, String value){
//		dataCubeModel.add(observation, RDF.type, QB.Observation);
//		dataCubeModel.add(observation, QB.dataset, dataset);
//		dataCubeModel.add(observation, dimension, value);
//	}
//	
//	public void addMeasureObservation(Resource observation, Resource dataset, Property measure, String value){
//		dataCubeModel.add(observation, RDF.type, QB.Observation);
//		dataCubeModel.add(observation, QB.dataset, dataset);
//		dataCubeModel.add(observation, dimension, value);
//	}
	
}
