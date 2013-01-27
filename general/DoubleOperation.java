package edu.oregonstate.general;

import java.text.DecimalFormat;
import java.util.List;

import edu.oregonstate.io.ResultOutput;

public class DoubleOperation {

	/**
	 * add two double array
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static double[] add(double[] matrix1, double[] matrix2) {
		double[] matrix = new double[matrix1.length];
		assert matrix1.length == matrix2.length;
		for (int i = 0; i < matrix1.length; i++) {
			matrix[i] = matrix1[i] + matrix2[i];
		}
		
		return matrix;
	}
	
	/**
	 * normalize a double array
	 * 
	 * @param matrix
	 * @return
	 */
	public static double[] normalize(double[] matrix) {
		double sum = 0.0;
		for (double value : matrix) {
			sum += value * value;
		}
		
		if (sum == 0.0) return matrix;
		
		sum = Math.sqrt(sum);
		
		for(int i = 0; i < matrix.length; i++) {
			matrix[i] = matrix[i] / sum;
		}
		
		return matrix;
	}
	
	/**
	 * one double array times the other array
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static double time(double[] matrix1, double[] matrix2) {
		assert matrix1.length == matrix2.length;
		double sum = 0;
		for (int i = 0; i < matrix1.length; i++) {
			sum += matrix1[i] * matrix2[i];
		}
		
		return sum;
	}
	
	/**
	 * one double array times a contant value
	 * 
	 * @param matrix
	 * @param constant
	 * @return
	 */
	public static double[] time(double[] matrix, double constant) {
		for (int i = 0; i < matrix.length; i++) {
			matrix[i] = matrix[i] * constant;
		}
		return matrix;
	}
	
	/**
	 * one double array minus the other array
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static double[] minus(double[] matrix1, double[] matrix2) {
		
		assert matrix1.length == matrix2.length;
		double[] matrix = new double[matrix1.length];
		for (int i = 0; i < matrix1.length; i++) {
			matrix[i] = matrix1[i] - matrix2[i];
		}
		
		return matrix;
	}
	
	/**
	 * one string minus the other string 
	 * 
	 * @param matrix1
	 * @param matrix2
	 * @return
	 */
	public static double[] minus(String matrix1, String matrix2) {
		String[] features1 = matrix1.split(",");
		String[] features2 = matrix2.split(",");
		assert features1.length == features2.length;
		double[] result = new double[features1.length];
		for (int i = 0; i < features1.length; i++) {
			result[i] = Double.parseDouble(features1[i]) - Double.parseDouble(features2[i]);
		}
		
		return result;
	}
	
	/**
	 * one double array divide a constant
	 * 
	 * @param matrix
	 * @param divider
	 * @return
	 */
	public static double[] divide(double[] matrix, Object divider) {
		double denominator = Double.valueOf(divider.toString());
		for (int i = 0; i < matrix.length; i++) {
			matrix[i] = matrix[i] / denominator;
		}
		return matrix;
	}
	
	
	
	/**
	 * print double array
	 * 
	 * @param matrix
	 */
	public static String printArray(double[] matrix) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < matrix.length; i++){
			double value = matrix[i];
			
			if (i != matrix.length - 1) {
				sb.append(value + ", ");
			} else {
				sb.append(value);
			}
		}
		
		return sb.toString().trim();
	}
	
	public static double transformNaN(double value) {
		double result = value;
		if (Double.isNaN(value)) {
			result = 0.0;
		}
		return result;
	}
	
	/**
	 * create a descending double array for two specific parameters, starting numerical and dimension, 
	 * for example, the starting numerical is 1.0 and dimension is 10
	 * 
	 * the result is [1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1]
	 * 
	 * @param startNumerical
	 * @param dimension
	 * @return
	 */
	public static double[] createDescendingArray(double startNumerical, double endNumerical, int dimension) {
		double gap = (startNumerical - endNumerical) / dimension;
		double[] learningRates = new double[dimension];
		
		for (int i = 0; i < dimension; i++) {
			double learningRate = startNumerical - i * gap;
			DecimalFormat dtime = new DecimalFormat("#.############"); 
			learningRate= Double.valueOf(dtime.format(learningRate));
			learningRates[i] = learningRate;
		}
		
		return learningRates;
	}
	
	/**
	 * calculate weights
	 * 
	 * @param weights
	 */
	public static void calcualateWeightDifference(List<double[]> weights, String weightFile) {		
		for (int i = 0; i < weights.size() - 1; i++) {
			double[] differences = DoubleOperation.minus(weights.get(i), weights.get(i+1));
			double sum = 0.0;
			for (double difference : differences) {
				sum += difference * difference;
			}
			double length = Math.sqrt(sum);
			ResultOutput.writeTextFile(weightFile, length + "");
		}
	}
	
	/**
	 * calculate the norm of the weight
	 * 
	 * @param weights
	 * @param weightFile
	 */
	public static void printWeightNorm(List<double[]> weights, String weightFile) {
		for (int i = 0; i < weights.size(); i++) {
			double[] weight = weights.get(i);
			double length = calculateTwoNorm(weight);
			ResultOutput.writeTextFile(weightFile, length + "");
		}
	}
	
	/**
	 * calculate two norm of a vector
	 * 
	 * @param vector
	 * @return
	 */
	public static double calculateTwoNorm(double[] vector) {
		double sum = 0.0; 
		for (double element : vector) {
			sum += element * element;
		}
		double length = Math.sqrt(sum);
		return length;
	}
	
	/**
	 * transform string arrays to double arrays
	 * 
	 * @param stringArrays
	 * @return
	 */
	public static double[] transformStringArray(String[] stringArrays) {
		double[] doubleArrays = new double[stringArrays.length];
		for (int i = 0; i < stringArrays.length; i++) {
			doubleArrays[i] = Double.parseDouble(stringArrays[i]);
		}
		
		return doubleArrays;
	}
	
	/**
	 * transform string to double arrays
	 * 
	 * @param string
	 * @param separator
	 * @return
	 */
	public static double[] transformString(String string, String separator) {
		String[] stringArrays = string.split(separator);
		double[] doubleArrays = transformStringArray(stringArrays);
		return doubleArrays;
	}
}
