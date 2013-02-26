package edu.oregonstate.features.individualfeature;

import java.util.Arrays;
import java.util.HashSet;

import edu.oregonstate.features.FeatureVectorGenerator;
import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;

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
		
		for(Mention m1 : former.getCorefMentions()) {
			CorefCluster c1 = new CorefCluster(m1.mentionID, new HashSet<Mention>(Arrays.asList(m1)));
			c1.regenerateFeature();
			
			for(Mention m2 : latter.getCorefMentions()) {
				CorefCluster c2 = new CorefCluster(m2.mentionID, new HashSet<Mention>(Arrays.asList(m2)));
				c2.regenerateFeature();
				
				if (mentionType.equals("")) {
					for (String feature : verbElements) {
						totalNonAgreement += calculateNonAgreement(c1, c2, feature, mentionType);
					}
				} else {
					for (String feature : nounElements) {
						totalNonAgreement += calculateNonAgreement(c1, c2, feature, mentionType);
					}
				}
			}
		}
		
		return totalNonAgreement;
	}
}
