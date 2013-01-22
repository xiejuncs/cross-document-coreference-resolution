package edu.oregonstate.example;

import java.util.Calendar;
import java.util.List;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.LargetFileReading;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;

public class Training {

	public static void main(String[] args) {
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println(timeStamp);
		String path = "/nfs/guille/xfern/users/xie/Experiment/corpus/TEMPORYRESUT/Mon-Jan-21-16-39-06-PST-2013-trp-tep-f-os-BeamSearch-StructuredPerceptron-none-trP-teP-Pairwise/43/data/2000";
		
		int length = 39;
		double[] weight = new double[length];
		double[] totalWeight = new double[length];
		int violation = 0;
		Parameter para = new Parameter(weight, totalWeight, violation);
		System.out.println(path);
		LargetFileReading reader = new LargetFileReading();
		List<List<List<String>>> dataset = reader.readData(path);
		System.out.println(dataset.size());
		List<List<String>> goodDataset = dataset.get(0);
		List<List<String>> badDataset = dataset.get(1);
		for (int i = 0; i < goodDataset.size(); i++){
			System.out.println(i);
			List<String> goodRecords = goodDataset.get(i);
			List<String> badRecords = badDataset.get(i);
			para = trainModel(goodRecords, badRecords, para, 0.001);
		}
		System.out.println(para.getNoOfViolation());
		
		System.out.println("done");
		timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println(timeStamp);
	}
	
	
	private static Parameter trainModel(List<String> goodRecords, List<String> badRecords, Parameter para, double learningRate) {
		// do not update the parameter, copy a new parameter
		Parameter finalParameter = para.makeCopy();
		double[] finalWeight = finalParameter.getWeight();
		double[] finalTotalWeight = finalParameter.getTotalWeight();
		int mViolations = finalParameter.getNoOfViolation();
		int length = finalWeight.length;
		int numberOfInstance = 0;
		
		// get the data
		List<State<CorefCluster>> goodStates = LargetFileReading.processString(goodRecords);
		List<State<CorefCluster>> badStates = LargetFileReading.processString(badRecords);
		
		double[] fixedWeight = new double[length];
		System.arraycopy(finalWeight, 0, fixedWeight, 0, length);
		
		// form constraint and do update
		for (State<CorefCluster> goodState : goodStates) {
			for (State<CorefCluster> badState : badStates) {
				numberOfInstance += 1;
				// get the loss score of bad state and good state
				// if their loss score are equal, skip this kind of constraint
				double gLossScore = goodState.getF1Score();
				double bLossScore = badState.getF1Score();
				if (gLossScore == bLossScore) {
					continue;
				}
				
				// get the features of good state and bad state 
				double[] gNumericalFeatures = goodState.getNumericalFeatures();
				double[] bNumericalFeatures = badState.getNumericalFeatures();
					
				// calculate the action score of good state and bad state	
				double goodCostScore = DoubleOperation.time(fixedWeight, gNumericalFeatures);
				double badCostScore = DoubleOperation.time(fixedWeight, bNumericalFeatures);

				// violated constraint
				if (goodCostScore <= badCostScore) {
					mViolations += 1;
					double[] direction = DoubleOperation.minus(gNumericalFeatures, bNumericalFeatures);
					double[] term = DoubleOperation.time(direction, learningRate);
					finalWeight = DoubleOperation.add(finalWeight, term);
					finalTotalWeight = DoubleOperation.add(finalTotalWeight, finalWeight);
				}
			}
		}

		return new Parameter(finalWeight, finalTotalWeight, mViolations);
	}
}
