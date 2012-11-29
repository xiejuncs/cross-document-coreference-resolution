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
	
	public void calculateLossFunction();
	
	public void setDocument(Document document);
	
	public void setState(State<CorefCluster> state);
	
	public double[] getLossScore();
	
	public double[] getMetricScore();
	
	public String getDetailScoreInformation();
}
