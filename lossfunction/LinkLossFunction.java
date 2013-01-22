package edu.oregonstate.lossfunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;

/**
 * Linked based loss function, which is defined as equation 1 in the Stanford's paper: Joint Entity and Event Coreference Resolution across documents
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LinkLossFunction implements ILossFunction {

	
	public double[] calculateLossFunction(Document document, State<CorefCluster> state) {
		double[] scores = new double[3];
		
		// get cluster
		String[] ids = state.getID().split("-");
		Integer i_id = Integer.parseInt(ids[0]);
		CorefCluster cluster = state.getState().get(i_id);
		
		// mentions
		Set<Mention> mentions = cluster.corefMentions;
		List<Mention> allMentions = new ArrayList<Mention>();
		for (Mention mention : mentions) {
			allMentions.add(mention);
		}
		
		// for mention pair
		Map<Integer, Mention> goldMentions = document.allGoldMentions;
		int size = allMentions.size();
		double noOfLinks = 0.0;
		double correct = 0.0;
		for (int i = 0; i < size; i++) {
			Mention m1 = allMentions.get(i);
			
			for (int j = 0; j < i; j++) {
				Mention m2 = allMentions.get(j);
				noOfLinks += 1.0;
				
				if (goldMentions.containsKey(m1.mentionID) && goldMentions.containsKey(m2.mentionID)) {
					if (goldMentions.get(m1.mentionID).goldCorefClusterID == goldMentions.get(m2.mentionID).goldCorefClusterID) {
						correct += 1.0;
					}
				}
			}
		}
		
		scores[0] = correct / noOfLinks;
		
		return scores;
	}
	
	/* un-implemented */
	public double[] getMetricScore(Document document){
		return new double[3];
	}
	
}
