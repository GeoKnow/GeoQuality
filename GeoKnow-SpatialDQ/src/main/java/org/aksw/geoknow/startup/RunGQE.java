/**
 * 
 */
package org.aksw.geoknow.startup;

import java.io.IOException;

/**
 * @author sherif
 *
 */
import org.apache.log4j.Logger;
public class RunGQE {
		private static final Logger logger = Logger.getLogger(RunGQE.class.getName());
		long startTime = System.currentTimeMillis();
		static String inputEndPoint= new String();
		static String outputFile	= new String();
		static String className	= new String();
		static String predicateName= new String();
		static int metricNr = 0;
		private static final String HELP_MESSAGE = 
				"usage: java -jar GQE.jar -e <endpoint_url> -m <metric_number> -o <file>  [-c <class_name>]  [-p <predicate>]" + "\n"
				+ "-e\t\tendpoint\t<endpoint_url>\tSPARQL endpoint URL"+ "\n"
				+ "-o\t--output\t<file>\tOutput file"+ "\n"
				+ "-c\t--class\t<class_name>\tClass name required for metric number X"+ "\n"
				+ "-p\t--predicate\t<predicate>\tPredicate for point, required for metrix Y"+ "\n"
				+ "-m\t\tmetrics\t<metric_number>\tGQE metric number, where:"+ "\n"
				+ "\t 1 \t Average Point Set"+ "\n"
				+ "\t 2 \t Properties Per Class"+ "\n"
				+ "\t 3 \t Instances Per Class"+ "\n"
				+ "\t 4 \t Number of Intersecting Classes Instances"+ "\n"
				+ "\t 5 \t Average Number of Points Per Class"+ "\n"
				+ "\t 6 \t Average number of Polygons Per Class"+ "\n"
				+ "\t 7 \t Average Distance Between Point Sets which Represent the Same Resource"+ "\n"
				+ "\t 8 \t Coverage, weigted Coverage and Structurdness"+ "\n\n"
				+ "For Example: java -jar GQE.jar -e http://linkedgeodata.org/sparql -m 1 -Structuredness.n3";


		public static void run(String args[]) throws IOException{

			
			if(args.length <3){
				System.out.println(HELP_MESSAGE);
				System.exit(1);
			}
			for(int i=0; i<args.length; i+=2){
				if(args[i].equals("-?") || args[i].toLowerCase().equals("--help")){
					System.out.println(HELP_MESSAGE);
				}
				if(args[i].equals("-e") || args[i].toLowerCase().equals("--endpoint")){
					inputEndPoint = args[i+1];
				}
				if(args[i].equals("-m") || args[i].toLowerCase().equals("--metrics")){
					metricNr = Integer.parseInt(args[i+1]);
					runMetric();
				}
				if(args[i].equals("-o") || args[i].toLowerCase().equals("--output")){
					outputFile = args[i+1];
				}
				if(args[i].equals("-c") || args[i].toLowerCase().equals("--class")){
					className = args[i+1];
				}
				if(args[i].equals("-p") || args[i].toLowerCase().equals("--predicate")){
					predicateName = args[i+1];
				}
			} 
	

		}
/**
		 * @param metricNr
		 * @author sherif
		 */
		private static void runMetric() {
			switch(metricNr){
			case 1:
				break;
			case 2:
				break;
			case 3:
				break;
			case 4:
				break;
			case 5:
				break;
			case 6:
				break;
			case 7:
				break;
			case 8:
				break;
			default:
				System.out.println(metricNr + "is not correcte \n" + HELP_MESSAGE);
				System.exit(1);
				
			}
			
		}


	/**
	 * @param args
	 * @author sherif
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		run(args);

	}

}
