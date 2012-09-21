package edu.oregonstate.score;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;


import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;

/**
 * CEAF score implementation (See paper: Evaluation metrics for End-to-End Coreference Resolution Systems)
 * 
 * CEAF applies a similarity metric (which should be either mention based or entity based) for each pair of 
 * entities (i.e. a set of mentions) to measure the goodness of each possible alignment. The best mapping is
 * used for calculating CEAF precision, recall and F-measure.
 * 
 * There are two types similarity metric, called phi3 and phi4
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ScorerCEAF extends CorefScorer {
	
	// update all fields of CorefScorer to public. 
	public ScorerCEAF() {
		super();
		scoreType = ScoreType.CEAF;
	}
	
	/**
	 * calculate precision according to the equation 5 in the paper
	 */
	protected void calculatePrecision(Document doc){
		Map<Integer, CorefCluster> response = doc.corefClusters;
		Map<Integer, CorefCluster> reference = doc.goldCorefClusters;
		precisionNumSum = scoreHelper(reference, response);
		precisionDenSum = scoreHelper(response, response);
	}

	// calculate the simialrity	
	public double similarity(CorefCluster responseCluster, CorefCluster referenceCluster) {
		Set<Mention> responseMentions = responseCluster.corefMentions;
		Set<Mention> referenceMentions = referenceCluster.corefMentions;
		List<Integer> responseMentionIDs = new ArrayList<Integer>();
		List<Integer> referenceMentionIDs = new ArrayList<Integer>();
		
		for (Mention mention : responseMentions) {
			responseMentionIDs.add(mention.mentionID);
		}
		for (Mention mention : referenceMentions ) {
			referenceMentionIDs.add(mention.mentionID);
		}
		int responseSize = responseMentionIDs.size();
		int referenceSize = referenceMentionIDs.size();	
		responseMentionIDs.retainAll(referenceMentionIDs);
		int overlap = responseMentionIDs.size();
		return (2 * overlap ) / (responseSize * referenceSize); 			
	} 

	// calculate the cost function
	public double scoreHelper(Map<Integer, CorefCluster> reference, Map<Integer, CorefCluster> response) {
		double cost = 0.0;
		if (reference.size() == 0 || response.size() == 0) return 0.0;
		int size = reference.size() >= response.size() ? reference.size() : response.size();
		double[][] scores = new double[size][size];
		double max = 1.0;
		for (double[] score : scores) {
			Arrays.fill(score, max);
		}
		Set<Integer> responseSet = response.keySet();
		Iterator<Integer> responseIt = responseSet.iterator();
		int i = 0;
		int j = 0;
		while (responseIt.hasNext()) {
			CorefCluster responseCluster = response.get(responseIt.next());
			j = 0;
			Set<Integer> referenceSet = reference.keySet();
			Iterator<Integer> referenceIt = referenceSet.iterator();
			while (referenceIt.hasNext()) {
				CorefCluster referenceCluster = reference.get(referenceIt.next());
				scores[j][i] = max - similarity(responseCluster, referenceCluster); // how to calculate the similarity 
				j++;
			} 
			i++;
		}
		
		AssignmentProblem ap = new AssignmentProblem(scores);
		int[][] solution = ap.solve(new HungarianAlgorithm());
		for (i = 0; i < solution.length; i++) {
    			if (solution[i][0] >= 0) {
      				cost += max - scores[solution[i][0]][i]; //how to calculate this 
    			}
  		}		
		
  		return cost;
	}
	
	/**
	 * calculate recall according to the equation 6 in the paper
	 */
	protected void calculateRecall(Document doc){
		Map<Integer, CorefCluster> response = doc.corefClusters;
        Map<Integer, CorefCluster> reference = doc.goldCorefClusters;
		recallNumSum = scoreHelper(reference, response);
		recallDenSum = scoreHelper(reference, reference);
	}

}
