package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Event Lemmas : Cosine Similarity of the lemma vectors of two clusters
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Lemma extends NumericFeature {

	public Lemma() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double lemmaSimilarity = calculateCosineSimilarity(former, latter, featureName, mentionType);
		
		return lemmaSimilarity;
	}
}
