package org.aksw.geoknow.helper.vacabularies;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Represnt the SDMX Vocabulary used for Cube Vocabulary
 * @author d.cherix
 *
 */
public class SDMX {

	public static final String uri = "http://purl.org/linked-data/sdmx#";
	public static final String prefix = "sdmx";

	private static Property property(String name) {
		Property result = ResourceFactory.createProperty(uri + name);
		return result;
	}

	protected static final Resource resource( String local ){
		return ResourceFactory.createResource( uri + local );
	}

	public static class MEASURE {
		public static final String uri = "http://purl.org/linked-data/sdmx#";
		public static final String prefix = "sdmx";

		public static final Property age = property("age");
		public static final Property civil = property("civil");
		public static final Property currency = property("currency");
		public static final Property education = property("education");
		public static final Property obs = property("obs");
		public static final Property occupation = property("occupation");
		public static final Property sex = property("sex");

	}
}
