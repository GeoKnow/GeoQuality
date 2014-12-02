package org.aksw.geoknow.assessment;

import com.hp.hpl.jena.rdf.model.Model;

public interface GeoQualityMetric {
	Model run(Model inputModel);
	
}
