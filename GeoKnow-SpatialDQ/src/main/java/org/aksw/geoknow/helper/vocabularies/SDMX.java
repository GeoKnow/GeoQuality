package org.aksw.geoknow.helper.vocabularies;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * Represnt the SDMX Vocabulary used for Cube Vocabulary
 *
 * @author d.cherix
 *
 */
public class SDMX {

	public static final String uri = "http://purl.org/linked-data/sdmx#";
	public static final String prefix = "sdmx";

	private static Property property(String name) {
		return property(uri, name);
	}

	private static Property property(String namespace, String name) {
		Property result = ResourceFactory.createProperty(namespace + name);
		return result;
	}

	private static final Resource resource(String namespace, String local) {
		return ResourceFactory.createResource(namespace + local);
	}

	private static final Resource resource(String local) {
		return resource(uri, local);
	}

	/**
	 * Represent the sub vocabulary sdmx-measure {@link http://purl.org/linked-data/sdmx/2009/measure#}
	 * @author d.cherix
	 *
	 */
	public static class MEASURE {
		public static final String uri = "http://purl.org/linked-data/sdmx/2009/measure#";
		public static final String prefix = "sdmx-measure";

		public static final Property age = property(uri, "age");
		public static final Property civil = property(uri, "civil");
		public static final Property currency = property(uri, "currency");
		public static final Property education = property(uri, "education");
		public static final Property obs = property(uri, "obs");
		public static final Property occupation = property(uri, "occupation");
		public static final Property sex = property(uri, "sex");

	}

}
