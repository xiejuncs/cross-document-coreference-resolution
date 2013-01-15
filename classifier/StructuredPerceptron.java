package edu.oregonstate.classifier;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	private Parameter trainModel(List<double[]> dataset, Parameter para, double learningRate) {
		double[] weight = para.getWeight();
		int mViolations = para.getNoOfViolation();
		int length = weight.length;
		double[] finalWeight = new double[length];
		double[] finalTotalWeight = new double[length];
		System.arraycopy(weight, 0, finalWeight, 0, length);
		System.arraycopy(para.getTotalWeight(), 0, finalTotalWeight, 0, length);

		for (double[] data : dataset) {
			double[] goodNumericFeatures = new double[length];
			double[] badNumericFeatures = new double[length];
			System.arraycopy(data, 0, goodNumericFeatures, 0, length);
			System.arraycopy(data, length, badNumericFeatures, 0, length);

			double goodCostScore = DoubleOperation.time(finalWeight, goodNumericFeatures);
			double badCostScore = DoubleOperation.time(finalWeight, badNumericFeatures);

			// violated constraint
			if (goodCostScore <= badCostScore) {
				mViolations += 1;
				double[] direction = DoubleOperation.minus(goodNumericFeatures, badNumericFeatures);
				double[] term = DoubleOperation.time(direction, learningRate);
				finalWeight = DoubleOperation.add(finalWeight, term);
				finalTotalWeight = DoubleOperation.add(finalTotalWeight, finalWeight);
			}
		}

		ResultOutput.writeTextFile(logFile, "the violated constraint : " + (mViolations - para.getNoOfViolation()));
		return new Parameter(finalWeight, finalTotalWeight, mViolations);
	}
	
	/**
	 * train the model according to lots of files
	 */
	public Parameter train(List<String> paths, Parameter para) {
		ResultOutput.writeTextFile(logFile, "\n Begin classification: ");
		ResultOutput.writeTextFile(logFile, "\nStructured Perceptron with Iteration : " + mEpoch);
		double[] learningRates = DoubleOperation.createDescendingArray(1, 0, mEpoch);
		ResultOutput.writeTextFile(logFile, "\n Learning Rates : " + DoubleOperation.printArray(learningRates));
		boolean binary = Boolean.parseBoolean(mProps.getProperty(EecbConstants.IO_BINARY_PROP, "false"));
		
		for (int i = 0; i < mEpoch; i++) {
			double learningRate = learningRates[i];
			ResultOutput.writeTextFile(logFile, "\n the " + i + "th iteration with learning rate : " + learningRate);
			for (String path : paths) {
				List<double[]> dataset = null;
				if (binary) {
					dataset = readByteData(path);
				} else {
					dataset = readTextData(path);
				}
				
				ResultOutput.writeTextFile(logFile, path + " data size : " + dataset.size() );
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
	private List<double[]> readTextData(String path) {
		List<double[]> datas = new ArrayList<double[]>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				
				double[] data = DoubleOperation.transformString(line, ",");
				datas.add(data);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return datas;
	}
	
	/**
	 * read data from byte file
	 * 
	 * @param path
	 * @return
	 */
	private List<double[]> readByteData(String path) {
		DataInputStream dis = null;
		List<double[]> dataset = new ArrayList<double[]>();
		int length = FeatureFactory.getFeatures().length;
		try {
			dis = new DataInputStream(new FileInputStream(path));
			double[] datas = new double[2 * length];
			int i = 0;
			while (true) {
				double data = dis.readDouble();
				char ch = dis.readChar();
				datas[i] = data;
				if (ch == '\n') {
					dataset.add(datas);
					datas = new double[2 * length];
					i = 0;
					continue;
				}
				i++;
			}
		} catch (EOFException eof) {
			
		} catch (FileNotFoundException noFile) {
			
		} catch (IOException io) {
			
		} catch (Throwable anything) {
			
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException ignored) {
				}
			}

		}
		
		return dataset;
	}
}
