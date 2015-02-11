/**
 * 
 */
package org.aksw.geoknow.assessment.polygons;

import java.util.List;

import org.aksw.geoknow.helper.vocabularies.QB;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
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

//	<http://www.geoknow.eu/data-cube/dsd1> a qb:DataStructureDefinition ;
//		rdfs:label "A Data Structure Definition"@en ;
//		rdfs:comment "A Data Structure Definition for DataCube1" ;
//		qb:component <http://www.geoknow.eu/data-cube/dsd1/c1>,
//		<http://www.geoknow.eu/data-cube/dsd1/c2>,
//		<http://www.geoknow.eu/data-cube/dsd1/c3>,
//		<http://www.geoknow.eu/data-cube/dsd1/c4> .
	
	public void addDataStructureDefinition(Resource dataStructure, String label, String comment, List<Resource> components){
		dataCubeModel.add(dataStructure, RDF.type, QB.DataStructureDefinition);
		dataCubeModel.add(dataStructure, RDFS.label, label);
		dataCubeModel.add(dataStructure, RDFS.comment, comment);
		for(Resource component : components){
			dataCubeModel.add(dataStructure, QB.component, component);
		}
	}
			
			
//	 <http://www.geoknow.eu/data-cube/dsd1/c1> a qb:ComponentSpecification ;
//		 rdfs:label "Component Specification of IsOutlier " ;
//		 qb:dimension gk-dim:InstanceType . 
	
	public void addDimension(Resource component, String label, Resource dimensionProperty){
		dataCubeModel.add(component, RDF.type, QB.ComponentSpecification);
		dataCubeModel.add(component, RDFS.label, label);
		dataCubeModel.add(component, QB.dimension, dimensionProperty);
	}
	
//	<http://www.geoknow.eu/data-cube/dsd1/c4> a qb:ComponentSpecification ;
//		rdfs:label "Component Specification of Instance" ;
//		qb:measure sdmx-measure:InstanceCount . 
	
	public void addMeasure(Resource component, String label, Resource measure){
		dataCubeModel.add(component, RDF.type, QB.ComponentSpecification);
		dataCubeModel.add(component, RDFS.label, label);
		dataCubeModel.add(component, QB.measure, measure);
	}
	
//	gk-dim:InstanceType a 			qb:DimensionProperty ;
//						rdfs:label "Normal or Outlier instance"@en .
	public void addDimensionProperty(Resource dimension, String label){
		dataCubeModel.add(dimension, RDF.type, QB.DimensionProperty);
		dataCubeModel.add(dimension, RDFS.label, label);
	}
	
//	sdmx-measure:InstaceCount a qb:MeasureProperty ;
//	rdfs:label "Instance Count"@en .
	public void addMeasureProperty(Resource measure, String label){
		dataCubeModel.add(measure, RDF.type, QB.MeasureProperty);
		dataCubeModel.add(measure, RDFS.label, label);
	}
}
