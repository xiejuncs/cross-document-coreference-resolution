package edu.oregonstate.costfunction;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
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
 		for (int i = 0; i < ExperimentConstructor.features.length; i++) {
 			String feature = ExperimentConstructor.features[i];
 			double value = mFeatures.getCount(feature);
 			sum += value * mModel[i];
 		}
 		return sum;
 	}
	
}
