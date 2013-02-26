package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Coreferent Predicate in a AMLoc
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlPAMLoc extends NumericFeature {

	public SrlPAMLoc() {
		featureName = "SrlPAM-LOC";
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlPAMLoc = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlPAMLoc > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
