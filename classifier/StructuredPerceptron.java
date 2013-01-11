package edu.oregonstate.classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.DoubleOperation;
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
	
	/* update rate */
	private double updateStep;

	public StructuredPerceptron() {
		mProps = ExperimentConstructor.experimentProps;
		mEpoch = Integer.parseInt(mProps.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP, "1"));
	}

	/**
	 * train the model
	 */
	public Parameter train(String path, Parameter para) {
		double[] weight = para.getWeight();
		int violations = para.getNoOfViolation();
		int length = weight.length;
		double[] finalWeight = new double[length];
		double[] finalTotalWeight = new double[length];
		System.arraycopy(weight, 0, finalWeight, 0, length);
		System.arraycopy(para.getTotalWeight(), 0, finalTotalWeight, 0, length);
		
		List<String> datas = readMatrix(path);
		updateStep = Double.parseDouble(mProps.getProperty(EecbConstants.STRUCTUREDPERCEPTRON_RATE_PROP, "" + 1.0));
		
		for (int i = 0; i < mEpoch; i++) {
			
			// learn the weight using structured perceptron
			for (String data : datas) {
				String[] features = data.split("-");
				String goodFeatures = features[0];
				String badFeature = features[1];
				double[] goodNumericFeatures = DoubleOperation.transformString(goodFeatures, ",");
				double[] badNumericFeatures = DoubleOperation.transformString(badFeature, ",");

				double goodCostScore = DoubleOperation.time(weight, goodNumericFeatures);
				double badCostScore = DoubleOperation.time(weight, badNumericFeatures);
				
				// violated constraint
				if (goodCostScore <= badCostScore) {
					violations += 1;
					double[] direction = DoubleOperation.minus(goodNumericFeatures, badNumericFeatures);
					double[] term = DoubleOperation.time(direction, updateStep);
					finalWeight = DoubleOperation.add(finalWeight, term);
					finalTotalWeight = DoubleOperation.add(finalTotalWeight, weight);
				}
			}
		}
		
		return new Parameter(finalWeight, finalTotalWeight, violations);
	}
	
	/**
	 * train the model according to lots of files
	 */
	public Parameter train(List<String> paths, Parameter para) {
		for (String path : paths) {
			para = train(path, para);
		}
		
		return para;
	}
	

	/**
	 * read data
	 * 	
	 * @param path
	 * @return
	 */
	private List<String> readMatrix(String path) {
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
	
	public static void main(String[] args) {
		
	}
	
}
