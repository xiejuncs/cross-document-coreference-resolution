package edu.oregonstate.server;

import java.util.Properties;

import edu.oregonstate.general.StringOperation;
import edu.oregonstate.io.ResultOutput;
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
	
	public PipelineConfiguration(Properties props) {
		mProps = props;
		
		StringBuilder sb = new StringBuilder();

		String experiment = mProps.getProperty(EecbConstants.EXPERIMENT_PROP);
		sb.append(EecbConstants.EXPERIMENT_PROP + " = " + experiment + "\n");		
		
		String corpus = mProps.getProperty(EecbConstants.CORPUS_PROP);
		sb.append(EecbConstants.CORPUS_PROP + " = " + corpus + "\n");
		
		String conllScorer = mProps.getProperty(EecbConstants.CONLL_SCORER_PROP);
		sb.append(EecbConstants.CONLL_SCORER_PROP + " = " + conllScorer + "\n");
		
		String debug = mProps.getProperty(EecbConstants.DEBUG_PROP, "false");
		sb.append(EecbConstants.DEBUG_PROP + " = " + debug + "\n\n");
		
		constantProperties = sb.toString();
	}

	/*
	 * generate data generation configurations
	 * 
	 */
	public void datageneration() {
		String dataset = mProps.getProperty(EecbConstants.DATAGENERATION_DATASET_PROP, "true");
		String goldMention = mProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP, "true");
		String postprocessGold = mProps.getProperty(EecbConstants.DATAGENERATION_POSTPROCESS_GOLD_PROP, "true");
		
		StringBuilder sb = new StringBuilder();
		sb.append("datageneration.dataset = " + dataset + "\n");
		sb.append("datageneration.goldmention = " + goldMention + "\n");
		sb.append("datageneration.postprocess.gold = " + postprocessGold + "\n\n");
		
		String datagenerationConstantProperties = constantProperties + sb.toString();
		
		// generate training set properties
		String trainingset = mProps.getProperty(EecbConstants.DATAGENERATION_TRAININGSET_PROP, "");
		createDataConfiguration(trainingset, EecbConstants.DATAGENERATION_TRAININGSET_PROP, datagenerationConstantProperties);
		
		// generate testing set properties
		String testingset = mProps.getProperty(EecbConstants.DATAGENERATION_TESTINGSET_PROP, "");
		createDataConfiguration(testingset, EecbConstants.DATAGENERATION_TESTINGSET_PROP, datagenerationConstantProperties);
		
		// generate development set properties
		String developmentset = mProps.getProperty(EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, "");
		createDataConfiguration(developmentset, EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, datagenerationConstantProperties);
		
	}
	
	/**
	 * create data configuration
	 * 
	 * @param set
	 * @param key
	 * @param otherconfiguration
	 */
	private void createDataConfiguration(String set, String key, String otherconfiguration) {
		if (set.equals("")) {
			return;
		}
		
		String[] topics = StringOperation.splitString(set, ",");
		for (String topic : topics) {
			String topicConfiguration = key + " = " + topic;
			String configuration = otherconfiguration + topicConfiguration;
			ResultOutput.writeTextFile("datageneration" + topic + ".properties", configuration);
			
		}
	}
	
	
}
