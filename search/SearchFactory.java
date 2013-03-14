package edu.oregonstate.search;

import java.util.Properties;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.util.EecbConstants;

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
	 * perform experiment through search
	 * 
	 */
	public void performExperiment() {
		int length = FeatureFactory.getFeatureTemplate().size();
		if (experimentName.equals("searchtrueloss")) {
			double[] weight = new double[length];
			Parameter para = new Parameter(weight);
			
		}
	}
	
	
}
