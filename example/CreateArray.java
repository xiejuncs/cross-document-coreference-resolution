package edu.oregonstate.example;

import java.text.DecimalFormat;

public class CreateArray {

	public static void main(String[] args) {
		double startNumerical = 1.0;
		int dimension = 10;
		double gap = startNumerical / dimension;
		double[] learningRates = new double[dimension];
		
		for (int i = 0; i < dimension; i++) {
			double learningRate = startNumerical - i * gap;
			DecimalFormat dtime = new DecimalFormat("#.##"); 
			learningRate= Double.valueOf(dtime.format(learningRate));
			learningRates[i] = learningRate;
			System.out.println(learningRate);
		}
		
	}
}
