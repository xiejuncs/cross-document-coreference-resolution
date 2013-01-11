package edu.oregonstate.features;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.util.EecbConstants;

/**
 * generate feature set
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FeatureFactory {

	/**
	 * add halt feature to feature set if the stopping rate is halt
	 * 
	 * @return
	 */
	public static String[] getFeatures() {
		String stopping = ExperimentConstructor.experimentProps.getProperty(EecbConstants.STOPPING_PROP, "none");
		boolean extendFeature = false;
		if (stopping.equals("halt")) {
			extendFeature = true;
		}
		
		if (extendFeature) {
			return EecbConstants.extendFeaturesName;
		} else {
			return EecbConstants.featuresName;
		}
	}
}
