package edu.oregonstate.search;

import edu.stanford.nlp.dcoref.Document;
import Jama.Matrix;

/**
 * search interface, all search method need to implement this interface
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface ISearch {

	/** how many search step conducted by the search method */
	public int getSearchStep();
	
	/** how many violations */
	public int getViolations();
	
	/** weight */
	public double[] getWeight();
	
	/** set weight */
	public void setWeight(double[] weight);
	
	/** average total weight */
	public double[] getTotalWeight();
	
	/** set average total weight */
	public void setTotalWeight(double[] totalWeight);
	
	/** learn weight */
	public void trainingSearch();
	
	/** apply weight and calculate the result */
	public void testingSearch();
	
	/** set document */
	public void setDocument(Document document);
	
	
}
