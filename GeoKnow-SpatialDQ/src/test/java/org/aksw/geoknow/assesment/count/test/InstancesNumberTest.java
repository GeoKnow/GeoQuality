package org.aksw.geoknow.assesment.count.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.aksw.geoknow.assessment.count.InstancesNumberMetric;
import org.aksw.geoknow.helper.vocabularies.GK;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class InstancesNumberTest {
    @Test
    public void test() {

        Random random = new Random();
        Model model = ModelFactory.createDefaultModel();

        Map<String, Integer> map = new HashMap<String, Integer>();

        for (int i = 0; i < 20; i++) {
            Resource resource = model.createResource("http://example.org/class/" + i);
            resource.addProperty(RDF.type, OWL.Class);
            int r = random.nextInt(100 + 1);
            map.put(resource.getURI(), r);
            for (int j = 0; j < r; j++) {
                Resource instance = model.createResource("http://example.org/instance/" + j);
                instance.addProperty(RDF.type, resource);
            }
        }

        InstancesNumberMetric metric = new InstancesNumberMetric();
        Model cube = metric.generateResultsDataCube(model);

        ParameterizedSparqlString queryString = new ParameterizedSparqlString(
                "SELECT ?obs WHERE {?obs ?measure ?m ; ?dim ?d .}");
        for (Entry<String, Integer> e : map.entrySet()) {
            queryString.setIri("measure", GK.MEASURE.InstanceCount.getURI());
            queryString.setIri("dim", GK.DIM.Class.getURI());
            queryString.setIri("d", e.getKey());
            queryString.setLiteral("m", e.getValue());

            QueryExecution qexec = QueryExecutionFactory.create(queryString.asQuery(), cube);
            ResultSet result = qexec.execSelect();
            assertThat(result, is(notNullValue()));
            assertThat(result.hasNext(), is(true));
            assertThat(result.next(), is(notNullValue()));
            assertThat(result.hasNext(), is(false));
        }
    }
}
