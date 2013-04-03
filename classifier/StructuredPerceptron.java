package edu.oregonstate.classifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
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
	
	/* enable print the result of each iteration during training */
	private final boolean enablePrintIterationResult;
	
	/* print the result of the iteration for how many gap */
	private final int printIteartionGap;
	
	/**
	 * constructor
	 */
	public StructuredPerceptron() {
		mProps = ExperimentConstructor.experimentProps;
		experimentFolder = ExperimentConstructor.experimentFolder;
		mIterations = Integer.parseInt(mProps.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP, "5"));
		
		logFile = ExperimentConstructor.experimentLogFile;
		modelIndex = 0;
		String trainingStyle = mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_METHOD, "OnlineToBatch");
		trainingModel = EecbConstructor.createTrainingModel(trainingStyle);
		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
		length = featureTemplate.size();
		weights = new ArrayList<double[]>();
		
		learningRateConstant = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_PERCEPTRON_LEARINGRATE_CONSTANT, "false"));
		enablePrintIterationResult = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_ITERATION_RESULT, "false"));
		printIteartionGap = Integer.parseInt(mProps.getProperty(EecbConstants.CLASSIFIER_ITEARTION_GAP, "2"));
	}
	
	/**
	 * use zero vector to train the model
	 */
	public Parameter train(List<String> paths, int index) {
		ResultOutput.writeTextFile(logFile, "\nBegin to learn model : " + modelIndex);
		ResultOutput.writeTextFile(logFile, "\nStructured Perceptron with Iteration : " + mIterations);
		
		// model index
		modelIndex = index;
		double[] weight = new double[length];
		Parameter para = new Parameter(weight);

		Parameter trainedPara = train(paths, para);
		
		return trainedPara;
	}
	
	/**
	 * train the model according to lots of files
	 */
	public Parameter train(List<String> paths, Parameter para) {
		double startingRate = Double.parseDouble(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_PERCEPTRON_STARTRATE, "0.1"));
		double endRate = 0.0;
		if (learningRateConstant) {
			endRate = startingRate;
		}
		double[] learningRates = DoubleOperation.createDescendingArray(startingRate, endRate, mIterations);
		
		// do gradient update
		for (int i = 0; i < mIterations; i++) {
			double learningRate = learningRates[i];
			weights.add(para.getWeight());
			ResultOutput.writeTextFile(logFile, "the " + modelIndex + "'s model " + i + "iteration");
			// ResultOutput.printParameter(para, logFile);
			
			// shuffle the path
			Collections.shuffle(paths);
			int beforeViolation = para.getNoOfViolation();
			
			// do weight update
			para = trainingModel.train(paths, para, learningRate);
			
			// print number of violated constraint
			int afterviolation = para.getNoOfViolation();
			ResultOutput.writeTextFile(experimentFolder + "/violation/violation-" + modelIndex +".csv", (afterviolation - beforeViolation) + "\t" + para.getNumberOfInstance());
			
		}
		
		// calculate the weight difference between the previous iteration and the current iteration
		DoubleOperation.calcualateWeightDifference(weights, experimentFolder + "/weightdifference/weight-difference-"+ modelIndex + ".csv");
		DoubleOperation.printWeightNorm(weights, experimentFolder + "/weightnorm/weight-norm-"+ modelIndex + ".csv");
		
		return para;
	}
	
	/**
	 * train the model
	 */
	public Parameter train(String path, Parameter para) {
		return para;
	}
	
}