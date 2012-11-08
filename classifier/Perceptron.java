package edu.oregonstate.classifier;

import edu.oregonstate.general.MatrixOperation;
import edu.oregonstate.io.ResultOutput;

import Jama.Matrix;

/*Update the weight*/
public class Perceptron {
	
	private String trainingFile;
	
	private double learningRate;
	
	private double mIteration;
	
	private boolean maveragePerceptron;
	
	private double meplison;
	
	// use telescoping constructor pattern to construct two constructors
	public Perceptron(String filePath, double rate, double iteration, boolean averagePerceptron, double eplison) {
		trainingFile = filePath;
		learningRate = rate;
		mIteration = iteration;
		maveragePerceptron = averagePerceptron;
		meplison = eplison;
	}
	
	public Perceptron(String filePath, double rate, double iteration, boolean averagePerceptron) {
		this(filePath, rate, iteration, averagePerceptron, 1e-6);
	}
	
	// we always use this one, but maybe it will change later
	public Perceptron(String filePath, double rate, double iteration) {
		this(filePath, rate, iteration, true);
	}
	
	public Perceptron(String filePath, double rate) {
		this(filePath, rate, 0);
	}
	
	public Perceptron(String filePath) {
		this(filePath, 0.01);
	}
	 
	/** Implementation of batch Perceptron*/
	public Matrix learn_perceptron(Matrix trainingData, Matrix trainingTargets) {
		int column = trainingData.getColumnDimension(); // feature dimension size
		int row = trainingData.getRowDimension();
		Matrix initial_weight = new Matrix(column, 1);
		Matrix average_weight = new Matrix(column, 1);
		
		int i = 0;
		Matrix delta = new Matrix(column, 1);
		do{
			
			delta = new Matrix(column, 1);
			for (int j = 0; j < row; j++) {
				Matrix training_example = trainingData.getMatrix(j, j, 0, column - 1);
				Double label = trainingTargets.get(j, 0);  // get label
				double value = training_example.times(initial_weight).get(0, 0);
				if (value * label <= 0) {
					delta = delta.minus(training_example.transpose().times(label));
				}
			}
			delta = delta.times(1.0/row);
			initial_weight = initial_weight.minus(delta.times(learningRate));
			average_weight = average_weight.plus(initial_weight);
			i += 1;
			System.out.println(i);
			System.out.println(ResultOutput.printModel(initial_weight));
			
		} while (delta.norm2() > meplison);

		average_weight = average_weight.times(1.0/(row * i));
		System.out.println(i);
		if (maveragePerceptron) {
			return average_weight;
		} else {
			return initial_weight;
		}
		
	}
	
	public Matrix calculateMatrix() {
		Matrix training = MatrixOperation.readMatrix(trainingFile);
		/** get the actual features, meanwhile add a N*1 column vector with value being all 1 as the first column of the features */
		Matrix trainingData = getDataPoints(training);
		Matrix trainingTargets = getTargets(training);
		
		Matrix learnedWeight = learn_perceptron(trainingData, trainingTargets);
		return learnedWeight;
	}
	
	/**
	 * the last column correspond to the target
	 * Hence, remove target values from the last column of a data set.
	 * <p>
	 * Meanwhile, we need to add 1 to 0 column of each row as a bias term 
	 * 
	 * @param data_set
	 * @return
	 */
	private Matrix getDataPoints(Matrix data_set) {
		Matrix features = data_set.getMatrix(0, data_set.getRowDimension() - 1, 1, data_set.getColumnDimension() - 1);
		int rows = features.getRowDimension();
		int cols = features.getColumnDimension() + 1;
		Matrix modifiedFeatures = new Matrix(rows, cols);
		for (int r = 0; r < rows; ++r) {
			for (int c = 0; c < cols; ++c) {
				if (c == 0) {
					modifiedFeatures.set(r, c, 1.0);
				} else {
					modifiedFeatures.set(r, c, features.get(r, c-1));
				}
			}
		}
		return modifiedFeatures;
	}
	
	/**
	 * Returns the target values from the last column of a data set.
	 * 
	 * @param data_set
	 * @return
	 */
	private Matrix getTargets(Matrix data_set) {
	    return data_set.getMatrix(0, data_set.getRowDimension() - 1, 0, 0);
	}
	
	/**
 	 * Execute the perceptron algorithm
 	 *
 	 */
	public static void main(String[] args) {
		/*Import the two Gaussian File*/
		Perceptron perceptron = new Perceptron("../corpus/twogaussian.csv", 1, 0, false);
		Matrix weight = perceptron.calculateMatrix();
		System.out.println(ResultOutput.printModel(weight));
	}
}
