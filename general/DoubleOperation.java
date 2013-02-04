package edu.oregonstate.general;

import java.text.DecimalFormat;
import java.util.List;

import edu.oregonstate.io.ResultOutput;

public class DoubleOperation {

	/**
	 * add two double array
	 * 
	 * @param vector1
	 * @param vector2
	 * @return
	 */
	public static double[] add(double[] vector1, double[] vector2) {
		double[] result = new double[vector1.length];
		assert vector1.length == vector2.length;
		for (int i = 0; i < vector1.length; i++) {
			result[i] = vector1[i] + vector2[i];
		}
		
		return result;
	}
	
	/**
	 * normalize a double array
	 * 
	 * @param vector
	 * @return
	 */
	public static double[] normalize(double[] vector) {
		int length = vector.length;
		double[] normalizedVector = new double[length];
		
		// get the sum
		double sum = 0.0;
		for (double value : vector) {
			sum += value * value;
		}
		if (sum == 0.0) {
			return normalizedVector;
		}
		
		// divide by the length of the vector
		double vectorLength = Math.sqrt(sum);
		for(int i = 0; i < length; i++) {
			normalizedVector[i] = vector[i] / vectorLength;
		}
		
		return normalizedVector;
	}
	
	/**
	 * one double array times the other array
	 * 
	 * @param vector1
	 * @param vector2
	 * @return
	 */
	public static double time(double[] vector1, double[] vector2) {
		assert vector1.length == vector2.length;
		double sum = 0;
		for (int i = 0; i < vector1.length; i++) {
			sum += vector1[i] * vector2[i];
		}
		
		return sum;
	}
	
	/**
	 * one double array times a contant value
	 * 
	 * @param vector
	 * @param constant
	 * @return
	 */
	public static double[] time(double[] vector, double constant) {
		int length = vector.length;
		double[] result = new double[length];
		for (int i = 0; i < length; i++) {
			result[i] = vector[i] * constant;
		}
		return result;
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
	 * one double array divide a constant
	 * 
	 * @param vector
	 * @param divider
	 * @return
	 */
	public static double[] divide(double[] vector, Object divider) {
		int length = vector.length;
		double[] result = new double[length];
		
		double denominator = Double.valueOf(divider.toString());
		for (int i = 0; i < length; i++) {
			result[i] = vector[i] / denominator;
		}
		
		return result;
	}
	
	
	
	/**
	 * print double array
	 * 
	 * @param vector
	 */
	public static String printArray(double[] vector) {
		StringBuilder sb = new StringBuilder();
		
		for (int i = 0; i < vector.length; i++){
			double value = vector[i];
			
			if (i != vector.length - 1) {
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
	
	/**
	 * is vector a zero vector
	 * @param vector
	 * @return
	 */
	public static boolean isAllZero(double[] vector) {
		boolean allzero = true;
		for (double element : vector) {
			if (element != 0.0) {
				allzero = false;
				break;
			}
		}
		return allzero;
	}
	
}
