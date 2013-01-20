package edu.oregonstate.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.LargetFileReading;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.method.Dagger;
import edu.oregonstate.search.State;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;

/**
 * Learn the weight
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class StructuredPerceptron implements IClassifier {

	/* experiment property file */
	private final Properties mProps;

	/* the total epoch */
	private final int mEpoch;
	
	/* training topics */
	private final String[] trainingTopics;
	
	/* testing topics */
	private final String[] testingTopics;
	
	/* experiment folder */
	private final String experimentFolder;
	
	/* logFile */
	private final String logFile;
	
	/* model index */
	private int modelIndex;
	
	/* number of instance */
	private int numberOfInstance;
	
	public StructuredPerceptron() {
		mProps = ExperimentConstructor.experimentProps;
		experimentFolder = ExperimentConstructor.experimentResultFolder;
		mEpoch = Integer.parseInt(mProps.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP, "1"));
		logFile = ExperimentConstructor.logFile;
		trainingTopics = ExperimentConstructor.trainingTopics;
		testingTopics = ExperimentConstructor.testingTopics;
		numberOfInstance = 0;
		modelIndex = 0;
	}
	
	/**
	 * use zero vector to train the model
	 */
	public Parameter train(List<String> paths, int index) {
		ResultOutput.writeTextFile(logFile, "\n Begin classification: ");
		ResultOutput.writeTextFile(logFile, "\n Structured Perceptron with Iteration : " + mEpoch);
		
		// model index
		modelIndex = index;
		String[] featureTemplate = FeatureFactory.getFeatures();
		int length = featureTemplate.length;
		double[] weight = new double[length];
		double[] totalWeight = new double[length];
		int violation = 0;
		Parameter para = new Parameter(weight, totalWeight, violation);

		// store the structured perceptron intermediate result
		ResultOutput.writeTextFile(experimentFolder + "/classification-training.csv", "ANOTHER EXPERIMENT");
		ResultOutput.writeTextFile(experimentFolder + "/classification-testing.csv", "ANOTHER EXPERIMENT");
		
		Parameter trainedPara = train(paths, para);
		
		return trainedPara;
	}
	
	/**
	 * train the model according to lots of files
	 */
	public Parameter train(List<String> paths, Parameter para) {
		double[] learningRates = DoubleOperation.createDescendingArray(0.1, 0, mEpoch);
		ResultOutput.writeTextFile(logFile, "\n Learning Rates : " + DoubleOperation.printArray(learningRates));
		LargetFileReading reader = new LargetFileReading();
		Dagger dagger = new Dagger();
		
		// whether do post process on the document
		boolean trainPostProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
		boolean testPostProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
		
		// do stochastic gradient update
		List<double[]> weight = new ArrayList<double[]>();
		for (int i = 0; i < mEpoch; i++) {
			int violation = para.getNoOfViolation();
			double learningRate = learningRates[i];
			weight.add(para.getWeight());
			ResultOutput.writeTextFile(logFile, "\n the " + i + "th iteration with learning rate : " + learningRate);
			
			// read the data and do stochastic gradient update
			for (String path : paths) {
				List<List<List<State<CorefCluster>>>> dataset = reader.readData(path);
				para = trainModel(dataset, para, learningRate);
			}
			
			// print number of violated constraint
			int afterViolation = para.getNoOfViolation();
			ResultOutput.writeTextFile(experimentFolder + "/violation" + modelIndex +".csv", violation + "\t" + afterViolation + "\t" + (afterViolation - violation) + "\t" + numberOfInstance);
			
			// train the other models of Dagger
//			double[] learnedWeight = dagger.generateWeightForTesting(para);
//			dagger.testDocument(trainingTopics, learnedWeight, modelIndex, i, trainPostProcess, "classification-training", false);
//			dagger.testDocument(testingTopics, learnedWeight, modelIndex, i, testPostProcess, "classification-testing", false);
			
			// set the number of instance to 0
			numberOfInstance = 0;
		}
		
		// calculate the weight difference between the previous iteration and the current iteration
		DoubleOperation.calcualateWeightDifference(weight, experimentFolder + "/weight"+ modelIndex + ".csv");
		return para;
	}

	/**
	 * train the model
	 */
	public Parameter train(String path, Parameter para) {
		return para;
	}
	
	/**
	 * train the model based on dataset and parameter
	 * 
	 * @param dataset
	 * @param para
	 * @return
	 */
	private Parameter trainModel(List<List<List<State<CorefCluster>>>> dataset, Parameter para, double learningRate) {
		// do not update the parameter, copy a new parameter
		Parameter finalParameter = para.makeCopy();
		double[] finalWeight = finalParameter.getWeight();
		double[] finalTotalWeight = finalParameter.getTotalWeight();
		int mViolations = finalParameter.getNoOfViolation();
		int length = finalWeight.length;
		
		// get the data
		List<List<State<CorefCluster>>> goodDataset = dataset.get(0);
		List<List<State<CorefCluster>>> badDataset = dataset.get(1);
		
		// update the weight
		// constraints generated from one state should put together to update
		for (int i = 0; i < goodDataset.size(); i++) {
			
			// constraints generated from one state
			List<State<CorefCluster>> goodStates = goodDataset.get(i);
			List<State<CorefCluster>> badStates = badDataset.get(i);
			
			// set the fixed weight: do not focus on getting on high score, just focus
			// on minimize the number of violated constraints   
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
		}

		return new Parameter(finalWeight, finalTotalWeight, mViolations);
	}

}

//if (i == mEpoch - 1) {
//dagger.testDocument(trainingTopics, learnedWeight, i, 0, false, "classification-training", true);
//} else {
//dagger.testDocument(trainingTopics, learnedWeight, i, 0, false, "classification-training", false);
//}

/**
 * 
 * @param totalWeight
 * @param violation
 * @return
 */
//private double[] generateFixedWeight(double[] totalWeight, int violation) {
//	int length = totalWeight.length;
//	double[] otherTotalWeight = new double[length];
//	System.arraycopy(totalWeight, 0, otherTotalWeight, 0, length);
//	double[] learnedWeight = new double[length];
//	if (violation != 0) {
//		learnedWeight = DoubleOperation.divide(otherTotalWeight, violation);
//	}
//	return learnedWeight;
//}