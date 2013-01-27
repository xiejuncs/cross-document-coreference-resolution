package edu.oregonstate.example;

import edu.oregonstate.general.DoubleOperation;

public class MatrixOperation {

	public static void main(String[] args) {
		double[] rates = DoubleOperation.createDescendingArray(0.1, 0, 20);
		for (double rate : rates) {
			System.out.println(rate);
		}
	}
}
