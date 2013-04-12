package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * closest Left mention Feature
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlLeft extends NumericFeature {

	public SrlLeft() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlLeft = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlLeft > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
