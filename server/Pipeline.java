package edu.oregonstate.server;

import java.lang.reflect.Method;
import java.util.*;

import edu.oregonstate.general.StringOperation;
import edu.stanford.nlp.util.StringUtils;

/**
 * Submit the jobs consecutively
 * 
 * According to the pipeline of our experiment, there are several process, respectively 
 * data generation, search with true loss function and generate the features, learn weight using the algorithm,
 * search with learned weight and generate the features, learn weight using the algorithm, search with learned weight and do not generate
 * the features. This is a typical pipeline of our experiment, we need to encapsulate each component independent. 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Pipeline {

	// the procedures for the experiment
	private List<String> procedures;
	
	// experiment path
	private final String experimentPath;
	
	// default procedure for the experiment
	// searchlearnedweightwithoutfeature is just testing
	private final String DEFAULTPROCEDURE = "datageneration, searchtrueloss, learn, searchlearnedweightwithoutfeature";
	
	// properties for the experiment
	private Properties props; 
	
	public Pipeline(String experiPath) {
		procedures = new ArrayList<String>();
		experimentPath = experiPath;
		props = null;
		
		// initialize the procedures with procedure sequence
		generateProcedures();
	}
	
	// add procedure to the procedure list, and the execution sequence is specified according 
	// to the list
	public void addProcedure(String procedure) {
		procedures.add(procedure);
	}
	
	// one model of dagger needs one searchwithlearnweight and learn, 
	// so based on how many number defined for the experiment, we need to incorporate 
	// the according steps into our experiment setting
	public void dagger(String step) {
		String[] element = step.split("-");
		int noOfModel = Integer.parseInt(element[1]);
		
		for (int i = 0; i < noOfModel; i++) {
			addProcedure(OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHFEATURE);
			addProcedure(OREGONSTATE_LEARN);
		}
	}
	
	// get procedure for the experiment
	public List<String> getProcedure() {
		return procedures;
	}
	
	/**
	 * for each step, generate the configuration file for that step
	 * for example, in the phase of data generation, we need to generate a job for each topic, and maintain its options 
	 * 
	 * and submit the jobs, the following job needs to be specified with the previous job id in order to run the job after the completion
	 * of those previous jobs, just depend on the previous job id
	 */
	public void generateConfigurationFile() {
		PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(props, experimentPath);
		
		List<Integer> previousIDs = new ArrayList<Integer>();
		for (String procedure : procedures) {
			try {
				Method method = pipelineConfiguration.getClass().getMethod(procedure, previousIDs.getClass());
				previousIDs = (List<Integer>) method.invoke(pipelineConfiguration, previousIDs);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}
	
	/**
	 * generate procedure sequence for the experiment
	 * 
	 */
	public void generateProcedures() {
		String experimentConfigurationPath = experimentPath + "/config.properties";
		String[] propArgs = new String[]{"-props", experimentConfigurationPath};
		props = StringUtils.argsToProperties(propArgs);
		
		String procedure = props.getProperty("procedures", DEFAULTPROCEDURE);
		String[] steps = StringOperation.splitString(procedure, ",");
		// add procedure the procedures
		for (String step : steps) {
			if (step.startsWith("dagger")) {
				dagger(step);
			} else {
				addProcedure(step);
			}
		}
	}
	
	/**
	 * List of Operations Here, 
	 * Add new Operation here
	 */
	// generate the dataset
	public static final String OREGONSTATE_DATA_GENERATION = "datageneration";

	// search with the true loss function, output the features 
	public static final String OREGONSTATE_SEARCH_TRUELOSS = "searchtrueloss";

	// learn weight with a specified classifier
	public static final String OREGONSTATE_LEARN = "learn";

	// search with the learned weight, there are two options here, output the features and 
	// does not output features. If output the features, then this step is still in the training phase,
	// if does not output the features, then this step is in the final testing phase
	public static final String OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHFEATURE = "searchlearnedweightwithfeature";
	public static final String OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHOUTFEATURE = "searchlearnedweightwithoutfeature";

	public static void main(String[] args) {
		if (args.length > 1) {
			 System.out.println("there are more parameters, you just can specify one path parameter.....");
            System.exit(1);
		}
		
		if (args.length == 0) {
			// run the experiment in the local machine for debugging
			args = new String[1];
			args[0] = "src/edu/oregonstate/server/pipeline.properties";
		}
		
		Pipeline pipeline = new Pipeline("");
		
		String[] propArgs = new String[]{"-props", args[0]};
		Properties props = StringUtils.argsToProperties(propArgs);
		String procedure = props.getProperty("procedures", pipeline.DEFAULTPROCEDURE);
		String[] steps = StringOperation.splitString(procedure, ",");
		
		// add procedure the procedures
		for (String step : steps) {
			if (step.startsWith("dagger")) {
				pipeline.dagger(step);
			} else {
				pipeline.addProcedure(step);
			}
		}
	}

}

//connect to the server
//ClusterConnection connection = new ClusterConnection();
//try {
//	connection.connect();
//	List<Integer> jobIds = connection.queryJobIds();
//
//	for (Integer id : jobIds) {
//		System.out.println(id);
//	}
//
//	connection.disconnect();
//} catch (Exception e) {
//	e.printStackTrace();
//	System.exit(1);
//}