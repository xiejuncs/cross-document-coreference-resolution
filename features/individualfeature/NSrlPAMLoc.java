package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Non-coreferent Predicate in a Specific Role AM-LOC
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class NSrlPAMLoc extends NumericFeature {

	public NSrlPAMLoc() {
		featureName = "NSrlPAM-LOC";
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double nSrlPAMLoc = calculateNonAgreement(former, latter, featureName, mentionType);
		double indicator = (nSrlPAMLoc > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}
}
