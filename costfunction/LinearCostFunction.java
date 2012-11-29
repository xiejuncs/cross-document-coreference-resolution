package edu.oregonstate.costfunction;

import Jama.Matrix;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.DoubleOperation;
import edu.stanford.nlp.stats.Counter;

public class LinearCostFunction implements ICostFunction {
	
	/** features */
	private Counter<String> mFeatures;
	
	/** model */
	private double[] mModel;
	
	public LinearCostFunction() {
	}
	
	public void setFeatures(Counter<String> features) {
		mFeatures = features;
	}
	
	public void setWeight(double[] model) {
		mModel = model;
	}
	
	/** 
	 * according to feature vector and model vector, calculate the cost
	 */
	public double calculateCostFunction() {
 		double sum = 0.0;
 		for (int i = 0; i < Feature.featuresName.length; i++) {
 			String feature = Feature.featuresName[i];
 			double value = mFeatures.getCount(feature);
 			sum += value * mModel[i];
 		}
 		return sum;
 	}
	
}
