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
		String[] verbElements = {"SrlA0", "SrlA1", "SrlA2", "SrlAMLoc", "SrlLeft", "SrlRight"};
		String[] nounElements = {"SrlPA0", "SrlPA1", "SrlPA2", "SrlPAMLoc"};
		
		List<String> verbRoles = Arrays.asList(verbElements);
		List<String> nounRoles = Arrays.asList(nounElements);
		
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
		
		return totalAgreement;
	}

}
