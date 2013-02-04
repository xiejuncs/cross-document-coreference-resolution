package edu.oregonstate.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.method.Dagger;
import edu.oregonstate.training.ITraining;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;

/**
 * Learn the weight
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class StructuredPerceptron implements IClassifier {

	/* experiment property file */
	private final Properties mProps;

	/* the total number of iterations */
	private final int mIterations;
	
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
	
	/* the weight used for keeping track of the progress */
	private List<double[]> weights;
	
	/* the length of the features */
	private final int length;
	
	/* training model */
	private final ITraining trainingModel;
	
	/* learning rate constant or not */
	private final boolean learningRateConstant;
	
	/**
	 * constructor
	 */
	public StructuredPerceptron() {
		mProps = ExperimentConstructor.experimentProps;
		experimentFolder = ExperimentConstructor.experimentResultFolder;
		mIterations = Integer.parseInt(mProps.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP, "1"));
		logFile = ExperimentConstructor.logFile;
		trainingTopics = ExperimentConstructor.trainingTopics;
		testingTopics = ExperimentConstructor.testingTopics;
		modelIndex = 0;
		String trainingStyle = mProps.getProperty(EecbConstants.TRAINING_STYLE_PROP, "OnlineTobatch");
		trainingModel = EecbConstructor.createTrainingModel(trainingStyle);
		String[] featureTemplate = FeatureFactory.getFeatures();
		length = featureTemplate.length;
		weights = new ArrayList<double[]>();
		
		learningRateConstant = Boolean.parseBoolean(mProps.getProperty(EecbConstants.LEARING_RATE_CONSTANT_PROP, "false"));
	}
	
	/**
	 * use zero vector to train the model
	 */
	public Parameter train(List<String> paths, int index) {
		ResultOutput.writeTextFile(logFile, "\n Begin classification: ");
		ResultOutput.writeTextFile(logFile, "\n Structured Perceptron with Iteration : " + mIterations);
		
		// model index
		modelIndex = index;
		double[] weight = new double[length];
		Parameter para = new Parameter(weight);

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
		double startingRate = Double.parseDouble(mProps.getProperty(EecbConstants.STRUCTUREDPERCEPTRON_STARTRATE_PROP, "0.1"));
		double endRate = 0.0;
		if (learningRateConstant) {
			endRate = startingRate;
		}
		double[] learningRates = DoubleOperation.createDescendingArray(startingRate, endRate, mIterations);
		
		Dagger dagger = new Dagger();
		
		// whether do post process on the document
		boolean trainPostProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
		boolean testPostProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
		
		// do gradient update
		for (int i = 0; i < mIterations; i++) {
			double learningRate = learningRates[i];
			weights.add(para.getWeight());
			ResultOutput.writeTextFile(logFile, "the " + modelIndex + "'s model 's " + i + "iteration");
			ResultOutput.printParameter(para, logFile);
			
			// shuffle the path
			Collections.shuffle(paths);
			int violations = para.getNoOfViolation();
			
			// do weight update
			para = trainingModel.train(paths, para, learningRate);
			
			// print number of violated constraint
			int afterviolation = para.getNoOfViolation();
			ResultOutput.writeTextFile(experimentFolder + "/violation-" + modelIndex +".csv", (afterviolation - violations) + "\t" + para.getNumberOfInstance());
			
			// train the other models of Dagger
//			double[] learnedWeight = dagger.generateWeightForTesting(para);
//			dagger.testDocument(trainingTopics, learnedWeight, modelIndex, i, trainPostProcess, "classification-training", false);
//			dagger.testDocument(testingTopics, learnedWeight, modelIndex, i, testPostProcess, "classification-testing", false);
		}
		
		// calculate the weight difference between the previous iteration and the current iteration
		DoubleOperation.calcualateWeightDifference(weights, experimentFolder + "/weight-difference-"+ modelIndex + ".csv");
		DoubleOperation.printWeightNorm(weights, experimentFolder + "/weight-norm-"+ modelIndex + ".csv");
		
		return para;
	}
	
	/**
	 * train the model
	 */
	public Parameter train(String path, Parameter para) {
		return para;
	}
	
}