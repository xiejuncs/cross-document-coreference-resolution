package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Non-coreferent Predicate in a Specific Role A0
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class NSrlPA0 extends NumericFeature {

	public NSrlPA0() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double nSrlPA0 = calculateNonAgreement(former, latter, featureName, mentionType);
		double indicator = (nSrlPA0 > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}
}
