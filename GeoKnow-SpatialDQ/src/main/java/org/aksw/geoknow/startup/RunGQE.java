/**
 *
 */
package org.aksw.geoknow.startup;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.aksw.geoknow.assessment.GeoQualityMetric;
import org.aksw.geoknow.assessment.count.AveragePointsPerClass;
import org.aksw.geoknow.assessment.count.AveragePolygonsPerClass;
import org.aksw.geoknow.assessment.count.InstancesNumberMetric;
import org.aksw.geoknow.assessment.count.InstancesOfOtherClassesNumberMetric;
import org.aksw.geoknow.assessment.count.PropertiesPerClass;
import org.aksw.geoknow.datacube.StructurednessDataCube;
import org.aksw.simba.largerdfbench.util.Structuredness;
/**
 * @author sherif
 *
 */
import org.apache.log4j.Logger;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryException;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;

public class RunGQE {
    private static final Logger logger = Logger.getLogger(RunGQE.class.getName());
    long startTime = System.currentTimeMillis();
    static String inputEndPoint = new String();
    static String outputFile = new String();
    static String className = new String();
    static String predicateName = new String();
    static String polygonClass = null;
    static List<String> defaultGraphs = new ArrayList();
    static int metricNr = 0;
    private static final String HELP_MESSAGE = "usage: java -jar GQE.jar -e <endpoint_url> -m <metric_number> -o <file>  [-c <class_name>]  [-p <predicate>]"
            + "\n"
            + "-e\t\tendpoint\t<endpoint_url>\tSPARQL endpoint URL" + "\n"
            + "-o\t--output\t<file>\tOutput file" + "\n"
            + "-c\t--class\t<class_name>\tClass name optional for metric number 1" + "\n"
            + "-p\t--predicate\t<predicate>\tPredicate for point, required for metrix 1" + "\n"
            + "-cp\t--classPolygon\t<predicate>\tPredicate for polygon, required for metrix 6" + "\n"
            + "-g\t--graph\tgraph\tGraph to use as default graph" + "\n"
            + "-m\t\tmetrics\t<metric_number>\tGQE metric number, where:" + "\n"
            + "\t 1 \t Average Point Set" + "\n"
            + "\t 2 \t Properties Per Class" + "\n"
            + "\t 3 \t Instances Per Class" + "\n"
            + "\t 4 \t Number of Intersecting Classes Instances" + "\n"
            + "\t 5 \t Average Number of Points Per Class" + "\n"
            + "\t 6 \t Average number of Polygons Per Class" + "\n"
            + "\t 7 \t Average Distance Between Point Sets which Represent the Same Resource" + "\n"
            + "\t 8 \t Coverage, weigted Coverage and Structurdness" + "\n\n"
            + "For Example: java -jar GQE.jar -e http://linkedgeodata.org/sparql -m 1 -Structuredness.n3";

    public static void run(String args[])
            throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException {

        if (args.length < 3) {
            System.out.println(HELP_MESSAGE);
            System.exit(1);
        }
        for (int i = 0; i < args.length; i += 2) {
            if (args[i].equals("-?") || args[i].toLowerCase().equals("--help")) {
                System.out.println(HELP_MESSAGE);
            }
            if (args[i].equals("-e") || args[i].toLowerCase().equals("--endpoint")) {
                inputEndPoint = args[i + 1];
            }
            if (args[i].equals("-m") || args[i].toLowerCase().equals("--metrics")) {
                metricNr = Integer.parseInt(args[i + 1]);
                runMetric();
            }
            if (args[i].equals("-o") || args[i].toLowerCase().equals("--output")) {
                outputFile = args[i + 1];
            }
            if (args[i].equals("-c") || args[i].toLowerCase().equals("--class")) {
                className = args[i + 1];
            }
            if (args[i].equals("-p") || args[i].toLowerCase().equals("--predicate")) {
                predicateName = args[i + 1];
            }
            if (args[i].equals("-g") || args[i].toLowerCase().equals("--graph")) {
                defaultGraphs.add(args[i + 1]);
            }
            if (args[i].equals("-g") || args[i].toLowerCase().equals("--graph")) {
                defaultGraphs.add(args[i + 1]);
            }
        }

    }

    /**
     * @param metricNr
     * @author sherif
     * @throws QueryEvaluationException
     * @throws MalformedQueryException
     * @throws RepositoryException
     * @throws IOException
     */
    private static void runMetric()
            throws RepositoryException, MalformedQueryException, QueryEvaluationException, IOException {
        switch (metricNr) {
        case 1:
            GeoQualityMetric metric = new InstancesNumberMetric(defaultGraphs);
            writeOutput(outputFile, metric.generateResultsDataCube(inputEndPoint));
            break;
        case 2:
            metric = new PropertiesPerClass(defaultGraphs);
            writeOutput(outputFile, metric.generateResultsDataCube(inputEndPoint));
            break;
        case 3:
            metric = new InstancesNumberMetric(defaultGraphs);
            writeOutput(outputFile, metric.generateResultsDataCube(inputEndPoint));
            break;
        case 4:
            metric = new InstancesOfOtherClassesNumberMetric(defaultGraphs);
            writeOutput(outputFile, metric.generateResultsDataCube(inputEndPoint));
            break;
        case 5:
            if (className != null && !className.isEmpty()) {
                metric = new AveragePointsPerClass(new PropertyImpl(predicateName), className, defaultGraphs);
            } else {
                metric = new AveragePointsPerClass(new PropertyImpl(predicateName), defaultGraphs);
            }
            writeOutput(outputFile, metric.generateResultsDataCube(inputEndPoint));
            break;
        case 6:
            metric = new AveragePolygonsPerClass(polygonClass, defaultGraphs);
            writeOutput(outputFile, metric.generateResultsDataCube(inputEndPoint));
            break;
        case 7:
            break;
        case 8:
            System.out.println("\nCoverage, Weighted Coverage and Structuredness DataCube generation started ...");
            double structuredness = Structuredness.getStructurednessValue(inputEndPoint, null);
            // System.out.println("\nOverall Structuredness or Coherence: " + structuredness);
            StructurednessDataCube.generateDataCubes(inputEndPoint, structuredness, outputFile);

            break;
        default:
            System.out.println(metricNr + "is not correcte \n" + HELP_MESSAGE);
            System.exit(1);

        }

    }

    static void writeOutput(String path, Model model) {
        try {
            model.write(new FileWriter(path), "TTL");
        } catch (IOException e) {
            logger.error(e);
        }
    }

    /**
     * @param args
     * @author sherif
     * @throws IOException
     * @throws QueryEvaluationException
     * @throws MalformedQueryException
     * @throws RepositoryException
     */
    public static void main(String[] args)
            throws IOException, RepositoryException, MalformedQueryException, QueryEvaluationException {
        // System.out.println(args.length);
        run(args);

    }

}
