package org.aksw.geoknow.assessment.count;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

public class Run1_6 {

    private static List<GeoQualityMetric> metrics = new LinkedList<GeoQualityMetric>();

    // static {
    // metrics.add(new InstancesNumberMetric());
    // metrics.add(new InstancesOfOtherClassesNumberMetric());
    // metrics.add(new PropertiesPerClass());
    // metrics.add(new AveragePointsPerClass(new PropertyImpl("http://geovocab.org/geometry#long")));
    // metrics.add(new AveragePolygonsPerClass("http://geovocab.org/geometry#Polygon"));
    // metrics.add(new AverageSurfaceMetric());
    // }

    public static void main(String[] args) throws IOException {

        Options options = new Options();
        options.addOption(Option.builder("o").argName("file").desc("output file").longOpt("output")
                .hasArg(true).required(true).build());
        options.addOption(Option.builder("h").desc("print this message").longOpt("help")
                .hasArg(false).required(false).build());
        options.addOption(
                Option.builder("m").argName("1,2").desc("metrics to use as 1,2 to run metics 1 and 2").longOpt("metric")
                        .hasArg(true).required(true).build());
        options.addOption(Option.builder("p").desc("predicate for points").longOpt("pointsPredicate")
                .hasArg(true).build());
        options.addOption(Option.builder("pC").desc("class for points id needed").longOpt("pointsClass")
                .hasArg(true).build());
        options.addOption(Option.builder("c").desc("predicate for polygons").longOpt("polygonsClass")
                .hasArg(true).build());
        options.addOption(Option.builder("e").desc("sparql endpoint").longOpt("endpoint")
                .hasArg(true).required(true).build());
        options.addOption(Option.builder("w").desc("use wkt for polygon description").longOpt("wkt")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("g").desc("graph to use").longOpt("graph")
                .hasArg(true).required(false).build());
        options.addOption(Option.builder("f")
                .desc("output format possible values:\n\tRDF/XML (default)\n\tRDF/XML-ABBREV\n\tN-TRIPLE\n\tTURTLE or\tTTL\n\tN3")
                .longOpt("format")
                .hasArg(true).required(true).build());

        String endpoint = "";
        String output = "";
        String format = null;
        boolean wkt = true;
        List<String> defaultGraphs = null;

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (line.hasOption('h')){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar metrics.jar", options,true);
                return;
            }

            String polygonClass = line.getOptionValue('c', "http://geovocab.org/geometry#Polygon");
            String pointPredicate = line.getOptionValue('p', "http://geovocab.org/geometry#long");
            String pointClass = line.getOptionValue("pC");
            endpoint = line.getOptionValue('e');
            output = line.getOptionValue('o');
            wkt = line.getOptionValue('w') == null ? true : Boolean.parseBoolean(line.getOptionValue('w'));
            format = (String) (line.getOptionValue('f') == null ? true : line.getOptionValue('f'));
            if(line.getOptionValues('g')!=null){
                defaultGraphs = Lists.newLinkedList();
                for(String g : line.getOptionValues('g')){
                    defaultGraphs.add(g);
                }
            }

            String[] metricsName = line.getOptionValues('m');
            Set<String> metricsCode = new HashSet<String>();
            for (String s : metricsName) {
                for (String x : s.split(",")) {
                    metricsCode.add(x);
                }
            }
            for (String metricCode : metricsCode) {
                switch (metricCode) {
                case "1":
                    metrics.add(new InstancesNumberMetric(defaultGraphs));
                    break;
                case "2":
                    metrics.add(new PropertiesPerClass(defaultGraphs));
                    break;
                case "3":
                    if (wkt) {
                        metrics.add(new AverageSurfaceMetricWKT(defaultGraphs));
                    } else {
                        metrics.add(new AverageSurfaceMetric(defaultGraphs));
                    }
                    break;
                case "4":
                    metrics.add(new InstancesOfOtherClassesNumberMetric(defaultGraphs));
                    break;
                case "5":
                    if (pointClass != null) {
                        metrics.add(new AveragePointsPerClass(new PropertyImpl(pointPredicate), pointClass,defaultGraphs));
                    } else {
                        metrics.add(new AveragePointsPerClass(new PropertyImpl(pointPredicate),defaultGraphs));
                    }
                    break;
                case "6":
                    metrics.add(new AveragePolygonsPerClass(polygonClass, defaultGraphs));
                    break;
                }
            }

        } catch (ParseException exp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar metrics.jar", options,true);
            return;
        }

        Model model = ModelFactory.createDefaultModel();
        for (GeoQualityMetric metric : metrics) {
            System.out.println("######### Starting " + metric.getClass().getSimpleName() + " ############");
            Model generateResultsDataCube = metric.generateResultsDataCube(endpoint);
            model.add(generateResultsDataCube);
            generateResultsDataCube.write(new FileWriter("/tmp/nuts-" + metric.getClass().getSimpleName() + ".ttl"),
                    "TTL");
            System.out.println("######### Terminated " + metric.getClass().getSimpleName() + " ############");
        }
        model.write(new FileWriter(output), format);
    }
}
