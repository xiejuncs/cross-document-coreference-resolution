package edu.oregonstate.features.individualfeature;

import java.util.Arrays;
import java.util.List;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * Number of Coreferent Arguments or Predicates :
 * The total number of shared arguments and predicates between mentions in the two clusters
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlAgreeCount extends NumericFeature {

	public SrlAgreeCount() {
		featureName = this.getClass().getSimpleName();
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double totalAgreement = 0.0;
		String[] verbElements = {"SrlA0", "SrlA1", "SrlA2", "SrlAM-LOC", "Left", "Right"};
		String[] nounElements = {"SrlPA0", "SrlPA1", "SrlPA2", "SrlPAM-LOC"};
		
		List<String> verbRoles = Arrays.asList(verbElements);
		List<String> nounRoles = Arrays.asList(nounElements);
		
		// remove Left and Right
//		if ((former.predictedCentroid.get("SrlA0").size() > 0) && (latter.predictedCentroid.get("SrlA0").size() > 0) ) {
//			verbRoles.remove("Left");
//		}
//		
//		if ((former.predictedCentroid.get("SrlA1").size() > 0) && (latter.predictedCentroid.get("SrlA1").size() > 0)) {
//			verbRoles.remove("Right");
//		}
		
		if (mentionType.equals("")) {
			for (String feature : verbRoles) {
				double number = calculateAgreement(former, latter, feature, mentionType);
				totalAgreement += (number > 0.0) ? 1.0 : 0.0;
			}
		} else {
			for (String feature : nounRoles) {
				double number = calculateAgreement(former, latter, feature, mentionType);
				totalAgreement += (number > 0.0) ? 1.0 : 0.0;
			}
		}
		
		// Mention Pair
//		for(Mention m1 : former.getCorefMentions()) {
//			CorefCluster c1 = new CorefCluster(m1.mentionID, new HashSet<Mention>(Arrays.asList(m1)));
//			c1.regenerateFeature();
//			
//			for(Mention m2 : latter.getCorefMentions()) {
//				CorefCluster c2 = new CorefCluster(m2.mentionID, new HashSet<Mention>(Arrays.asList(m2)));
//				c2.regenerateFeature();
//				
//				if (mentionType.equals("")) {
//					for (String feature : verbElements) {
//						totalAgreement += calculateAgreement(c1, c2, feature, mentionType);
//					}
//				} else {
//					for (String feature : nounElements) {
//						totalAgreement += calculateAgreement(c1, c2, feature, mentionType);
//					}
//				}
//			}
//		}
		
		return totalAgreement;
	}

}
