package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Number of non-Coreferent Arguments or Predicates :
 * The total number of uncommon arguments and predicates between mentions in the two clusters
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class NSrlAgreeCount extends NumericFeature {

	public NSrlAgreeCount() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double totalNonAgreement = 0.0;
		String[] verbElements = {"NSrlA0", "NSrlA1", "NSrlA2", "NSrlAM-LOC"};
		String[] nounElements = {"NSrlPA0", "NSrlPA1", "NSrlPA2", "NSrlPAM-LOC"};
		
		if (mentionType.equals("")) {
			for (String feature : verbElements) {
				double number = calculateNonAgreement(former, latter, feature, mentionType);
				totalNonAgreement += (number > 0.0) ? 1.0 : 0.0;
			}
		} else {
			for (String feature : nounElements) {
				double number = calculateNonAgreement(former, latter, feature, mentionType);
				totalNonAgreement += (number > 0.0) ? 1.0 : 0.0;
			}
		}
		
		return totalNonAgreement;
	}
}
