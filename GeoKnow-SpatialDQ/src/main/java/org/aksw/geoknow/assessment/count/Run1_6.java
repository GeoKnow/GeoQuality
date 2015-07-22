package org.aksw.geoknow.assessment.count;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.aksw.geoknow.assessment.GeoQualityMetric;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

public class Run1_6 {

    private static List<GeoQualityMetric> metrics = new LinkedList<GeoQualityMetric>();

    static {
        metrics.add(new InstancesNumberMetric());
        metrics.add(new InstancesOfOtherClassesNumberMetric());
        metrics.add(new PropertiesPerClass());
        metrics.add(new AveragePointsPerClass(new PropertyImpl("http://geovocab.org/geometry#long")));
        metrics.add(new AveragePolygonsPerClass("http://geovocab.org/geometry#Polygon"));
        metrics.add(new AverageSurfaceMetric());
    }

    public static void main(String[] args) throws IOException {
        Model model =  ModelFactory.createDefaultModel();;
        for(GeoQualityMetric metric :metrics){
            OntModel m = ModelFactory.createOntologyModel();
            m.read(new FileReader("nuts-rdf-0.91.ttl"), "http://nuts.geovocab.org/id/", "TTL");
            System.out.println("######### Starting "+metric.getClass().getSimpleName()+" ############");
            Model generateResultsDataCube = metric.generateResultsDataCube(m);
            model.add(generateResultsDataCube);
            generateResultsDataCube.write(new FileWriter("/tmp/nuts-"+metric.getClass().getSimpleName()+".ttl"), "TTL");
            System.out.println("######### Terminated "+metric.getClass().getSimpleName()+" ############");
        }
        model.write(new FileWriter("datacubes/NUTS/NUTS-metric1-6.ttl"), "TTL");
    }
}
