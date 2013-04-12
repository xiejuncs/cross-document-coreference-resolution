package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

public class SrlRight extends NumericFeature {
	
	public SrlRight() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double srlRight = calculateAgreement(former, latter, featureName, mentionType);
		double indicator = (srlRight > 0.0) ? 1.0 : 0.0;
		
		return indicator;
	}

}
