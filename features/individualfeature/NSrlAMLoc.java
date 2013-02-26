package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Non-coreferent Arguments in a Specific Role AMLOC
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class NSrlAMLoc extends NumericFeature {

	public NSrlAMLoc() {
		featureName = "NSrlAM-LOC";
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double nSrlAMLoc = calculateNonAgreement(former, latter, featureName, mentionType);
		double indicator = (nSrlAMLoc > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}
}
