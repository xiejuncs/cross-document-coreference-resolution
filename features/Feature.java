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

/**
 * the abstract feature definition, every individual feature should incorporate this feature
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class Feature {

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
		
		// Cluster Type: verb or noun
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
		
		// SYNONYM feature
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

		// OTHER FEATURE except SRLAGREECOUNT
		for(String feature : formerCentroid.keySet()) {
			Counter<String> centFeature1 = latterCentroid.get(feature);
			Counter<String> centFeature2 = formerCentroid.get(feature);

			if(mentionType.equals("-PRONOMINAL") && (feature.startsWith("MENTION_WORD") || feature.startsWith("HEAD"))) continue;
			if(feature.equals("LEMMA") && centFeature1.getCount("say")>0 && centFeature2.getCount("say") > 0) continue;
			
			if (feature.startsWith("SRL") || feature.equals("LEFT") || feature.equals("RIGHT")) {
				Set<String> featureSet1 = new HashSet<String>();
				featureSet1.addAll(centFeature1.keySet());
				featureSet1.retainAll(centFeature2.keySet());
				
				if (featureSet1.size() > 0) {
					features.incrementCount(feature+mentionType);
				}
			} else {
				features.incrementCount(feature+mentionType, SimilarityVector.getCosineSimilarity(new SimilarityVector(centFeature1), new SimilarityVector(centFeature2)));
			}
		}
		
		// SRLAGREECOUNT feature
		int srlAgreeCount = 0;
		for(String feature : features.keySet()) {
			// SRL feature
			if(feature.startsWith("SRL")) {
				if(features.getCount(feature) > 0) srlAgreeCount++;
			}
			
			// VERB : LEFT and RIGHT
			if(isVerb) {
				if (feature.startsWith("LEFT") || feature.startsWith("RIGHT")) {
					if (features.getCount(feature) > 0) {
						srlAgreeCount++;
					}
				}
			}
			
		}
		
		features.incrementCount("SRLAGREECOUNT" + mentionType, srlAgreeCount);
		
		return features;
	}

}
