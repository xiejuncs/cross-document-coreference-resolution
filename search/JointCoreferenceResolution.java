package edu.oregonstate.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Jama.Matrix;
import edu.oregonstate.EventCoreference;
import edu.oregonstate.features.Feature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.stats.Counter;

/**
 * Algorithm 1 in the paper
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class JointCoreferenceResolution {
	private List<CorefCluster> clusters;
	private Document mdocument;
	private Dictionaries mDictionary;
	private Matrix mModel;

	public JointCoreferenceResolution(Document document, Dictionaries dictionary, Matrix model) {
		mdocument = document;
		mDictionary = dictionary;
		clusters = new ArrayList<CorefCluster>();
		mModel = model;
		initialize();
	}
	
	/** initialize the clusters */
	private void initialize() {
		for (Integer key : mdocument.corefClusters.keySet()) {
			CorefCluster cluster = mdocument.corefClusters.get(key);
			clusters.add(cluster);
		}
	}
	
	private void fillScore(Map<String, Double> scoreMap) {
		// compute the pair of the entities
		for (int i = 0; i < (clusters.size() - 1); i++) {
			for (int j = 0; j < i; j++) {
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
	public void merge(Dictionaries dictionary) {
		Map<String, Double> scoreMap = new HashMap<String, Double>();
		fillScore(scoreMap);
		while(scoreMap.size() > 0) {
			String index = compare_hashMap(scoreMap);
			String[] indexs = index.split("-");
			CorefCluster c1 = clusters.get(Integer.parseInt(indexs[0]));
			CorefCluster c2 = clusters.get(Integer.parseInt(indexs[1]));
			System.out.println("another merge----" + c1.getClusterID() + "---->" + c2.getClusterID());
			int removeID = c1.getClusterID();
			CorefCluster.mergeClusters(mdocument, c2, c1, mDictionary);
			mdocument.corefClusters.remove(removeID);
			for (Integer id : mdocument.corefClusters.keySet()) {
            	CorefCluster cluster = mdocument.corefClusters.get(id);
            	cluster.regenerateFeature();
            }
			clusters = new ArrayList<CorefCluster>();
			for (Integer key : mdocument.corefClusters.keySet()) {
				CorefCluster cluster = mdocument.corefClusters.get(key);
				clusters.add(cluster);
			}
			scoreMap = new HashMap<String, Double>();
			fillScore(scoreMap);
		}
		
		// Print the score
		if(EventCoreference.printScore){
	    	CorefScorer mucscore = new ScorerMUC();
	    	mucscore.calculateScore(mdocument);
	    	mucscore.printF1(EventCoreference.logger, true);
	    	
	    	CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
	    	score.calculateScore(mdocument);
	    	score.printF1(EventCoreference.logger, true);
	    	
	    	CorefScorer pairscore = new ScorerPairwise();
	    	pairscore.calculateScore(mdocument);
	    	pairscore.printF1(EventCoreference.logger, true);
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
	
	// according to the how many features not how many value is larger than 0
	private double calculateScore(Counter<String> features) {
		double sum = 0.0;
		for (int i = 0; i < mModel.getRowDimension(); i++) {
			if (i == 0) {
				sum += mModel.get(i, 0);
			} else {
				sum += features.getCount(Feature.featuresName[i-1]) * mModel.get(i, 0);
			}
		}
		return sum;
	}
	
}
