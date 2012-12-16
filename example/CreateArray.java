package edu.oregonstate.example;

import java.text.DecimalFormat;

import edu.oregonstate.general.DoubleOperation;

public class CreateArray {

	public static void main(String[] args) {
		double[] array = DoubleOperation.createDescendingArray(1.0, 0.0, 10);
		
		System.out.println("done");
		
	}
}
