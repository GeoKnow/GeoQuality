package org.aksw.geoknow.assessment;

import com.hp.hpl.jena.rdf.model.Model;

public interface GeoQualityMetric {
	Model generateResultsDataCube(Model inputModel);
	Model generateResultsDataCube(String endpointUrl);

}
