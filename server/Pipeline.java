package edu.oregonstate.server;

import java.lang.reflect.Method;
import java.util.*;

import edu.oregonstate.general.StringOperation;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
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
	// adjust the experiment name
	private final String[] variables = {EecbConstants.DATAGENERATION_GOLDMENTION_PROP, EecbConstants.FEATURE_ATOMIC_NAMES};

	/**
	 * List of Operations Here,
	 * Add new Operation here
	 */
	// generate the dataset
	private final String OREGONSTATE_DATA_GENERATION = "datageneration";

	// search with the true loss function, output the features 
	private final String OREGONSTATE_SEARCH_TRUELOSS = "searchtrueloss";

	// learn weight with a specified classifier
	private final String OREGONSTATE_LEARN = "learn";

	// search with the learned weight, there are two options here, output the features and 
	// does not output features. If output the features, then this step is still in the training phase,
	// if does not output the features, then this step is in the final testing phase
	private final String OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHFEATURE = "searchlearnedweightwithfeature";
	private final String OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHOUTFEATURE = "searchlearnedweightwithoutfeature";

	/** Aggregate the generated results */
	private final String OREGONSTATE_RESULT_AGGREGATION = "resultaggregation";

	// the procedures for the experiment
	private List<String> procedures;

	// experiment path
	private final String experimentPath;

	// default procedure for the experiment
	// searchlearnedweightwithoutfeature is just testing
	private final String DEFAULTPROCEDURE = "datageneration-0, searchtrueloss-0, learn-0, searchlearnedweightwithoutfeature-0, resultaggregation-0";

	// properties for the experiment
	private Properties props;

	public Pipeline() {
		procedures = new ArrayList<String>();
		experimentPath = null;
	}

	public Pipeline(String experiPath) {
		procedures = new ArrayList<String>();
		experimentPath = experiPath;

		String experimentConfigurationPath = experimentPath + "/config.properties";
		String[] propArgs = new String[]{"-props", experimentConfigurationPath};
		props = StringUtils.argsToProperties(propArgs);
		String procedure = props.getProperty("procedures", DEFAULTPROCEDURE);

		// initialize the procedures with procedure sequence
		generateProcedures(procedure);
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

		// add the procedures for dagger step
		for (int i = 1; i <= noOfModel; i++) {
			addProcedure(OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHFEATURE + "-" + i);
			addProcedure(OREGONSTATE_LEARN + "-" + i);
			addProcedure(OREGONSTATE_SEARCH_LEARNEDWEIGHT_WITHOUTFEATURE + "-" + i);
			addProcedure(OREGONSTATE_RESULT_AGGREGATION + "-" + i);
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
		PipelineConfiguration pipelineConfiguration = new PipelineConfiguration(props, experimentPath, variables);

		List<Integer> previousIDs = new ArrayList<Integer>();
		for (String procedureinformation : procedures) {
			try {
				// get procedure and phaseIndex
				String[] elements = procedureinformation.split("-");
				String procedure = elements[0];
				String phaseIndex = elements[1];

				Method method = pipelineConfiguration.getClass().getMethod(procedure, previousIDs.getClass(), phaseIndex.getClass());
				previousIDs = (List<Integer>) method.invoke(pipelineConfiguration, previousIDs, phaseIndex);

				// finish those previousIDs then submit the following jobs
				boolean halt = true;
				while (halt) {
					// sleep for a minute
					Thread.sleep(6000);

					ClusterConnection connection = new ClusterConnection();
					connection.connect();

					boolean contain = false;

					List<Integer> runningJobIDs = connection.queryJobIds();
					if (runningJobIDs.isEmpty()) break; // there are no running jobs of the specified user

					// check whether there are still job running for the previous procedure
					for (Integer jobID : previousIDs) {
						if (runningJobIDs.contains(jobID)) {
							contain = true;
							break;
						}
					}

					// if previousIDs do not contain the job ID
					// which means that all jobs are finished
					if (!contain) {
						halt = false;
					}

					connection.disconnect();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * generate procedure sequence for the experiment
	 * 
	 */
	public void generateProcedures(String procedure) {
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
	 * do the experiment
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String experimentPath = args[0];
		Pipeline pipeline = new Pipeline(experimentPath);
		Command.chmod(experimentPath);
		pipeline.generateConfigurationFile();
	}

}