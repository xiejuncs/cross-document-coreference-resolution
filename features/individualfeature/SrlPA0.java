package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Coreferent Predicate in a A0
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlPA0 extends NumericFeature {

	public SrlPA0() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlPA0 = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlPA0 > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
