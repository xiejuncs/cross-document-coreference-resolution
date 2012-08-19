package edu.oregonstate.search;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import Jama.Matrix;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.oregonstate.features.Feature;
import edu.oregonstate.training.Train;

/**
 * 
 * According to the description in section 3.4 of the paper
 * 
 * we use a single linear regression to model cluster merge oeprations 
 * between both verbal and nominal clusters. Intuitively, the liner regression 
 * models the quality of the merge operation, i.e., a score larger than 0.5 indicates that more 
 * than half of the mention pairs introduced by this merge are correct. 
 * <b>NOTE</b>
 * In each iteration, we perform the merge operation that has the highest score. Once two clusters
 * are merged, we regenerate all the mention features to reflect the current clusters.
 * We stop when no merging operation with an overall benefit is found.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class IterativeResolution {

	private Map<Integer, CorefCluster> mpredictedCorefCluster;
	private int featureSize; // feature size, in order to define the dimension of linearModel
	private List<CorefCluster> clusters;
	private List<Integer> clusterKeys;
	private Document mdocument;
	private Dictionaries mDictionary;
	private Matrix mModel;

	public IterativeResolution(Document document, Dictionaries dictionary, Matrix model) {
		mdocument = document;
		mDictionary = dictionary;
		mpredictedCorefCluster = document.corefClusters;
		clusters = new ArrayList<CorefCluster>();
		clusterKeys = new ArrayList<Integer>();
		mModel = model;
		initialize();
	}
	
	/** initialize the clusters */
	private void initialize() {
		for (Integer key : mpredictedCorefCluster.keySet()) {
			CorefCluster cluster = mpredictedCorefCluster.get(key);
			clusters.add(cluster);
			clusterKeys.add(key);
		}
	}
	
	private void fillScore(Map<String, Double> scoreMap) {
		// compute the pair of the entityes
		for (int i = 0; i < (clusters.size() - 1); i++) {
			for (int j = 0; j < clusters.size(); j++) {
				CorefCluster c1 = clusters.get(i);
				CorefCluster c2 = clusters.get(j);
				Counter<String> features = Feature.getFeatures(mdocument, c1, c2, false, mDictionary); // get the feature size
				double value = calculateScore(features);
				if (value > 0.5) {
					scoreMap.put(Integer.toString(i) + "-" + Integer.toString(j), value);
				}
			}
		}
	}
	
	/**
	 * iterative entity/event resolution
	 */
	public void merge(Document document, Dictionaries dictionary) {
		
		Map<String, Double> scoreMap = new HashMap<String, Double>();
		fillScore(scoreMap);
		int i = 0;
		while(scoreMap.size() > 0) {
			System.out.println("another merge----");
			String index = compare_hashMap(scoreMap);
			String[] indexs = index.split("-");
			CorefCluster c1 = clusters.get(Integer.parseInt(indexs[0]));
			CorefCluster c2 = clusters.get(Integer.parseInt(indexs[1]));
			Counter<String> features = Feature.getFeatures(document, c1, c2, false, dictionary); // get the feature
			Set<Mention> toMentions = c1.getCorefMentions();
			Set<Mention> fromMentions = c2.getCorefMentions();
			double correct = 0.0;
			double total = toMentions.size() * fromMentions.size();
			Map<Integer, Mention> goldCorefClusters = document.allGoldMentions;
			for (Mention toMention : toMentions) {
				for (Mention fromMention : fromMentions) {
					if (!goldCorefClusters.containsKey(toMention.mentionID) || !goldCorefClusters.containsKey(fromMention.mentionID)) {
						continue;
					}
					
					if (goldCorefClusters.get(toMention.mentionID).goldCorefClusterID == goldCorefClusters.get(fromMention.mentionID).goldCorefClusterID) {
		    			correct += 1.0;
		    		}
				}
			}
			
			double quality = correct/total;
			String record = Train.buildString(features, quality);
			Train.writeTextFile(Train.currentOutputFileName, record);
			
			int removeID = c1.getClusterID();
			CorefCluster.mergeClusters(document, c2, c1, mDictionary);
			document.corefClusters.remove(removeID);
			scoreMap = new HashMap<String, Double>();
			fillScore(scoreMap);
			
			i++;
		}
		

	}
	
	/*Compare HashMap to get the index with the maximum value*/
	public String compare_hashMap(Map<String, Double> scores) {
		Collection<Double> c = scores.values();
		Double maxvalue = Collections.max(c);
		String maxIndex = "";
		
		Set<String> scores_set = scores.keySet();
		Iterator<String> scores_it = scores_set.iterator();
		while(scores_it.hasNext()) {
			String id = scores_it.next();
			Double value = scores.get(id);
			if (value == maxvalue) {
				maxIndex = id;
				break;
			}
		}
		return maxIndex;
	}
	
	
	private double calculateScore(Counter<String> features) {
		assert mModel.getRowDimension() == features.size();
		double sum = 0.0;
		int i = 0;
		for (String key : features.keySet()) {
			sum += features.getCount(key) * mModel.get(i, 0);
			i += 1;
		}
		return sum;
	}

}
