/**
 * 
 */
package org.aksw.geoknow.helper.vacabularies;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

/**
 * @author sherif
 *
 */
public class QB {
	public static final String uri = "http://purl.org/linked-data/cube#";
	public static final String prefix = "qb";

	private static Property property(String name) {
		Property result = ResourceFactory.createProperty(uri + name);
		return result;
	}

	protected static final Resource resource( String local ){ 
		return ResourceFactory.createResource( uri + local ); 
	}

	public static String getURI(){ return uri;	}

	public static final Property structure	 = property("structure");
	public static final Property component	 = property("component");
	public static final Property dimension	 = property("dimension");
	public static final Property measure	 = property("measure");
	
	public static final Resource Dataset				 = resource( "Dataset" );
	public static final Resource DataStructureDefinition = resource( "DataStructureDefinition" );
	public static final Resource ComponentSpecification	 = resource( "ComponentSpecification" );
	public static final Resource DimensionProperty		 = resource( "DimensionProperty" );
	public static final Resource MeasureProperty		 = resource( "MeasureProperty" );

}
