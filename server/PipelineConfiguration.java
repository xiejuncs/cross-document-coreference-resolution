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
	
	public PipelineConfiguration(Properties props, String experimentPath) {
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
		sb.append(EecbConstants.WORDNET_PROP + " = " + wordnet + "\n\n");
		
		constantProperties = sb.toString();
		
		mExperimentPath = experimentPath;
	}

	/*
	 * generate data generation configurations
	 * 
	 */
	public List<Integer> datageneration(ArrayList<Integer> previousIDs) {
		List<Integer> jobIDs = new ArrayList<Integer>();
		
		String previousIDString = buildPreviousIDString(previousIDs);
		
		String dataset = mProps.getProperty(EecbConstants.DATAGENERATION_DATASET_PROP, "true");
		String goldMention = mProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP, "true");
		String postprocessGold = mProps.getProperty(EecbConstants.DATAGENERATION_POSTPROCESS_GOLD_PROP, "true");
		String annotators = mProps.getProperty(EecbConstants.DATAGENERATION_ANNOTATORS_PROP);
		
		StringBuilder sb = new StringBuilder();
		sb.append("datageneration.dataset = " + dataset + "\n");
		sb.append("datageneration.goldmention = " + goldMention + "\n");
		sb.append("datageneration.postprocess.gold = " + postprocessGold + "\n");
		sb.append(EecbConstants.DATAGENERATION_ANNOTATORS_PROP + " = " + annotators + "\n\n");
		
		String datagenerationConstantProperties = constantProperties + sb.toString();
		
		// generate training set properties
		String trainingset = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		List<Integer> trainingIDs = createDataConfiguration(trainingset, EecbConstants.DATAGENERATION_TRAININGSET_PROP, 
				datagenerationConstantProperties, previousIDString);
		if (trainingIDs != null) {
			jobIDs.addAll(trainingIDs); 
		}
		
		// generate testing set properties
		String testingset = mProps.getProperty(EecbConstants.DATAGENERATION_TESTINGSET_PROP, "");
		List<Integer> testingIDs = createDataConfiguration(testingset, EecbConstants.DATAGENERATION_TESTINGSET_PROP, 
				datagenerationConstantProperties, previousIDString);
		if (testingIDs != null) {
			jobIDs.addAll(testingIDs);
		}
		
		
		// generate development set properties
		String developmentset = mProps.getProperty(EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, "");
		List<Integer> developmentIDs = createDataConfiguration(developmentset, EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, 
				datagenerationConstantProperties, previousIDString);
		if (developmentIDs != null) {
			jobIDs.addAll(developmentIDs); 
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
				sb.append(previousIDs.get(index) + ", ");
			}
		}
		
		return sb.toString();
		
	}
	
	/**
	 * create data configuration
	 * 
	 * @param set
	 * @param key
	 * @param otherconfiguration
	 */
	private List<Integer> createDataConfiguration(String set, String key, String otherconfiguration, String previousIDString) {
		if (set.equals("")) {
			return null;
		}
		
		List<Integer> jobIDs = new ArrayList<Integer>();
		
		String[] topics = StringOperation.splitString(set, ",");
		for (String topic : topics) {
			String topicConfiguration = key + " = " + topic;
			String configuration = otherconfiguration + topicConfiguration;
			
			String jobConfigPrefix = mExperimentPath + "/datageneration-" + topic;
			String jobConfigName = jobConfigPrefix + "-config.properties";
			
			// create config file
			ResultOutput.writeTextFile(jobConfigName, configuration);
			
			// create run file
			generateRunFile(jobConfigPrefix, "edu.oregonstate.dataset.DatasetFactory");
			
			// create simple file
			generateSimpleFile(jobConfigPrefix, "datageneration", topic);
			
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

	
	private void generateSimpleFile(String jobConfigPrefix, String step, String topic) {
		String simplePath = jobConfigPrefix + "-simple.sh";
		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/csh\n\n");
		sb.append("# Give the job a name\n");
		sb.append("#$ -N Jun-" + topic + "-" + step + "\n");
		sb.append("# set working directory on all host to\n");
		sb.append("# directory where the job was started\n");
		sb.append("#$ -cwd\n\n");

		sb.append("# send all process STDOUT (fd 2) to this file\n");
		sb.append("#$ -o " + mExperimentPath + "/" + step + "-" + topic + "-screencross.txt\n\n");

		sb.append("# send all process STDERR (fd 3) to this file\n");
		sb.append("#$ -e " +  mExperimentPath + "/" + step + "-" + topic + "-job_outputcross.err\n\n");

		sb.append("# specify the hardware platform to run the job on.\n");
		sb.append("# options are: amd64, em64t, i386, volumejob (use i386 if you don't care)\n");
		sb.append("#$ -q eecs,eecs1,eecs2,share\n\n");

		sb.append("# Commands\n");
		sb.append(jobConfigPrefix + "-run.sh");
		ResultOutput.writeTextFile(simplePath, sb.toString().trim());
	}
	
}
