package edu.oregonstate.training;

import java.util.List;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;

public class Batch extends ITraining {
	
	/**
	 * implement the batch 
	 */
	public Parameter train(List<String> paths, Parameter para, double learningRate) {
		double[] previousWeight = para.getWeight();
		double[] previousTotalWeight = para.getTotalWeight();
		int violations = 0;
		int numberOfInstance = 0;
		
		double[] delta = new double[length];
		double[] totalDelta = new double[length];
		for (String path : paths) {
			List<List<List<String>>> dataset = reader.readData(path);
			List<List<String>> goodDataset = dataset.get(0);
			List<List<String>> badDataset = dataset.get(1);
			
			for (int index = 0; index < goodDataset.size(); index++){
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
						
						// calculate the action score of good state and bad state	
						double goodCostScoreForUpdating = DoubleOperation.time(previousWeight, gNumericalFeatures);
						double badCostScoreForUpdating = DoubleOperation.time(previousWeight, bNumericalFeatures);

						// violated current constraint
						if (goodCostScoreForUpdating <= badCostScoreForUpdating) {
							violations += 1;
							double[] direction = DoubleOperation.minus(gNumericalFeatures, bNumericalFeatures);
							
							// enable PA learning rate
							if (enablePALearningRate) {
								learningRate = calculatePALossLearningRate(gLossScore, bLossScore, direction, enablePALearningRateLossScore);
							}
							
							double[] term = DoubleOperation.time(direction, learningRate);
							delta = DoubleOperation.add(delta, term);
							totalDelta = DoubleOperation.add(totalDelta, delta);
						}
					}
				}
			}
		}
		
		double[] currentWeight = DoubleOperation.add(previousWeight, delta);
		double[] currentTotalWeight = DoubleOperation.add(previousTotalWeight, totalDelta);
		
		return new Parameter(currentWeight, currentTotalWeight, violations, numberOfInstance);
	}
	
}
