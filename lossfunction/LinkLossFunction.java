package edu.oregonstate.lossfunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.oregonstate.search.State;
import edu.oregonstate.util.Command;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;

/**
 * Linked based loss function, which is defined as equation 1 in the Stanford's paper: Joint Entity and Event Coreference Resolution across documents
 * 
 * define the pairwise accuracy
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LinkLossFunction implements ILossFunction {

	
	public double[] calculateLossFunction(Document document, State<CorefCluster> state) {
		Command.generateStateDocument(document, state);
		double[] scores = calculateScore(document);
		return scores;
	}
	
	private double[] calculateScore(Document document) {
		double[] scores = new double[3];
		double truePositve = calculateTruePositive(document);
		double trueNegative = calculateTrueNegative(document);
		double falsePositive = calculateFalsePositive(document);
		double falseNegative = calculateFalseNegative(document);
		
		double score = (truePositve + trueNegative) / ( trueNegative + truePositve + falsePositive + falseNegative);
		scores[0] = score;
		
		return scores;
	}
	
	private double calculateTruePositive(Document doc) {
		double truePositive = 0.0;
		
		Map<Integer, Mention> goldMentions = doc.allGoldMentions;
		for(CorefCluster c : doc.corefClusters.values()){
			for(Mention m1 : c.getCorefMentions()){
				for(Mention m2 : c.getCorefMentions()) {
					if(m1.mentionID >= m2.mentionID) continue;
					if(goldMentions.containsKey(m1.mentionID) && goldMentions.containsKey(m2.mentionID)
							&& goldMentions.get(m1.mentionID).goldCorefClusterID == goldMentions.get(m2.mentionID).goldCorefClusterID){
						truePositive += 1;
					}
				}
			}
		}
		
		return truePositive;
	}
	
	private double calculateTrueNegative(Document doc) {
		double trueNegative = 0.0;
		
		Map<Integer, Mention> goldMentions = doc.allGoldMentions;
		Map<Integer, Mention> predictedMentions = doc.allPredictedMentions;
		List<Mention> allPredictedMentions = new ArrayList<Mention>();
		for (Integer key : predictedMentions.keySet()) {
			Mention mention = predictedMentions.get(key);
			allPredictedMentions.add(mention);
		}
		
		for (int i = 0; i < allPredictedMentions.size(); i++) {
			Mention mi = allPredictedMentions.get(i);
			for (int j = 0; j < i; j++) {
				Mention mj = allPredictedMentions.get(j);
				if (mi.corefClusterID != mj.corefClusterID) {
					if(goldMentions.containsKey(mi.mentionID) && goldMentions.containsKey(mj.mentionID)
							&& goldMentions.get(mi.mentionID).goldCorefClusterID != goldMentions.get(mj.mentionID).goldCorefClusterID){
						trueNegative += 1.0;
					}
				}
				
			}
		}
		
		return trueNegative;
	}
	
	private double calculateFalsePositive(Document doc) {
		double falsePositive = 0.0;
		
		Map<Integer, Mention> goldMentions = doc.allGoldMentions;
		for(CorefCluster c : doc.corefClusters.values()){
			for(Mention m1 : c.getCorefMentions()){
				for(Mention m2 : c.getCorefMentions()) {
					if(m1.mentionID >= m2.mentionID) continue;
					if(goldMentions.containsKey(m1.mentionID) && goldMentions.containsKey(m2.mentionID)
							&& goldMentions.get(m1.mentionID).goldCorefClusterID != goldMentions.get(m2.mentionID).goldCorefClusterID){
						falsePositive += 1;
					}
				}
			}
		}
		
		return falsePositive;
		
	}
	
	private double calculateFalseNegative(Document doc) {
		double falseNegative = 0.0;
		
		Map<Integer, Mention> predictedMentions = doc.allPredictedMentions;
		for(CorefCluster g : doc.goldCorefClusters.values()) {
			for(Mention m1 : g.getCorefMentions()){
				for(Mention m2 : g.getCorefMentions()) {
					if(m1.mentionID >= m2.mentionID) continue;
					if(predictedMentions.containsKey(m1.mentionID) && predictedMentions.containsKey(m2.mentionID)
							&& predictedMentions.get(m1.mentionID).corefClusterID != predictedMentions.get(m2.mentionID).corefClusterID){
						falseNegative += 1;
					}
				}
			}
		}
		
		return falseNegative;
	}
	
	/* un-implemented */
	public double[] getMetricScore(Document document){
		return calculateScore(document);
	}
	
}


//get cluster
//		String[] ids = state.getID().split("-");
//		Integer i_id = Integer.parseInt(ids[0]);
//		CorefCluster cluster = state.getState().get(i_id);
//		
//		// mentions
//		Set<Mention> mentions = cluster.corefMentions;
//		List<Mention> allMentions = new ArrayList<Mention>();
//		for (Mention mention : mentions) {
//			allMentions.add(mention);
//		}
//		
//		// for mention pair
//		Map<Integer, Mention> goldMentions = document.allGoldMentions;
//		int size = allMentions.size();
//		double noOfLinks = 0.0;
//		double correct = 0.0;
//		for (int i = 0; i < size; i++) {
//			Mention m1 = allMentions.get(i);
//			
//			for (int j = 0; j < i; j++) {
//				Mention m2 = allMentions.get(j);
//				noOfLinks += 1.0;
//				
//				if (goldMentions.containsKey(m1.mentionID) && goldMentions.containsKey(m2.mentionID)) {
//					if (goldMentions.get(m1.mentionID).goldCorefClusterID == goldMentions.get(m2.mentionID).goldCorefClusterID) {
//						correct += 1.0;
//					}
//				}
//			}
//		}
//		
//		scores[0] = correct / noOfLinks;