package edu.oregonstate.costfunction;

/**
 * the interface of Cost Function,
 * 
 * The most used cost function is linear combination of features
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface CostFunction {

	public double calculateCostFunction(Object T);
}

