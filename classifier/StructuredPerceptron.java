package edu.oregonstate.classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.EecbConstants;

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
	
	/* logFile */
	private final String logFile;
	
	public StructuredPerceptron() {
		mProps = ExperimentConstructor.experimentProps;
		mEpoch = Integer.parseInt(mProps.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP, "1"));
		logFile = ExperimentConstructor.logFile;
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
	private Parameter trainModel(List<String> dataset, Parameter para, double learningRate) {
		double[] weight = para.getWeight();
		int violations = para.getNoOfViolation();
		int length = weight.length;
		double[] finalWeight = new double[length];
		double[] finalTotalWeight = new double[length];
		System.arraycopy(weight, 0, finalWeight, 0, length);
		System.arraycopy(para.getTotalWeight(), 0, finalTotalWeight, 0, length);

		for (String data : dataset) {
			String[] features = data.split(";");
			String goodFeatures = features[0];
			String badFeature = features[1];
			double[] goodNumericFeatures = DoubleOperation.transformString(goodFeatures, ",");
			double[] badNumericFeatures = DoubleOperation.transformString(badFeature, ",");

			double goodCostScore = DoubleOperation.time(finalWeight, goodNumericFeatures);
			double badCostScore = DoubleOperation.time(finalWeight, badNumericFeatures);

			// violated constraint
			if (goodCostScore <= badCostScore) {
				violations += 1;
				double[] direction = DoubleOperation.minus(goodNumericFeatures, badNumericFeatures);
				double[] term = DoubleOperation.time(direction, learningRate);
				finalWeight = DoubleOperation.add(finalWeight, term);
				finalTotalWeight = DoubleOperation.add(finalTotalWeight, finalWeight);
			}
		}

		return new Parameter(finalWeight, finalTotalWeight, violations);
	}
	
	/**
	 * train the model according to lots of files
	 */
	public Parameter train(List<String> paths, Parameter para) {
		ResultOutput.writeTextFile(logFile, "\n Begin classification: ");
		ResultOutput.writeTextFile(logFile, "\nStructured Perceptron with Iteration : " + mEpoch);
		double[] learningRates = DoubleOperation.createDescendingArray(1, 0, mEpoch);
		ResultOutput.writeTextFile(logFile, "\n Learning Rates : " + DoubleOperation.printArray(learningRates));
		
		for (int i = 0; i < mEpoch; i++) {
			double learningRate = learningRates[i];
			ResultOutput.writeTextFile(logFile, "\n the " + i + "th iteration with learning rate : " + learningRate);
			for (String path : paths) {
				List<String> dataset = readData(path);
				
				para = trainModel(dataset, para, learningRate);
			}
			
		}
		
		return para;
	}
	
	
	
	/**
	 * use zero vector to train the model
	 */
	public Parameter train(List<String> paths) {
		String[] featureTemplate = FeatureFactory.getFeatures();
		int length = featureTemplate.length;
		double[] weight = new double[length];
		double[] totalWeight = new double[length];
		int violation = 0;
		Parameter para = new Parameter(weight, totalWeight, violation);
		Parameter trainedPara = train(paths, para);
		
		return trainedPara;
	}
	

	/**
	 * read data
	 * 	
	 * @param path
	 * @return
	 */
	private List<String> readData(String path) {
		List<String> datas = new ArrayList<String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				datas.add(line);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return datas;
	}
	
}
