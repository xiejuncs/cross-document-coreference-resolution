package edu.oregonstate.example;

public class NumericComparison {

	public static void main(String[] args) {
		double globalScore = 0.0;
		double localScore = 0.0/0.0;
		
		System.out.println(localScore);
		System.out.println(globalScore);
		System.out.println(globalScore <= localScore);
	}
}
