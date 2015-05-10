package org.aksw.geoknow.crocus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.geoknow.datacube.CrocusDataCube;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
/**
 * Client side request implementation
 * @author Saleem
 *
 */
public class Client {

	@SuppressWarnings("static-access")
	public static void main(String[] args) throws IOException, InterruptedException, MalformedQueryException, QueryEvaluationException, RepositoryException {
    	Set<String> properties =new HashSet<String>();
    	properties.add("http://www.geonames.org/ontology#officialName");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        List<HttpMessageConverter<?>> list = new ArrayList<HttpMessageConverter<?>>();
        list.add(new MappingJacksonHttpMessageConverter());
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(list);
        Config config = new Config();
        HttpEntity<Config> entity = new HttpEntity<Config>(config,
                headers);
        config.setBaseUri("http://aksw.org/");
        config.setEndpoint("http://localhost:8893/sparql");
        config.setOwlClass("http://www.geonames.org/ontology#Feature");
        config.setProperties(properties);
        config.setSessionToken("6m2tao1o9178qejmlskflcamc2");
        config = restTemplate.postForObject("http://localhost:8080/ontologymetrics/evaluate", entity,
                Config.class);
        System.out.println(config);
        restTemplate = new RestTemplate();
        ResponseEntity<String> result = null;
        while (result == null || result.getBody().startsWith("{")) {
            Thread.currentThread();
			Thread.sleep(1000);
            result = restTemplate.getForEntity(
                    "http://localhost:8080/ontologymetrics/results?sessionToken=6m2tao1o9178qejmlskflcamc2", String.class);
            System.out.println("Calculating Stats...");
            
        }
        System.out.println("Generating data cubes...");
        CrocusDataCube cbGenerator = new CrocusDataCube(config.getEndpoint(),config.getOwlClass(),  new Date().toString());
        cbGenerator.generateDataCubes( result.getBody().toString());
    }
}
