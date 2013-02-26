package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

public class Animacy extends NumericFeature {

	public Animacy() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double animacySimilarity = calculateCosineSimilarity(former, latter, featureName, mentionType);
		
		return animacySimilarity;
	}

}
