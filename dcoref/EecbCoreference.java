package edu.oregonstate.ie.dcoref;

import java.util.Map;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import edu.oregonstate.CRC_MAIN;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.optimization.ScaledSGDMinimizer.weight;
import edu.stanford.nlp.stats.Counter;


/**
 * Iterative Entity/Event Resolution(Excerpt from Stanford's paper)
 * In this stage, given a linear regression model, find out those action merging two clusters
 * with the highest score (at least 0.5), and then merge the two clusters. 
 * Update the feature of the corresponding mentions and clusters and then do greedy search 
 * again. The process terminates until the merge process can not go forward any more.
 * <p>
 * In addition, we also need to make sure that this class can be used in the training part.
 * In the training part, we need to do the iterative merge T epochs to collect the training data.
 * After data collection, we train a linear regression model.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbCoreference {
	
	/** Clusters for coreferent mentions */
	public Map<Integer, CorefCluster> corefClusters;

	/** Gold Clusters for coreferent mentions */
	public Map<Integer, CorefCluster> goldCorefClusters;
	
	/** linear regression model represented as double list*/
	private Map<String, Double> weights;
	
	
	/** initialize the corefClusters and goldCorefClusters */
	public EecbCoreference(Document document, Map<String, Double> weights) {
		this.corefClusters = document.corefClusters;
		this.goldCorefClusters = document.goldCorefClusters;
		this.weights = weights;
	}
	
	/**
	 * Represent two coref clusters we need to compair
	 * 
	 * @author Jun Xie (xie@eecs.oregonstate.edu)
	 *
	 */
	public static class ClusterPair {
		public CorefCluster e1;
		public CorefCluster e2;
		
		public ClusterPair(CorefCluster e1, CorefCluster e2) {
			this.e1 = e1;
			this.e2 = e2;
		}
	}
	
	/** 
	 * use the CRC_main logger in order to output the result into the log file,
	 * if uses different logger, then the log file maybe not contain the log produced in this file
	 * <p>
	 * merge 2 clusters: to = to + from
	 * 
	 */
	private static Logger logger = CRC_MAIN.logger;
	
	public void merge(Document document, boolean gold, Dictionaries dictionaries) {
		List<CorefCluster> partialCorefClusters = new ArrayList<CorefCluster>();
		for (Integer key : corefClusters.keySet()) {
			partialCorefClusters.add(corefClusters.get(key));
		}
		
		HashMap<ClusterPair, Double> pairScore = new HashMap<EecbCoreference.ClusterPair, Double>();
		int clusterSize = partialCorefClusters.size();
		for (int i = 0; i < (clusterSize - 2); i++) {
			for (int j = (i + 1); j < (clusterSize - 1); j++) {
				CorefCluster e1 = partialCorefClusters.get(i);
				CorefCluster e2 = partialCorefClusters.get(j);
				ClusterPair pair = new ClusterPair(e1, e2);
				
				// get the feature when compared the too clusters and use features counter to multiply the weight in order to get a score
				Counter<String> features = Feature.getFeatures(document, e1, e2, gold, dictionaries);
				double score = calculateScore(features, weights);
				if (score > 0.5) {
					pairScore.put(pair, score);
				}
			}
		}
		while (pairScore.size() > 0 ) {
			ClusterPair index = compare_hashMap(pairScore);
			CorefCluster e1 = index.e1;
			CorefCluster e2 = index.e2;
			int removeID = e1.getClusterID();
			CorefCluster.mergeClusters(e1, e2);
			corefClusters.remove(removeID);
			
			pairScore = new HashMap<EecbCoreference.ClusterPair, Double>();
			clusterSize = partialCorefClusters.size();
			for (int i = 0; i < (clusterSize - 2); i++) {
				for (int j = (i + 1); j < (clusterSize - 1); j++) {
					e1 = partialCorefClusters.get(i);
					e2 = partialCorefClusters.get(j);
					ClusterPair pair = new ClusterPair(e1, e2);
					
					// get the feature when compared the too clusters and use features counter to multiply the weight in order to get a score
					Counter<String> features = Feature.getFeatures(document, e1, e2, gold, dictionaries);
					double score = calculateScore(features, weights);
					if (score > 0.5) {
						pairScore.put(pair, score);
					}
				}
			}
		}	
	}
	
	/**
	 * iterate the hashmap and find which key has the largest value
	 * 
	 * @param scores
	 * @return
	 */
	public ClusterPair compare_hashMap(HashMap<ClusterPair, Double> scores) {
		Collection c = scores.values();
		Double maxvalue = Collections.max(c);
		ClusterPair maxIndex = new ClusterPair(null, null);
		Set<ClusterPair> scores_set = scores.keySet();
		Iterator<ClusterPair> scores_it = scores_set.iterator();
		while(scores_it.hasNext()) {
			ClusterPair id = scores_it.next();
			Double value = scores.get(id);
			if (value == maxvalue) {
				maxIndex = id;
				break;
			}
		}
		return maxIndex;
	}
	
	/** 
	 * calculate the score for a cluster pair
	 *
	 * @param features a list of features, represented as its defined counter data structure
	 * @param weights a list of features, its also represented by a hashmap, the reason for not using array is that we do not the index of features
	 * 
	 * @return score   multiply the features and weights
	 */
	private Double calculateScore(Counter<String> features, Map<String, Double> weights) {
		Double score = 0.0;
		assert features.size() == weights.size();
		for (String key : features.keySet()) {
			double counter = features.getCount(key);
			double weight = weights.get(key);
			score += counter * weight;
		}
		return score;
	}
	
}
