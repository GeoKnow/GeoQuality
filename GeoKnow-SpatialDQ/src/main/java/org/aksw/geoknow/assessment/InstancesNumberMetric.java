package org.aksw.geoknow.assessment;

import org.aksw.geoknow.helper.vacabularies.QB;
import org.aksw.geoknow.helper.vacabularies.SDMX;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * This metric return the number of instance for each class in the data set.
 *
 * @author d.cherix
 *
 */
public class InstancesNumberMetric implements GeoQualityMetric {

	private static final String GET_CLASSES = "SELECT ?class (count(distinct ?s) as ?count) WHERE {?s a ?class .}";

	private static final String NAMESPACE = "";

	private static final String URI_DIMENSION = NAMESPACE + "dimension/uri";

	private static final String NUMBER_MEASURE = NAMESPACE
			+ "measure/numberOfInstances";

	private static final String STRUCTURE = NAMESPACE + "structure";

	public Model generateResultsDataCube(Model inputModel) {

		Model cubeData = createModel();
		Resource dataSet = cubeData.createResource(NAMESPACE + "dataset/");

		cubeData.add(cubeData.createStatement(dataSet, QB.structure,
				cubeData.createResource(STRUCTURE)));

		QueryExecution queryExec = QueryExecutionFactory.create(GET_CLASSES,
				inputModel);
		ResultSet result = queryExec.execSelect();
		Property measure = cubeData.createProperty(NUMBER_MEASURE);
		Property dimension = cubeData.createProperty(URI_DIMENSION);
		while (result.hasNext()) {
			QuerySolution solution = result.next();
			Resource owlClass = solution.getResource("class");
			long instances = solution.getLiteral("count").getLong();
			Resource obs = cubeData.createResource("", QB.Observation);
			obs.addLiteral(measure, instances);
			obs.addProperty(dimension, owlClass);
			obs.addProperty(QB.dataset, dataSet);
		}
		return cubeData;
	}

	private Model createModel() {
		Model cubeData = ModelFactory.createDefaultModel();
		Resource structure = cubeData.createResource(STRUCTURE,
				QB.DataStructureDefinition);

		Property uriDimension = cubeData.createProperty(URI_DIMENSION);
		uriDimension.addProperty(RDF.type, QB.DimensionProperty);
		uriDimension.addProperty(RDFS.range, RDFS.Resource);
		uriDimension.addProperty(RDFS.label,
				cubeData.createLiteral("Class", "en"));

		structure.addProperty(QB.component, cubeData.createResource()
				.addProperty(QB.dimension, uriDimension));

		Property numberInstancesMeasure = cubeData
				.createProperty(NUMBER_MEASURE);
		numberInstancesMeasure.addProperty(RDF.type, QB.MeasureProperty);
		numberInstancesMeasure.addLiteral(RDFS.label,
				cubeData.createLiteral("Number of instances", "en"));
		numberInstancesMeasure
				.addProperty(RDFS.subPropertyOf, SDMX.MEASURE.obs);
		numberInstancesMeasure.addProperty(RDFS.range, XSD.integer);
		structure.addProperty(QB.component, cubeData.createResource()
				.addProperty(QB.measure, numberInstancesMeasure));
		return cubeData;
	}

	public Model generateResultsDataCube(String endpointUrl) {

		QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointUrl,
				"CONSTRUCT {?s a ?o .} WHERE {?s a ?o .}");
		Model model = qexec.execConstruct();

		return this.generateResultsDataCube(model);
	}

}
