package edu.oregonstate.search;

import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.classifier.Parameter;

/**
 * Search Entry point
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SearchFactory extends ExperimentConstructor {

	// model index
	private int modelIndex;
	
	// experiment name
	private final String experimentName;
	
	public SearchFactory(Properties props) {
		super(props);
		
		String experiment = props.getProperty(EecbConstants.SEARCH_TYPE, "searchtrueloss");
		modelIndex = 0;
		String[] elements = experiment.split("-");
		if (elements.length > 1) {
			modelIndex = Integer.parseInt(elements[1]);
		}
		experimentName = elements[0];
	}
	

	/**
	 * perform experiment
	 * 
	 */
	public void performExperiment() {
		if (experimentName.equals("searchtrueloss")) {
			
			
		}
		
	}
}
