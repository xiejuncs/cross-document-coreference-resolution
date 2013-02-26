package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * coreferent arguments in AMLoc
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlAMLoc extends NumericFeature {

	public SrlAMLoc() {
		featureName = "SrlAM-LOC";
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlAMLoc = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlAMLoc > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
