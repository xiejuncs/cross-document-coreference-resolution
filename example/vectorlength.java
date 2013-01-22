package edu.oregonstate.example;

import java.text.DecimalFormat;

import edu.oregonstate.general.DoubleOperation;

public class vectorlength {

	public static void main(String[] args) {
		double[] learningRates = createDescendingArray(0.001, 0, 50);
		System.out.println("Learning Rates : " + DoubleOperation.printArray(learningRates));
	}
	
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
}
