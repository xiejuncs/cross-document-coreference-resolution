package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Cosine Similarity of gender
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Gender extends NumericFeature {
	
	public Gender() {
		featureName = this.getClass().getSimpleName();
	}

	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double genderSimilarity = calculateCosineSimilarity(former, latter, featureName, mentionType);
		
		return genderSimilarity;
	}

}
