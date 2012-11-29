package edu.oregonstate.example;

import Jama.Matrix;;

public class MatrixOperation {

	public static void main(String[] args) {
		double[][] vals = {{1.},{4.},{7.}};
		Matrix A = new Matrix(vals);
		A = A.times(2);
		Matrix B = A.transpose();
		A= A.times(B);
		System.out.println("1");
	}
}
