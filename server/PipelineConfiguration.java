package edu.oregonstate.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.general.StringOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;

/**
 * generate configuration file for each 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class PipelineConfiguration {

	// experiment properties
	private final Properties mProps;

	// the constant configuration
	private final String constantProperties;

	// experiment path
	private final String mExperimentPath;

	public PipelineConfiguration(Properties props, String experimentPath, String[] variables) {
		mProps = props;

		StringBuilder sb = new StringBuilder();

		String experiment = mProps.getProperty(EecbConstants.EXPERIMENT_PROP);
		sb.append(EecbConstants.EXPERIMENT_PROP + " = " + experiment + "\n");

		String corpus = mProps.getProperty(EecbConstants.CORPUS_PROP);
		sb.append(EecbConstants.CORPUS_PROP + " = " + corpus + "\n");

		String conllScorer = mProps.getProperty(EecbConstants.CONLL_SCORER_PROP);
		sb.append(EecbConstants.CONLL_SCORER_PROP + " = " + conllScorer + "\n");

		String debug = mProps.getProperty(EecbConstants.DEBUG_PROP, "false");
		sb.append(EecbConstants.DEBUG_PROP + " = " + debug + "\n");

		String wordnet = mProps.getProperty(EecbConstants.WORDNET_PROP);
		sb.append(EecbConstants.WORDNET_PROP + " = " + wordnet + "\n");
		
		// add the variable
		for (String variable : variables) {
			String value = mProps.getProperty(variable);
			sb.append(variable + " = " + value + "\n");
		}
		
		sb.append("\n");

		constantProperties = sb.toString();

		mExperimentPath = experimentPath;
	}

	/*
	 * generate data generation configurations
	 * 
	 */
	public List<Integer> datageneration(ArrayList<Integer> previousIDs, String phaseIndex) {
		List<Integer> jobIDs = new ArrayList<Integer>();

		String previousIDString = buildPreviousIDString(previousIDs);

		String dataset = mProps.getProperty(EecbConstants.DATAGENERATION_DATASET_PROP, "true");
		String goldMention = mProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP, "true");
		String postprocessGold = mProps.getProperty(EecbConstants.DATAGENERATION_POSTPROCESS_GOLD_PROP, "true");
		String annotators = mProps.getProperty(EecbConstants.DATAGENERATION_ANNOTATORS_PROP);

		StringBuilder sb = new StringBuilder();
		sb.append("phase = " + phaseIndex + "\n");
		sb.append("datageneration.dataset = " + dataset + "\n");
		sb.append("datageneration.goldmention = " + goldMention + "\n");
		sb.append("datageneration.postprocess.gold = " + postprocessGold + "\n");
		sb.append(EecbConstants.DATAGENERATION_ANNOTATORS_PROP + " = " + annotators + "\n\n");

		String datagenerationConstantProperties = constantProperties + sb.toString();
		String procedure = "datageneration";
		String mainClass = "edu.oregonstate.dataset.DatasetFactory";

		// generate training set properties
		String trainingset = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		List<Integer> trainingIDs = createDataConfiguration(trainingset, EecbConstants.DATAGENERATION_TRAININGSET_PROP, 
				datagenerationConstantProperties, previousIDString, procedure, mainClass, phaseIndex);
		if (trainingIDs != null) {
			jobIDs.addAll(trainingIDs); 
		}

		// generate testing set properties
		String testingset = mProps.getProperty(EecbConstants.DATAGENERATION_TESTINGSET_PROP, "");
		List<Integer> testingIDs = createDataConfiguration(testingset, EecbConstants.DATAGENERATION_TESTINGSET_PROP, 
				datagenerationConstantProperties, previousIDString, procedure, mainClass, phaseIndex);
		if (testingIDs != null) {
			jobIDs.addAll(testingIDs);
		}


		// generate development set properties
		String developmentset = mProps.getProperty(EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, "");
		List<Integer> developmentIDs = createDataConfiguration(developmentset, EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, 
				datagenerationConstantProperties, previousIDString, procedure, mainClass, phaseIndex);
		if (developmentIDs != null) {
			jobIDs.addAll(developmentIDs); 
		}

		return jobIDs;
	}

	/**
	 * search with true loss function configuration
	 * 
	 * @param previousIDs
	 * @return
	 */
	public List<Integer> searchtrueloss(ArrayList<Integer> previousIDs, String phaseIndex) {
		List<Integer> jobIDs = new ArrayList<Integer>();

		String previousIDString = buildPreviousIDString(previousIDs);
		String searchtype = "searchtrueloss";
		
		StringBuilder sb = new StringBuilder();
		sb.append("phase = " + phaseIndex + "\n");
		sb.append("search.type = " + searchtype + "\n");
		
		String searchConstantProperties = constantProperties + sb.toString();
		String procedure = "searchtrueloss";
		String mainClass = "edu.oregonstate.search.SearchFactory";
		
		// generate training set properties
		String trainingset = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		List<Integer> trainingIDs = createDataConfiguration(trainingset, EecbConstants.DATAGENERATION_TRAININGSET_PROP, 
				searchConstantProperties, previousIDString, procedure, mainClass, phaseIndex);
		if (trainingIDs != null) {
			jobIDs.addAll(trainingIDs); 
		}
		
		return jobIDs;
	}
	
	/**
	 * learn
	 * 
	 * @param previousIDs
	 * @return
	 */
	public List<Integer> learn(ArrayList<Integer> previousIDs, String phaseIndex) {
		List<Integer> jobIDs = new ArrayList<Integer>();

		String previousIDString = buildPreviousIDString(previousIDs);
		StringBuilder sb = new StringBuilder();
		sb.append("phase = " + phaseIndex + "\n");
		
		String trainingset = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		sb.append(EecbConstants.DATAGENERATION_TRAININGSET_PROP + " = " + trainingset + "\n");
		
		String searchConstantProperties = constantProperties + sb.toString();
		String procedure = "learn";
		String mainClass = "edu.oregonstate.classifier.ClassifierFactory";
		
		String jobConfigPrefix = mExperimentPath + "/" + phaseIndex + "-" + procedure;
		String jobConfigName = jobConfigPrefix + "-config.properties";

		// create config file
		ResultOutput.writeTextFile(jobConfigName, searchConstantProperties);

		// create run file
		generateRunFile(jobConfigPrefix, mainClass);

		// create simple file
		generateSimpleFile(jobConfigPrefix, procedure, "a", phaseIndex);

		Command.chmod(mExperimentPath);

		ClusterConnection connection = new ClusterConnection();
		try {
			connection.connect();
			String jobSimpleName = previousIDString + jobConfigPrefix + "-simple.sh";

			int jobID = connection.submitJob(jobSimpleName);
			connection.disconnect();
			jobIDs.add(jobID);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return jobIDs;
	}
	
	/**
	 * do testing without feature
	 * 
	 * @param previousIDs
	 * @return
	 */
	public List<Integer> searchlearnedweightwithoutfeature(ArrayList<Integer> previousIDs, String phaseIndex) {
		List<Integer> jobIDs = new ArrayList<Integer>();

		String previousIDString = buildPreviousIDString(previousIDs);
		String searchtype = "searchlearnedweightwithoutfeature";
		
		StringBuilder sb = new StringBuilder();
		sb.append("phase = " + phaseIndex + "\n");
		sb.append("search.type = " + searchtype + "\n");
		
		String searchConstantProperties = constantProperties + sb.toString();
		String procedure = "searchlearnedweightwithoutfeature";
		String mainClass = "edu.oregonstate.search.SearchFactory";
		
		// generate testing set properties
		String testing = mProps.getProperty(EecbConstants.DATAGENERATION_TESTINGSET_PROP, "");
		List<Integer> testingIDs = createDataConfiguration(testing, EecbConstants.DATAGENERATION_TESTINGSET_PROP, 
				searchConstantProperties, previousIDString, procedure, mainClass, phaseIndex);
		if (testingIDs != null) {
			jobIDs.addAll(testingIDs); 
		}
		
		return jobIDs;
	}
	
	/**
	 * do testing with outputting the feature
	 * 
	 * @param previousIDs
	 * @param phaseIndex
	 * @return
	 */
	public List<Integer> searchlearnedweightwithfeature(ArrayList<Integer> previousIDs, String phaseIndex) {
		List<Integer> jobIDs = new ArrayList<Integer>();

		String previousIDString = buildPreviousIDString(previousIDs);
		String searchtype = "searchlearnedweightwithfeature";
		
		StringBuilder sb = new StringBuilder();
		sb.append("phase = " + phaseIndex + "\n");
		sb.append("search.type = " + searchtype + "\n");
		
		String searchConstantProperties = constantProperties + sb.toString();
		String procedure = "searchlearnedweightwithfeature";
		String mainClass = "edu.oregonstate.search.SearchFactory";
		
		// generate training set properties
		String trainingset = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		List<Integer> trainingIDs = createDataConfiguration(trainingset, EecbConstants.DATAGENERATION_TRAININGSET_PROP, 
				searchConstantProperties, previousIDString, procedure, mainClass, phaseIndex);
		if (trainingIDs != null) {
			jobIDs.addAll(trainingIDs); 
		}
		
		return jobIDs;
	}
	
	
	/**
	 * aggregate the result according to the coref cluster
	 * 
	 * @param previousIDs
	 * @param phaseIndex
	 * @return
	 */
	public List<Integer> resultaggregation(ArrayList<Integer> previousIDs, String phaseIndex) {
		List<Integer> jobIDs = new ArrayList<Integer>();
		
		String previousIDString = buildPreviousIDString(previousIDs);
		
		StringBuilder sb = new StringBuilder();
		sb.append("phase = " + phaseIndex + "\n");
		
		// different sets, including training, testing and development
		String training = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		if (!training.equals("")) {
			sb.append(EecbConstants.DATAGENERATION_TRAININGSET_PROP + " = " + training + "\n");
		}
		
		String testing = mProps.getProperty(EecbConstants.DATAGENERATION_TESTINGSET_PROP, "");
		if (!testing.equals("")) {
			sb.append(EecbConstants.DATAGENERATION_TESTINGSET_PROP + " = " + testing + "\n");
		}
		
		String development = mProps.getProperty(EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, "");
		if (!development.equals("")) {
			sb.append(EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP + " = " + development + "\n");
		}
		
		String searchConstantProperties = constantProperties + sb.toString();
		String procedure = "resultaggregation";
		String mainClass = "edu.oregonstate.server.ResultAggregation";
		
		// generate testing set properties
		String jobConfigPrefix = mExperimentPath + "/" + phaseIndex + "-" + procedure;
		String jobConfigName = jobConfigPrefix + "-config.properties";

		// create config file
		ResultOutput.writeTextFile(jobConfigName, searchConstantProperties);

		// create run file
		generateRunFile(jobConfigPrefix, mainClass);

		// create simple file
		generateSimpleFile(jobConfigPrefix, procedure, "a", phaseIndex);

		Command.chmod(mExperimentPath);

		ClusterConnection connection = new ClusterConnection();
		try {
			connection.connect();
			String jobSimpleName = previousIDString + jobConfigPrefix + "-simple.sh";

			int jobID = connection.submitJob(jobSimpleName);
			connection.disconnect();
			jobIDs.add(jobID);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
		return jobIDs;
	}

	/**
	 * build previous ID string
	 * 
	 * @param previousIDs
	 * @return
	 */
	private String buildPreviousIDString(List<Integer> previousIDs) {
		if (previousIDs.size() == 0) return "";

		StringBuilder sb = new StringBuilder();
		sb.append("-hold_jid ");
		for (int index = 0; index < previousIDs.size(); index++) {
			if (index == (previousIDs.size() - 1)) {
				sb.append(previousIDs.get(index) + " ");
			} else {
				sb.append(previousIDs.get(index) + ",");
			}
		}

		return sb.toString();

	}

	/**
	 * create data configuration
	 * 
	 * @param set
	 * @param key
	 * @param constantconfiguration
	 */
	private List<Integer> createDataConfiguration(String set, String key, 
												  String constantconfiguration, String previousIDString,
												  String procedure, String mainClass, String phaseIndex) {
		if (set.equals("")) {
			return null;
		}

		List<Integer> jobIDs = new ArrayList<Integer>();

		String[] topics = StringOperation.splitString(set, ",");
		ClusterConnection connection = new ClusterConnection();
		try {
			connection.connect();
			for (String topic : topics) {
				Thread.sleep(3000);
				String topicConfiguration = key + " = " + topic;
				String configuration = constantconfiguration + topicConfiguration;

				String jobConfigPrefix = mExperimentPath + "/" + phaseIndex + "-" + procedure + "-" + topic;
				String jobConfigName = jobConfigPrefix + "-config.properties";

				// create config file
				ResultOutput.writeTextFile(jobConfigName, configuration);

				// create run file
				generateRunFile(jobConfigPrefix, mainClass);

				// create simple file
				generateSimpleFile(jobConfigPrefix, procedure, topic, phaseIndex);

				Command.chmod(mExperimentPath);

				String jobSimpleName = previousIDString + jobConfigPrefix + "-simple.sh";

				int jobID = connection.submitJob(jobSimpleName);

				jobIDs.add(jobID);

			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		} finally {
			connection.disconnect();
		}

		return jobIDs;
	}

	/**
	 * generate the run file
	 * 
	 * @param jobConfigPrefix
	 */
	private void generateRunFile(String jobConfigPrefix, String mainClassPath) {
		String runPath = jobConfigPrefix + "-run.sh";
		String configPath = jobConfigPrefix + "-config.properties";
		StringBuilder sb = new StringBuilder();
		sb.append("CLASSPATH=/nfs/guille/xfern/users/xie/Experiment/jarfile/cr.jar");                                 // change the name according to the jar file
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/commons-collections-3.2.1.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/commons-io-2.4.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/commons-logging-1.1.1.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/Jama-1.0.3.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/jaws-bin.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/joda-time.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/jsch-0.1.49.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/log4j-1.2.17.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/stanford-corenlp-2012-05-22-models.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/xom.jar\n");
		sb.append("export CLASSPATH\n");
		sb.append("java -Xmx8g " + mainClassPath + " " + configPath);
		ResultOutput.writeTextFile(runPath, sb.toString());
	}

	/**
	 * generate simple file
	 * 
	 * @param jobConfigPrefix
	 * @param step
	 * @param topic
	 * @param phaseIndex
	 */
	private void generateSimpleFile(String jobConfigPrefix, String step, String topic, String phaseIndex) {
		String simplePath = jobConfigPrefix + "-simple.sh";
		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/csh\n\n");
		sb.append("# Give the job a name\n");
		sb.append("#$ -N Jun-" + phaseIndex + "-" + topic + "-" + step + "\n");
		sb.append("# set working directory on all host to\n");
		sb.append("# directory where the job was started\n");
		sb.append("#$ -cwd\n\n");

		sb.append("# send all process STDOUT (fd 2) to this file\n");
		sb.append("#$ -o " + mExperimentPath + "/" + step + "-" + topic + "-screencross.txt\n\n");

		sb.append("# send all process STDERR (fd 3) to this file\n");
		sb.append("#$ -e " +  mExperimentPath + "/" + step + "-" + topic + "-job_outputcross.err\n\n");

		sb.append("# specify the hardware platform to run the job on.\n");
		sb.append("# options are: amd64, em64t, i386, volumejob (use i386 if you don't care)\n");
		sb.append("#$ -q eecs,eecs1,eecs2\n\n");

		sb.append("# Commands\n");
		sb.append(jobConfigPrefix + "-run.sh");
		ResultOutput.writeTextFile(simplePath, sb.toString().trim());
	}

}
