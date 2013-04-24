package edu.oregonstate.features;

import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.StringOperation;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.Dictionaries.MentionType;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * feature vector generator
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FeatureVectorGenerator {

	/**
	 * generate centroids for each cluster after merge two clusters
	 * 
	 * @param document
	 */
	public static void generateCentroid(Document document) {
		for (Integer id : document.corefClusters.keySet()) {
			CorefCluster cluster = document.corefClusters.get(id);
			cluster.regenerateFeature();
		}
	}
	
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
	 * @return
	 */
	public static Counter<String> getFeatures(Document document, CorefCluster c1, CorefCluster c2){
		Counter<String> features = new ClassicCounter<String>();
		
		// which cluster appears earlier
		CorefCluster former;
		CorefCluster latter;
		if(c1.getRepresentativeMention().appearEarlierThan(c2.getRepresentativeMention())) {
			former = c1;
			latter = c2;
		} else { 
			former = c2;
			latter = c1;
		}
		
		// Action Type : Verb or Noun
		boolean isVerb = isVerb(former, latter);

		// Action Type: verb or noun
		String mentionType = getMentionType(former, latter);
		
		// generate features according to atomic feature
		String[] featureNames = getAtomicFeatureNames();
		for (String feature : featureNames) {
			try {
				Feature individualFeature = (Feature) Class.forName("edu.oregonstate.features.individualfeature."+feature).getConstructor().newInstance();
				double value = individualFeature.generateFeatureValue(document, former, latter, mentionType);
				
				features.incrementCount(feature + mentionType, value);
			} catch (Exception e) {
				System.out.println(feature);
				e.printStackTrace();
			}
		}
		
		// remove the Left and Right according to basic rule
		boolean noLeft = false;
		boolean noRight = false;
		String left = "";
		String right = "";
		
		for (String feature : features.keySet()) {
			if (isVerb) {
				if (feature.startsWith("SrlA0")) noLeft = true;
				if (feature.startsWith("SrlA1") || feature.startsWith("SrlPA0")) noRight = true;
				if (feature.contains("Left")) left = feature;
				if (feature.contains("Right")) right = feature;
			}
		}
		
		if (noLeft) features.remove(left);
		if (noRight) features.remove(right);
		
		return features;
	}
	
	// get Atomic Feature Name
	public static String[] getAtomicFeatureNames() {
		String propertyKey = EecbConstants.FEATURE_ATOMIC_NAMES;
		Properties mProps = ExperimentConstructor.experimentProps;
		String featureIndicator = mProps.getProperty(propertyKey, "F");
		String atomicFeatureNames = "";
		if (featureIndicator.equals("F")) {
			atomicFeatureNames = EecbConstants.FEATURE_NAMES;
		} else {
			atomicFeatureNames = EecbConstants.NFEATURE_NAMES;
		}
		
		String[] featureNames = StringOperation.splitString(atomicFeatureNames, ",");
		return featureNames;
	}
	
	/**
	 * the action type, verb or noun
	 * 
	 * @param former
	 * @param latter
	 * @return
	 */
	public static boolean isVerb(CorefCluster former, CorefCluster latter) {
		Mention formerRep = former.getRepresentativeMention();
		Mention latterRep = latter.getRepresentativeMention();
		boolean isVerb = latterRep.isVerb || formerRep.isVerb;
		
		return isVerb;
	}
	
	// get mention type 
	public static String getMentionType(CorefCluster former, CorefCluster latter) {
		Mention formerRep = former.getRepresentativeMention();
		Mention latterRep = latter.getRepresentativeMention();
		boolean isVerb = latterRep.isVerb || formerRep.isVerb;
		String mentionType = "";
		if(!isVerb) {
			if(formerRep.mentionType==MentionType.PROPER && latterRep.mentionType==MentionType.PROPER) mentionType = "-PROPER";
			else if(formerRep.mentionType==MentionType.PRONOMINAL || latterRep.mentionType==MentionType.PRONOMINAL) mentionType = "-PRONOMINAL";
			else mentionType = "-NOMINAL";
		}
		
		return mentionType;
	}
	
}
