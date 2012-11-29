package edu.oregonstate.classifier;

import java.util.*;

import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.general.PriorityQueue;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;

/**
 * The interface of classifier, all methods implemented with this interface should implement all methods, maybe do nothing
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface IClassifier {

	public void train();
	
	public void setRightLinks(Set<String> rightLinks);
	
	public void setStatesLossFunction(PriorityQueue<State<CorefCluster>> statesLossFunction);
	
	public void setBeam(FixedSizePriorityQueue<State<CorefCluster>> beam);
	
	public void setBestState(State<CorefCluster> bestState);
	
	public int getViolations();
	
	public void setWeight(double[] weight);
	
	public double[] getWeight();
	
	public void setTotalWeight(double[] totalWeight);
	
	public double[] getTotalWeight();
	
	public void setState(Map<String, State<CorefCluster>> states);
	
	public void setSearchStep(int searchStep);
	
	public void setPreviousBestState(State<CorefCluster> previousBestState);
	
	public void setBestScore(double bestScore);
}
