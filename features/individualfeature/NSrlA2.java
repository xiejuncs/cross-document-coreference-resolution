package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Non-coreferent Arguments in a Specific Role A2
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class NSrlA2 extends NumericFeature {

	public NSrlA2() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double nSrlA2 = calculateNonAgreement(former, latter, featureName, mentionType);
		double indicator = (nSrlA2 > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
