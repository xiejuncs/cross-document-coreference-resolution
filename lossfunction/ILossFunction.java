package edu.oregonstate.lossfunction;

import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * the interface of Loss Functions
 * 
 * There are a lot of loss functions, for example, highe loss, 0-1 loss, hamming loss.
 * Through this class, given different object, the loss function can be calculated.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface ILossFunction {
	
	/* calculate loss function */
	public double[] calculateLossFunction(Document document, State<CorefCluster> state);
	
	/* the detail information of a score */
	public String getDetailScoreInformation();
	
	/* scoring the document */
	public double[] getMetricScore(Document document);
}
