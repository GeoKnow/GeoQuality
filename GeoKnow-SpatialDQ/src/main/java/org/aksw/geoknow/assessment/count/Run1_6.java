package org.aksw.geoknow.assessment.count;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.aksw.geoknow.assessment.GeoQualityMetric;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

public class Run1_6 {

    private static List<GeoQualityMetric> metrics = new LinkedList<GeoQualityMetric>();

    static {
        metrics.add(new InstancesNumberMetric());
        metrics.add(new InstancesOfOtherClassesNumberMetric());
        metrics.add(new PropertiesPerInstances());
        metrics.add(new AveragePointsPerInstance(new PropertyImpl("http://www.w3.org/2003/01/geo/wgs84_pos#geometry")));
        metrics.add(new AveragePolygonsPerInstance("http://geovocab.org/geometry#Polygon"));
//        metrics.add(new AverageSurfaceMetric());
    }

    public static void main(String[] args) throws IOException {
        Model model =  ModelFactory.createDefaultModel();;
        for(GeoQualityMetric metric :metrics){
            System.out.println("######### Starting "+metric.getClass().getSimpleName()+" ############");
            Model generateResultsDataCube = metric.generateResultsDataCube("http://akswnc3.informatik.uni-leipzig.de:8850/sparql");
            model.add(generateResultsDataCube);
            generateResultsDataCube.write(new FileWriter("datacubes/LinkedGeoData/"+metric.getClass().getSimpleName()+".ttl"), "TTL");
            System.out.println("######### Terminated "+metric.getClass().getSimpleName()+" ############");
        }
        model.write(new FileWriter("datacubes/LinkedGeoData/GLD-metric1-5.ttl"), "TTL");
    }
}
