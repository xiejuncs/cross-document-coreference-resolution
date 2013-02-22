package edu.oregonstate.training;

import java.util.List;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;

/**
 * AROW Implementation based on Online training style
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class AROWOnline extends ITraining {

	/**
	 * implement the batch 
	 */
	public Parameter train(List<String> paths, Parameter para, double learningRate) {
		double[] previousWeight = para.getWeight();
		int violation = para.getNoOfViolation();
		int numberOfInstance = 0;
		
		// use to update weight
		Parameter finalParameter = para.makeCopy();
		double[] finalWeight = finalParameter.getWeight();
		double[] finalTotalWeight = finalParameter.getTotalWeight();
		double[][] finalVariance = finalParameter.getVariance();
		
		for (String path : paths) {
			List<List<List<String>>> dataset = reader.readData(path);
			List<List<String>> goodDataset = dataset.get(0);
			List<List<String>> badDataset = dataset.get(1);
			
			List<Integer> randomLists = createRandomIndex(goodDataset.size());
			
			for (int i = 0; i < randomLists.size(); i++){
				int index = randomLists.get(i);
				
				List<String> goodRecords = goodDataset.get(index);
				List<String> badRecords = badDataset.get(index);
				// get the data
				List<State<CorefCluster>> goodStates = reader.processString(goodRecords);
				List<State<CorefCluster>> badStates = reader.processString(badRecords);
				
				if (!incorporateZeroVector) {
					if (reader.isAllZero(goodStates)) continue;
				}
				
				// form constraint
				for (State<CorefCluster> goodState : goodStates) {
					for (State<CorefCluster> badState : badStates) {
						numberOfInstance += 1;
						
						// if loss score equal, do not consider this kind of constraint
						double gLossScore = goodState.getF1Score();
						double bLossScore = badState.getF1Score();
						if (gLossScore == bLossScore) {
							continue;
						}
						
						// get the features of good state and bad state 
						double[] gNumericalFeatures = goodState.getNumericalFeatures();
						double[] bNumericalFeatures = badState.getNumericalFeatures();
						
						double goodCostScoreForCounting = DoubleOperation.time(previousWeight, gNumericalFeatures);
						double badCostScoreForCounting = DoubleOperation.time(previousWeight, bNumericalFeatures);
						if (goodCostScoreForCounting <= badCostScoreForCounting) {
							violation += 1;
						}
						
						double[] feature = DoubleOperation.minus(gNumericalFeatures, bNumericalFeatures);
						double margin = 0.0;
						if (enableNormalizeWeight) {
							double[] normalizedWeight = DoubleOperation.normalize(finalWeight);
							margin = DoubleOperation.time(normalizedWeight, feature);
						} else {
							margin = DoubleOperation.time(finalWeight, feature);
						}
						
						if (margin < 1) {
							double beta = 1 / ( DoubleOperation.transformation(feature, finalVariance) + mHyperParameter );
							double alpha = Math.max(0, beta * (1 - DoubleOperation.time(feature, finalWeight)));
							double constant = alpha;
							double[] delta = DoubleOperation.time(DoubleOperation.matrixTime(finalVariance, feature), constant) ;
							boolean zeroVector = DoubleOperation.isZeroVector(delta);

							// update the weight and variance
							if (!zeroVector) {
								finalWeight = DoubleOperation.add(finalWeight, delta);
								finalTotalWeight = DoubleOperation.add(finalTotalWeight, finalWeight);

								double[] sumX = DoubleOperation.matrixTime(finalVariance, feature);
								double[][] sumXX = DoubleOperation.vectorProduct(sumX, feature);
								double[][] betaSumXX = DoubleOperation.time(sumXX, beta);
								double[][] betaSumXXSum = DoubleOperation.time(betaSumXX, finalVariance);
								finalVariance = DoubleOperation.matrixMinus(finalVariance, betaSumXXSum);
							}
						}
						
					}
				}
			}
		}
		
		return new Parameter(finalWeight, finalVariance, finalTotalWeight, violation, numberOfInstance);
	}
	
}
