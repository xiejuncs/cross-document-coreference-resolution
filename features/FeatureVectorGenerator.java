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

		// Action Type: verb or noun
		String mentionType = getMentionType(former, latter);
		
		// generate features according to atomic feature
		String[] featureNames = getAtomicFeatureNames();
		for (String feature : featureNames) {
			try {
				Feature individualFeature = (Feature) Class.forName("edu.oregonstate.features.individualfeature."+feature).getConstructor().newInstance();
				double value = individualFeature.generateFeatureValue(document, former, latter, mentionType);
				feature = transformFeature(feature);
				
				features.incrementCount(feature + mentionType, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return features;
	}
	
	/**
	 * because dash is allowed in the class name, so need to transform the feature 
	 * if necessary
	 * 
	 * @param feature
	 * @return
	 */
	public static String transformFeature(String feature) {
		if (feature.equals("SrlAMLoc")) {
			feature = "SrlAM-LOC";
		}
		
		if (feature.equals("SrlPAMLoc")) {
			feature = "SrlPAM-LOC";
		}
		
		if (feature.equals("NSrlAMLoc")) {
			feature = "NSrlAM-LOC";
		}
		
		if (feature.equals("NSrlPAMLoc")) {
			feature = "NSrlPAM-LOC";
		}
		
		return feature;		
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
