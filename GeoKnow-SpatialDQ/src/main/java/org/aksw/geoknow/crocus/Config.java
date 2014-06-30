package org.aksw.geoknow.crocus;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Configuration Settings for CROCUS
 * @author Saleem
 *
 */
public class Config {
    private String owlClass;
    private String endpoint;
    private Set<String> properties;
    private String sessionToken;
    private String baseUri;
    private Date startedAt;
    private String graph;
    private String errorMessage;
    private String status;
    private boolean isSuccess;
 
    public String getOwlClass() {
        return owlClass;
    }
    public void setOwlClass(String owlClass) {
        this.owlClass = owlClass;
    }
    public String getEndpoint() {
        return endpoint;
    }
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    public Set<String> getProperties() {
        return properties;
    }
    public void setProperties(Set<String> properties) {
        this.properties = properties;
    }
    public String getSessionToken() {
        return sessionToken;
    }
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }
    public String getBaseUri() {
        return baseUri;
    }
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }
    public Date getStartedAt() {
        return startedAt;
    }
    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }
    @Override
    public String toString() {
        return "Config [owlClass=" + owlClass + ", endpoint=" + endpoint + ", properties=" + properties
                + ", sessionToken=" + sessionToken + ", baseUri=" + baseUri + ", startedAt=" + startedAt + "]";
    }
    
    public static void main(String[] args) throws JsonGenerationException, JsonMappingException, IOException {
        Config c = new Config();
        c.setEndpoint("http://localhost:8080/sparql");
        c.setSessionToken("abcdef");
        c.setBaseUri("http://base.org/");
        c.setOwlClass("http://example.org/#GraduateStudent");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(System.out, c);
    }
  
    public String getGraph() {
        return graph;
    }
    public void setGraph(String graph) {
        this.graph = graph;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    public boolean isSuccess() {
        return isSuccess;
    }
    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}

