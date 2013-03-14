package edu.oregonstate.experiment;

import java.util.Properties;

import edu.oregonstate.io.ResultOutput;
import edu.stanford.nlp.util.StringUtils;

/**
 * cross coreference resolution
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public class CrossCoreferenceResolution extends ExperimentConstructor {
	 
	/**
	 * set experiment properties
	 * 
	 * @param props
	 */
	public CrossCoreferenceResolution(Properties props) {
		super(props);
	}

	/**
	 * perform the cross coreference resolution experiment
	 */
	public void performExperiment() {
		
		
	}
	
	/**
	 * The main entry point of the experiment
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 1) {
			 System.out.println("there are more parameters, you just can specify one path parameter.....");
             System.exit(1);
		}
		
		if (args.length == 0) {
			// run the experiment in the local machine for debugging
			args = new String[1];
			args[0] = "../corpus/config.properties";
		}
		
		String[] propArgs = new String[]{"-props", args[0]};
		
		Properties props = StringUtils.argsToProperties(propArgs);
		ExperimentConstructor experiment = new CrossCoreferenceResolution(props);
		ResultOutput.printTime(logFile, "The start of the experiment: ");
		experiment.performExperiment();
		ResultOutput.printTime(logFile, "The end of the experiment");
	}
}
