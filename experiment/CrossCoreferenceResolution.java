package edu.oregonstate.experiment;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;

import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.server.Pipeline;
import edu.stanford.nlp.util.StringUtils;

/**
 * cross coreference resolution
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public class CrossCoreferenceResolution extends ExperimentConstructor {
	
	private Map<String, String> methodToClasses = new HashMap<String, String>();
	
	private final String configFolder;
	
	/**
	 * set experiment properties
	 * 
	 * @param props
	 */
	public CrossCoreferenceResolution(Properties props, String configfolder) {
		super(props);
		
		configFolder = configfolder;
		
		/**
		 * map the procedure to the corresponding main class
		 */
		methodToClasses.put("datageneration", "edu.oregonstate.dataset.DatasetFactory");
		methodToClasses.put("searchtrueloss", "edu.oregonstate.search.SearchFactory");
		methodToClasses.put("learn", "edu.oregonstate.classifier.ClassifierFactory");
		methodToClasses.put("searchlearnedweightwithoutfeature", "edu.oregonstate.search.SearchFactory");
		methodToClasses.put("resultaggregation", "edu.oregonstate.server.ResultAggregation");
		methodToClasses.put("searchlearnedweightwithfeature", "edu.oregonstate.search.SearchFactory");
		
	}

	/**
	 * perform the cross coreference resolution experiment
	 */
	public void performExperiment() {
		String procedure = experimentProps.getProperty("procedures");
		Pipeline pipeline = new Pipeline();
		pipeline.generateProcedures(procedure);
		List<String> procedures = pipeline.getProcedure();
		
		//TODO
		File experimentDirectory = new File(configFolder);
		String[] experiments = experimentDirectory.list();
		
		for (String stepInformation : procedures) {
			System.out.println(stepInformation);
			String[] elements = stepInformation.split("-");
			String step = elements[0];
			String phaseIndex = elements[1];
			String prefix = phaseIndex + "-" + step;
			String mainClass = methodToClasses.get(step);
			for (String experiment : experiments) {
				if (experiment.startsWith(prefix)) {
					try {
						
						Class experimentClass = Class.forName(mainClass);
						Class[] proto = new Class[1];
						proto[0] = Properties.class;
						Object[] params = new Object[1];
						
						// get property of the experiment
						String[] propArgs = new String[]{"-props", configFolder + "/" + experiment};
						Properties prop = StringUtils.argsToProperties(propArgs);
						
						params[0] = prop;
						Constructor ct = experimentClass.getConstructor(proto);
						ExperimentConstructor experimenter = (ExperimentConstructor) ct.newInstance(params);
						experimenter.performExperiment();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		
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
		
		String configFolder = "../corpus/alignexperiment";
		if (args.length == 0) {
			// run the experiment in the local machine for debugging
			args = new String[1];
			args[0] = configFolder +  "/config.properties";
		}
		
		String[] propArgs = new String[]{"-props", args[0]};
		
		Properties props = StringUtils.argsToProperties(propArgs);
		ExperimentConstructor experiment = new CrossCoreferenceResolution(props, configFolder);
		ResultOutput.printTime(logFile, "The start of the experiment: ");
		experiment.performExperiment();
		ResultOutput.printTime(logFile, "The end of the experiment");
	}
}
