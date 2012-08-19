package edu.oregonstate.features;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import edu.oregonstate.featureExtractor.JointArgumentMatch;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.util.IntPair;
import edu.oregonstate.featureExtractor.SimilarityVector;


public class Feature {

	// used in generating features
	public static boolean SRL_INDICATOR = true;
	public static boolean USE_DISAGREE = true;
	
	/**
	 * 
	 * generate features for cluster pair
	 * <b>NOTE</b>
	 * if any of two clusters containing a verbal mention, we consider the merge an operation between event (V) clusters;
	 * otherwise it is a merge between entity (E) clusters.
	 * According to the paper, we append to all entity features the suffix Proper or Common based on the type of the head 
	 * word of the first mention in each of the two clusters. We use the suffix Proper only if both head words are proper noun
	 * 
	 * <p>
	 * The detail list used when comparing two clusters are specified in Table 2 in section 5 
	 * 
	 * @param document
	 * @param c1
	 * @param c2
	 * @param gold
	 * @param dict
	 * @return
	 */
	public static Counter<String> getFeatures(Document document, CorefCluster c1, CorefCluster c2, boolean gold, Dictionaries dict){

		CorefCluster former;
		CorefCluster latter;
		// an example, Key: Number, ClassicCounter<String> Number = {singular: 1; plural:1}
		HashMap<String, ClassicCounter<String>> formerCentroid;
		HashMap<String, ClassicCounter<String>> latterCentroid;
    
		if(c1.getRepresentativeMention().appearEarlierThan(c2.getRepresentativeMention())) {
			former = c1;
			latter = c2;
			if(gold) {
				formerCentroid = c1.goldCentroid;
				latterCentroid = c2.goldCentroid;
			} else {
				formerCentroid = c1.predictedCentroid;
				latterCentroid = c2.predictedCentroid;
			}
		} else { 
			former = c2;
			latter = c1;
			if(gold) {
				formerCentroid = c2.goldCentroid;
				latterCentroid = c1.goldCentroid;
			} else {
				formerCentroid = c2.predictedCentroid;
				latterCentroid = c1.predictedCentroid;
			}
		}
		Mention formerRep = former.getRepresentativeMention();
		Mention latterRep = latter.getRepresentativeMention(); 
		boolean isVerb = latterRep.isVerb || formerRep.isVerb;

		String mentionType = "";
		if(!isVerb) {
			if(formerRep.mentionType==MentionType.PROPER && latterRep.mentionType==MentionType.PROPER) mentionType = "-PROPER";
			else if(formerRep.mentionType==MentionType.PRONOMINAL || latterRep.mentionType==MentionType.PRONOMINAL) mentionType = "-PRONOMINAL";
			else mentionType = "-NOMINAL";
		}
		Counter<String> features = new ClassicCounter<String>();
    
		double headNom = 0.0;
		double headDenom = 0.0;
		double synonymNom = 0.0;
		double synonymDenom = 0.0;

		for(Mention m1 : c1.getCorefMentions()) {
			for(Mention m2 : c2.getCorefMentions()) {
				if(!JointArgumentMatch.DOPRONOUN && (m1.isPronominal() || m2.isPronominal())) continue;
				IntPair menPair = new IntPair(Math.min(m1.mentionID, m2.mentionID), Math.max(m1.mentionID, m2.mentionID));

				if(isVerb) {
					synonymDenom++;
					if(document.mentionSynonymInWN.contains(menPair)) {
						synonymNom++;
					}
				} else {
					synonymDenom++;
					if(document.mentionSynonymInWN.contains(menPair)) {
						synonymNom++;
					}
				}
			}
		}

		if(isVerb) {
			features.incrementCount("SYNONYM", synonymNom/synonymDenom);
		} else {
			if(synonymDenom > 0) {
				if(!mentionType.equals("-PRONOMINAL")) features.incrementCount("SYNONYM"+mentionType, synonymNom/synonymDenom);
			}
		}

		for(String feature : formerCentroid.keySet()) {
			if(!latterCentroid.containsKey(feature)) {
				continue;
			}
			Counter<String> centFeature1 = latterCentroid.get(feature);
			Counter<String> centFeature2 = formerCentroid.get(feature);

			if(mentionType.equals("-PRONOMINAL") && (feature.startsWith("MENTION_WORD") || feature.startsWith("HEAD"))) continue;
			if(feature.equals("LEMMA") && centFeature1.getCount("say")>0 && centFeature2.getCount("say") > 0) continue;
			if(feature.startsWith("SRL")) {
				Set<String> featureSet1 = new HashSet<String>();
				featureSet1.addAll(centFeature1.keySet());
				featureSet1.retainAll(centFeature2.keySet());
				int overlap = 0;
				for(String f : featureSet1){
					overlap += centFeature1.getCount(f)* centFeature2.getCount(f);
				}
				if(SRL_INDICATOR) {
					if(featureSet1.size() > 0) {
						features.incrementCount(feature+mentionType);
					}
				} else {
					features.incrementCount(feature+mentionType, overlap);
				}
			} else {
				features.incrementCount(feature+mentionType, SimilarityVector.getCosineSimilarity(new SimilarityVector(centFeature1), new SimilarityVector(centFeature2)));
			}
			if(USE_DISAGREE && feature.startsWith("SRL")) features.incrementCount("DISAGREE"+feature+mentionType, 1-SimilarityVector.getCosineSimilarity(new SimilarityVector(centFeature1), new SimilarityVector(centFeature2)));
		}
		if(USE_DISAGREE && features.containsKey("NUMBER"+mentionType)) features.incrementCount("NUMBER_DISAGREE"+mentionType, 1-features.getCount("NUMBER"+mentionType));
		if(USE_DISAGREE && features.containsKey("GENDER"+mentionType)) features.incrementCount("GENDER_DISAGREE"+mentionType, 1-features.getCount("GENDER"+mentionType));
		if(USE_DISAGREE && features.containsKey("ANIMACY"+mentionType)) features.incrementCount("ANIMACY_DISAGREE"+mentionType, 1-features.getCount("ANIMACY"+mentionType));

		boolean noLeft = false;
		boolean noRight = false;
		String left = "";
		String right = "";
		int srlAgreeCount = 0;
		for(String feature : features.keySet()) {
			if(feature.startsWith("SRL")) {
				if(features.getCount(feature) > 0) srlAgreeCount++;
			}
			if(isVerb) {
				if(feature.startsWith("SRLROLES-A0")) noLeft = true;
				if(feature.startsWith("SRLROLES-A1") || feature.startsWith("SRLPRED-A0")) noRight = true;
			}
			if(feature.contains("LEFT")) left = feature;
			if(feature.contains("RIGHT")) right = feature;
		}
		features.incrementCount("SRLAGREECOUNT", srlAgreeCount);
		if(noLeft) features.remove(left);
		if(noRight) features.remove(right);

		if(features.containsKey("HEAD")){
			features.setCount("HEAD-NOMINAL", features.getCount("HEAD"));
			features.remove("HEAD");
		}
		if(features.containsKey("NUMBER")){
			features.setCount("NUMBER-NOMINAL", features.getCount("NUMBER"));
			features.remove("NUMBER");
		}
		if(features.containsKey("GENDER")){
			features.setCount("GENDER-NOMINAL", features.getCount("GENDER"));
			features.remove("GENDER");
		}
		if(features.containsKey("ANIMACY")){
			features.setCount("ANIMACY-NOMINAL", features.getCount("ANIMACY"));
			features.remove("ANIMACY");
		}
		if(features.containsKey("NETYPE")){
			features.setCount("NETYPE-NOMINAL", features.getCount("NETYPE"));
			features.remove("NETYPE");
		}
		if(features.containsKey("MENTION_WORDS")){
			features.setCount("MENTION_WORDS-NOMINAL", features.getCount("MENTION_WORDS"));
			features.remove("MENTION_WORDS");
		}
		
		return features;
	}

}
