package edu.oregonstate.costfunction;

import edu.oregonstate.features.FeatureFactory;
import edu.stanford.nlp.stats.Counter;

public class LinearCostFunction implements ICostFunction {
	
	public LinearCostFunction() {
	}
	
	/** 
	 * according to feature vector and model vector, calculate the cost
	 */
	public double calculateCostFunction(Counter<String> features, double[] model) {
 		double sum = 0.0;
 		String[] featureTemplate = FeatureFactory.getFeatureTemplate();
 		for (int i = 0; i < featureTemplate.length; i++) {
 			String feature = featureTemplate[i];
 			double value = features.getCount(feature);
 			sum += value * model[i];
 		}
 		return sum;
 	}
	
}
