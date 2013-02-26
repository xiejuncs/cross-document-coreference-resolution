package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * coreferent arguments in A2
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlA2 extends NumericFeature {

	public SrlA2() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlA2 = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlA2 > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
