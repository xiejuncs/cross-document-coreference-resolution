package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Coreferent Predicate in a A1
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlPA1 extends NumericFeature {

	public SrlPA1() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlPA1 = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlPA1 > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
