package edu.oregonstate.features;

import java.util.HashMap;
import java.util.Set;

import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.oregonstate.featureExtractor.SimilarityVector;
import edu.oregonstate.general.SetOperation;

/**
 * the abstract feature definition, every individual feature should incorporate this feature
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class Feature {
	
	// feature name
	protected String featureName;
	
	public Feature() {
		featureName = getClass().getSimpleName();
	}
	
	// the extended class override this method to 
	// single its type : Numeric
	public boolean isNominal() {
		return false;
	}
	
	// the extended class override this method to 
	// single its type : Numeric
	public boolean isNumeric() {
		return false;
	}
	
	// return feature name
	public String getFeatureName() {
		return featureName;
	}
	
	// generate feature value according to the document, the two clusters, and mention type
	public abstract double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType);
	
	/**
	 * calculate specific feature similarity given two clusters
	 * 
	 * @param former
	 * @param latter
	 * @param name
	 * @return
	 */
	protected double calculateCosineSimilarity(CorefCluster former, CorefCluster latter, String name, String mentionType) {
		double cosineSimilarity = 0.0;
		
		if(mentionType.equals("-PRONOMINAL") && (name.startsWith("MentionWord") || name.startsWith("Head"))) {
			return cosineSimilarity;
		}
		
		HashMap<String, ClassicCounter<String>> formerCentroid = former.predictedCentroid;
		HashMap<String, ClassicCounter<String>> latterCentroid = latter.predictedCentroid;
		
		Counter<String> formerVector = formerCentroid.get(name);
		Counter<String> latterVector = latterCentroid.get(name);
		
		if(name.equals("Lemma") && latterVector.getCount("say")>0 && formerVector.getCount("say") > 0) {
			return cosineSimilarity;
		}
		
		cosineSimilarity = SimilarityVector.getCosineSimilarity(new SimilarityVector(formerVector), new SimilarityVector(latterVector));
		
		return cosineSimilarity;
	}
	
	/**
	 * How many shared arguments two clusters have in a given role
	 * 
	 * @param former
	 * @param latter
	 * @param name
	 * @return
	 */
	protected double calculateAgreement(CorefCluster former, CorefCluster latter, String name, String mentionType) {		
		if(mentionType.equals("-PRONOMINAL") && (name.startsWith("MentionWord") || name.startsWith("Head"))) {
			return 0.0;
		}
		
		HashMap<String, ClassicCounter<String>> formerCentroid = former.predictedCentroid;
		HashMap<String, ClassicCounter<String>> latterCentroid = latter.predictedCentroid;
		
		Counter<String> formerVector = formerCentroid.get(name);
		Counter<String> latterVector = latterCentroid.get(name);
		
		if(name.equals("Lemma") && latterVector.getCount("say")>0 && formerVector.getCount("say") > 0) {
			return 0.0;
		}
		
		Set<String> commonElementSet = SetOperation.intersection(formerVector, latterVector);
		
		return commonElementSet.size();
	}
	
	/**
	 * 
	 * How many non-shared arguments two clusters have in a given role
	 * 
	 * @param former
	 * @param latter
	 * @param name
	 * @return
	 */
	protected double calculateNonAgreement(CorefCluster former, CorefCluster latter, String name, String mentionType) {
		String featureName = name.substring(1);
		
		if(mentionType.equals("-PRONOMINAL") && (name.startsWith("MentionWord") || name.startsWith("Head"))) {
			return 0.0;
		}
		
		HashMap<String, ClassicCounter<String>> formerCentroid = former.predictedCentroid;
		HashMap<String, ClassicCounter<String>> latterCentroid = latter.predictedCentroid;
		
		Counter<String> formerVector = formerCentroid.get(featureName);
		Counter<String> latterVector = latterCentroid.get(featureName);
		
		if(name.equals("Lemma") && latterVector.getCount("say")>0 && formerVector.getCount("say") > 0) {
			return 0.0;
		}
		
		Set<String> union = SetOperation.union(formerVector, latterVector);
		Set<String> intersection = SetOperation.intersection(formerVector, latterVector);

		return (union.size() - intersection.size());
	}

}
