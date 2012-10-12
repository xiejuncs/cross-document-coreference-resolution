package edu.oregonstate.general;

import Jama.Matrix;

/**
 * Jun Xie(xiejuncs@gmail.com)
 */
public class MatrixDisplay {

	/*
	for (int i = 0; i < vectors.size(); i++) {
			System.out.println("The " + i + "th document...");
			MatrixDisplay.display(vectors.get(i));
	}
	*/
	
	/**
	 * display the matrix in a pretty way
	 * @param matrix
	 */
	public static void display(Matrix matrix) {
		int row = matrix.getRowDimension();
		int column = matrix.getColumnDimension();
		StringBuilder sb = new StringBuilder();
		if (column == 1) {
			for (int i = 0; i < row; i++) {
				sb.append(matrix.get(i, column - 1) + " ");
			}
			System.out.println(sb.toString().trim());
		} else {
			for (int i = 0; i < row; i++) {
				for (int j = 0; j < column; j++) {
					sb.append(matrix.get(i, j) + " ");
				}
				sb.append("\n");
			}
			System.out.println(sb.toString().trim());
		}
	}
}
