package edu.oregonstate.costfunction;

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

	// calculate cost function according to features and the model
	public double calculateCostFunction(Counter<String> features, double[] model);
}

