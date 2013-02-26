package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * 2nd Order Similarity of Mention Words : cosine similarity of vectors containing words
 * that are distributionally similar to words in the cluster mentions
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class MentionWord extends NumericFeature {

	public MentionWord() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double mentionWordSimilarity = calculateCosineSimilarity(former, latter, featureName, mentionType);
		
		return mentionWordSimilarity;
	}

}
