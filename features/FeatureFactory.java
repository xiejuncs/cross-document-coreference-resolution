package edu.oregonstate.features;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.oregonstate.search.State;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * generate feature set
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FeatureFactory {

	/**
	 * add halt feature to feature template set if the stopping rate is halt
	 * 
	 * @return
	 */
	public static String[] getFeatureTemplate() {
		String stopping = "none";
		//String stopping = ExperimentConstructor.experimentProps.getProperty(EecbConstants.STOPPING_PROP, "none");
		boolean extendFeature = false;
		if (stopping.equals("halt")) {
			extendFeature = true;
		}
		
		if (extendFeature) {
			return EecbConstants.extendFeaturesName;
		} else {
			return EecbConstants.featuresName;
		}
	}
	
	/**
	 * get features for the state, there are two versions, one is based on the action, one is based on the state
	 * 
	 * @param document
	 * @param iCluster
	 * @param jCluster
	 * @param dictionaries
	 * @param enableStateFeature
	 * @return
	 */
	public static Counter<String> getFeatures(Document document, State<CorefCluster> initial, Dictionaries dictionaries){
		Counter<String> features = new ClassicCounter<String>();
		
		// generate a state and its features
		Command.generateStateDocument(document, initial);
		Map<Integer, CorefCluster> corefClusters = document.corefClusters;
		List<CorefCluster> clusters = new ArrayList<CorefCluster>();
		for (Integer id : corefClusters.keySet()) {
    		CorefCluster cluster = corefClusters.get(id);
    		cluster.regenerateFeature();
    		clusters.add(cluster);
    	}
		
		// generate the overall feature
		int size = clusters.size();
		int sum = 0;
		for (int i = 0; i < size; i++) {
            CorefCluster icluster = clusters.get(i);
            
            for (int j = 0; j < i; j++) {
            	sum += 1;
                CorefCluster jcluster = clusters.get(j);
                
                Counter<String> feature = Feature.getFeatures(document, icluster, jcluster, false, dictionaries);
                features.addAll(feature);
            }
		}
		
		// normalize the weight
		for (String key : features.keySet()) {
			double value = features.getCount(key);
			features.setCount(key, value / sum);
		}
		
		return features;
	}
	
}
