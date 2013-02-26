package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * cosine similarity of NE label vectors
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class NEType extends NumericFeature {
	
	public NEType() {
		featureName = this.getClass().getSimpleName();
	}

	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double NETypeSimilarity = calculateCosineSimilarity(former, latter, featureName, mentionType);
		
		return NETypeSimilarity;
	}

}
