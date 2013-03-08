package edu.oregonstate.dataset;

import java.util.Properties;

import edu.oregonstate.general.StringOperation;
import edu.oregonstate.util.EecbConstants;

/**
 * generate training topics, testing topics, development topics
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TopicGeneration {

	// training topics	
	private String[] trainingTopics;
	
	// testing topics
	private String[] testingTopics;
	
	// development topics
	private String[] developmentTopics;
	
	// experiment properties
	private final Properties experimentProps;
	
	public TopicGeneration(Properties experimentProperties) {
		experimentProps = experimentProperties;
		trainingTopics = null;
		testingTopics = null;
		developmentTopics = null;
		generateTopics();
	}
	
	/**
	 * generate topics
	 * 
	 */
	public void generateTopics() {
		String[] sets = new String[]{EecbConstants.DATAGENERATION_TRAININGSET_PROP, EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP, EecbConstants.DATAGENERATION_TESTINGSET_PROP};
		
		for (String set : sets) {
			String topicString = experimentProps.getProperty(set, "");
			if (topicString != "") {
				
				if (set.equals(EecbConstants.DATAGENERATION_TRAININGSET_PROP)) {
					trainingTopics = StringOperation.splitString(topicString, ",");
				}
				
				if (set.equals(EecbConstants.DATAGENERATION_DEVELOPMENTSET_PROP)) {
					developmentTopics = StringOperation.splitString(topicString, ",");
				}
				
				if (set.equals(EecbConstants.DATAGENERATION_TESTINGSET_PROP)) {
					testingTopics = StringOperation.splitString(topicString, ",");
				}
				
			}
		}
		
	}
	
	// return training topic
	public String[] trainingTopics() {
		return trainingTopics;
	}
	
	// return testing topic
	public String[] testingTopics() {
		return testingTopics;
	}
	
	// return development topic
	public String[] developmentTopics() {
		return developmentTopics;
	}
	
}
