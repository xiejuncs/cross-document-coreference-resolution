package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Cosine Similarity of number
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Number extends NumericFeature {
	
	public Number() {
		featureName = this.getClass().getSimpleName();
	}

	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double numberSimilarity = calculateCosineSimilarity(former, latter, featureName, mentionType);
		
		return numberSimilarity;
	}

}
