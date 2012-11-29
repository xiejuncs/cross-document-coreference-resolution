package edu.oregonstate.costfunction;

import Jama.Matrix;
import edu.stanford.nlp.stats.Counter;

/**
 * the interface of Cost Function,
 * 
 * The most used cost function is linear combination of features
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface ICostFunction {

	public double calculateCostFunction();
	
	public void setFeatures(Counter<String> features);
	
	public void setWeight(double[] model);

}

