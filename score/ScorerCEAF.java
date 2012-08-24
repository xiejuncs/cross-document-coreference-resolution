package edu.oregonstate.score;

import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;

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
		
	}
	
	/**
	 * calculate recall according to the equation 6 in the paper
	 */
	protected void calculateRecall(Document doc){
		
	}
}
