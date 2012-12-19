package edu.oregonstate.classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;

/**
 * For the L2 regularized linear regression, the basic updating rule is:
 * \theta_{j} = \theta_{j} + \alpha \cdot ((y_{i} - f(x_{i}) )x_{ij} - \lamda \theta_{j} )
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LinearRegressionbyStochasticGradient {

	/* regularizer term */
	private double mRegularizer;
	
	/* training file Path */
	private String mTrainingFile;
	
	/* stopping criteria : use number of iteration or the norm difference between the current weight and the previous weight is lower than a threshold */
	private boolean mUseEpoch;
	
	/* the number of epoch */
	private int mEpoch;
	
	/* log file */
	private String logFile;
	
	/* sse error */
	private List<Double> SSE;
	
	public LinearRegressionbyStochasticGradient(String trainingFile, double regularizer, boolean useEpoch ) {
		this(trainingFile, regularizer, useEpoch, 10);
	}
	
	public LinearRegressionbyStochasticGradient(String trainingFile, double regularizer, boolean useEpoch, int epoch) {
		mRegularizer = regularizer;
		mTrainingFile = trainingFile;
		mUseEpoch = useEpoch;
		mEpoch = epoch;
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		logFile = timeStamp + "-linear-regression-log";
		SSE = new ArrayList<Double>();
	}
	
	/**
	 * Read a matrix from a comma separated file
	 * This method add a bias feature 1.0 into the data set
	 * N : the number of instances
	 * M : the number of dimension
	 * 
	 * @param fileName
	 * @return N * M
	 */
	public List<double[]> readMatrix() {
		List<double[]> data_array = new ArrayList<double[]>();
		boolean existNaN = false;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(mTrainingFile));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				String fields[] = line.split(",");
				double data[] = new double[fields.length + 1];
				data[0] = 1.0;
				for (int i = 0; i < fields.length; i++) {
					if (fields[i].equals("NaN")) {
						data[i + 1] = 0.0;
						existNaN = true;
					} else {
						data[i + 1] = Double.parseDouble(fields[i]);
					}
				}
				data_array.add(data);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("is there exist NaN: " + Boolean.toString(existNaN));

	    return data_array;
	}
	
	/**
	 * Returns the target values from the last column of a data set.
	 * the last column correspond to the target
	 * 
	 * @param data_set
	 * @return targets : 1 * N
	 */
	private double[] getTargets(List<double[]> dataSet) {
		int rows = dataSet.size();
		double[] targets = new double[rows];
		for (int i = 0; i < rows; i++) {
			double[] data = dataSet.get(i);
			double target = data[data.length - 1];
			targets[i] = target; 
		}
		
	    return targets;
	}
	
	/**
	 * generate features for the data set.
	 * 
	 * @param data_set
	 * @return
	 */
	private List<double[]> getFeatures(List<double[]> dataSet) {
		List<double[]> features = new ArrayList<double[]>();
		for (double[] data : dataSet) {
			int length = data.length;
			double[] feature = new double[length - 1];
			System.arraycopy(data, 0, feature, 0, length - 1);
			features.add(feature);
		}
		return features;
	}
	
	/**
	 * Learn the weight using iterative stochastic gradient algorithm 
	 * 
	 * @param features
	 * @param targets
	 * @return
	 */
	public double[] trainLinearRegressionModel(List<double[]> dataSet) {
		// use the number of iterations or the weight convergence
		assert dataSet != null;
		
		int featureSize = dataSet.get(0).length - 1;
		double[] weight = new double[featureSize];
		double[] learningRates = DoubleOperation.createDescendingArray(1.0, 0.0, mEpoch);
		System.out.println(" learning rates used in the experiment : " + DoubleOperation.printArray(learningRates));
		
		if (mUseEpoch) {
			for (int i = 0; i < mEpoch; i++) {
				System.out.println("the itreation number : " + (i + 1));
				double learningRate = learningRates[i];
				System.out.println("learning rate : " + learningRate);
				String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
				System.out.println(timeStamp);
				
				// shuffle the data set
				Collections.shuffle(dataSet);
				
				// train the model
				weight = train(dataSet, weight, learningRate);
				
				System.out.println(DoubleOperation.printArray(weight));
				
				System.out.println("finish the iteration number :" + (i + 1));
				timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
				System.out.println(timeStamp);
				
				if (getEndCondition()) {
					break;
				}
			}
			
		} else {
			
		}
		
		return weight;
	}
	
	/* the end condition */
	private boolean getEndCondition() {
		boolean end = false;
		int size = SSE.size();
		double currentSSE = SSE.get(size - 1);
		double previousSSE = 0.0;
		if (size > 1){
			previousSSE = SSE.get(size - 2);
		}
		
		if (Math.abs(currentSSE - previousSSE) < 1) {
			end = true;
		}
		
		return end;
	}
	
	/**
	 * train the linear regression model for each epoch.
	 * 
	 * See the note : (@link http://www.hongliangjie.com/notes/lr.pdf)
	 * 
	 * After train, do testing on the dataset using the SSE cost function
	 * 
	 * @param dataSet
	 * @param weight
	 * @param learningRate
	 * @return
	 */
	private double[] train(List<double[]> dataSet, double[] weight, double learningRate) {
		List<double[]> features = getFeatures(dataSet);
		double[] targets = getTargets(dataSet);
		
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println("begin training");
		System.out.println(timeStamp);
		
		// go through every training examples
		for (int j = 0; j < dataSet.size(); j++) {
			double[] feature = features.get(j);
			double target = targets[j];
			
			double predictValue = DoubleOperation.time(feature, weight);
			double difference = target - predictValue;
			double[] margins = DoubleOperation.time(feature, difference);
			double[] regularizer = DoubleOperation.time(weight, mRegularizer);
			
			double[] marginDifference = DoubleOperation.minus(margins, regularizer);
			double[] learningMargin = DoubleOperation.time(marginDifference, learningRate);
			weight = DoubleOperation.add(weight, learningMargin);
			
			System.out.println(DoubleOperation.printArray(weight));
		}
		
		timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println("end training");
		System.out.println(timeStamp);
		
		// evaluate the model
		double sse = evaluate(features, targets, weight);
		SSE.add(sse);
		
		return weight;
	}
	
	/**
	 * evaluate the model according the current epoch's learned weight
	 * 
	 * @param features
	 * @param targets
	 * @param weight
	 */
	private double evaluate(List<double[]> features, double[] targets, double[] weight) {
		// go through every training examples
		double sum = 0.0;
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println("begin evaluation");
		System.out.println(timeStamp);
		for (int j = 0; j < features.size(); j++) {
			double[] feature = features.get(j);
			double target = targets[j];
			
			double predictValue = DoubleOperation.time(feature, weight);
			double difference = target - predictValue;
			
			double square = difference * difference;
			sum += square;
			
			System.out.println(sum);
		}
		
		System.out.println(" the SSE error for current iteration is " + (sum / 2));
		ResultOutput.writeTextFile(logFile, "" + (sum / 2));
		timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println("end evaluation");
		System.out.println(timeStamp);
		
		return sum;
	}
	
	
	/**
	 * This experiment is used for demonstrating that the stochastic gradient can be used 
	 * for learning the linear regression model.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LinearRegressionbyStochasticGradient lr = new LinearRegressionbyStochasticGradient("/nfs/guille/xfern/users/xie/Experiment/corpus/initial.csv", 
				1.0, true, 100);
		System.out.println("begin read the data");
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println(timeStamp);
		List<double[]> dataSet = lr.readMatrix();
		System.out.println("finish read the data");
		timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println(timeStamp);
		System.out.println("begin train the model");
		lr.trainLinearRegressionModel(dataSet);
		timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println(timeStamp);
		System.out.println("finish train the model");
		timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		System.out.println(timeStamp);
	}
	
}
