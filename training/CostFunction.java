package edu.oregonstate.training;

import Jama.Matrix;
import edu.oregonstate.features.Feature;
import edu.stanford.nlp.stats.Counter;

/**
 * 
 * use features to calculate a cost function.
 * This part we want to be flexible to a lot of features
 * 
 * Cost function or score function, they refer to the same
 * Cost function should be minimum, while score function should be maximum
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CostFunction {

	private Counter<String> mFeatures;
	private Matrix mModel;
	
	public CostFunction(Counter<String> features, Matrix model) {
		mFeatures = features;
		mModel = model;
	}
	
	/** according to feature vector and model vector, we calculate the cost */	
	public double calculateScore() {
 		double sum = 0.0;
 		for (int i = 0; i < mModel.getRowDimension(); i++) {
 			sum += mFeatures.getCount(Feature.featuresName[i]) * mModel.get(i, 0);
 		}
 		return sum;
 	}
}
