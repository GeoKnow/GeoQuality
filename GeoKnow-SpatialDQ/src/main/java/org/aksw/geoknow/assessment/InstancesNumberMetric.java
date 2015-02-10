package org.aksw.geoknow.assessment;

import org.aksw.geoknow.helper.vacabularies.QB;
import org.aksw.geoknow.helper.vacabularies.SDMX;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;
/**
 * This metric return the number of instance for each class in the data set.
 * @author d.cherix
 *
 */
public class InstancesNumberMetric implements GeoQualityMetric {

	private static final String GET_CLASSES ="SELECT ?class (count(distinct ?s) as ?count) WHERE {?s a ?class .}";

	public Model generateResultsDataCube(Model inputModel) {
		Model cubeData = ModelFactory.createDefaultModel();

		Resource dataSet;
		Resource structure = cubeData.createResource("",QB.DataStructureDefinition);

		Resource integerDimension = cubeData.createResource("", QB.DimensionProperty);
		integerDimension.addProperty(RDFS.range, XSD.integer);
		integerDimension.addProperty(RDFS.label, cubeData.createLiteral("Positive integer", "en"));

		structure.addProperty(QB.component, cubeData.createResource().addProperty(QB.dimension, integerDimension));

		Resource measure = cubeData.createResource("", QB.MeasureProperty);
		measure.addLiteral(RDFS.label, cubeData.createLiteral("Number of instances", "en"));
		measure.addProperty(RDFS.subPropertyOf, SDMX.MEASURE.obs);
		structure.addProperty(QB.component, cubeData.createResource().addProperty(QB.measure, measure));


		cubeData.add(cubeData.createStatement(dataSet, QB.structure, structure));


		QueryExecution queryExec = QueryExecutionFactory.create(GET_CLASSES, inputModel);
		ResultSet result = queryExec.execSelect();
		while (result.hasNext()){
			QuerySolution solution = result.next();
			Resource owlClass=solution.getResource("class");
			int instances = solution.getLiteral("count").getInt();
			cubeData.add()
		}
		return null;
	}

	public Model generateResultsDataCube(String endpointUrl) {
		// TODO Auto-generated method stub
		return null;
	}



}
