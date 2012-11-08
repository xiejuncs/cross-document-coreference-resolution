package edu.oregonstate.example;

import Jama.Matrix;

public class MatrixExample {

	public static void main(String[] args) {
		System.out.println(1);
		Matrix matrix = new Matrix(2, 1);
		matrix.set(0, 0, 1);
		matrix.set(1, 0, 2);
		System.out.println(matrix.norm2());
	}
}
