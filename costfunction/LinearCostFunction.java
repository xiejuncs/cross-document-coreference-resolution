package edu.oregonstate.costfunction;

import java.util.List;

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
 		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
 		for (int i = 0; i < featureTemplate.size(); i++) {
 			String feature = featureTemplate.get(i);
 			double value = features.getCount(feature);
 			sum += value * model[i];
 		}
 		return sum;
 	}
	
}
