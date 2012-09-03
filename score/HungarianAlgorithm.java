package edu.oregonstate.score;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * an implementation of the classic hungarian algorithm for the assignment problem
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class HungarianAlgorithm implements AssignmentAlgorithm{

	public int[][] computeAssignments(double[][] matrix) {
		reduceMatrix(matrix);
		
		int[] starsByRow = new int[matrix.length];
		Arrays.fill(starsByRow, -1);
		int[] startsByCol = new int[matrix[0].length];
		Arrays.fill(startsByCol, -1);
		int[] primesByRow = new int[matrix.length];
		Arrays.fill(primesByRow, -1);
		
		int[] coverdRows = new int[matrix.length];
		int[] coverdCols = new int[matrix[0].length];
		
		initStars(matrix, starsByRow, startsByCol);
		coverColumnsOfStarredZeros(startsByCol, coverdCols);
		
		while(!allAreCovered(coverdCols) && !allAreCovered(coverdRows)) {
			int[] primedZero = primeSomeUncoveredZero(matrix, primesByRow, coverdRows, coverdCols);
			while (primedZero == null) {
				makeMoreZeroes(matrix, coverdRows, coverdCols);
				primedZero = primeSomeUncoveredZero(matrix, primesByRow, coverdRows, coverdCols);
			}
			
			int columnIndex = starsByRow[primedZero[0]];
			if (-1 == columnIndex) {
				incrementSetOfStarredZeroes(primedZero, starsByRow, startsByCol, primesByRow);
			    Arrays.fill(primesByRow, -1);
			    Arrays.fill(coverdRows, 0);
			    Arrays.fill(coverdCols, 0);
			    coverColumnsOfStarredZeros(startsByCol, coverdCols);
			} else {
				coverdRows[primedZero[0]] = 1;
				coverdCols[columnIndex] = 0;
			}
		}
		
		int[][] retval = new int[matrix[0].length][];
		for (int i = 0; i < startsByCol.length; i++) {
		    retval[i] = new int[] { startsByCol[i], i };
		}
		return retval;
	}
	
	private void incrementSetOfStarredZeroes(int[] unpairedZeroPrime, int[] starsByRow, int[] starsByCol, int[] primesByRow) {

	  // build the alternating zero sequence (prime, star, prime, star, etc)
	  int i, j = unpairedZeroPrime[1];

	  Set<int[]> zeroSequence = new LinkedHashSet<int[]>();
	  zeroSequence.add(unpairedZeroPrime);
	  boolean paired = false;
	  do {
	    i = starsByCol[j];
	    paired = -1 != i && zeroSequence.add(new int[] { i, j });
	    if (!paired) {
	      break;
	    }

	    j = primesByRow[i];
	    paired = -1 != j && zeroSequence.add(new int[] { i, j });

	  } while (paired);

	  // unstar each starred zero of the sequence
	  // and star each primed zero of the sequence
	  for (int[] zero : zeroSequence) {
	    if (starsByCol[zero[1]] == zero[0]) {
	      starsByCol[zero[1]] = -1;
	      starsByRow[zero[0]] = -1;
	    }
	    if (primesByRow[zero[0]] == zero[1]) {
	      starsByRow[zero[0]] = zero[1];
	      starsByCol[zero[1]] = zero[0];
	    }
	  }

	}
	
	private void makeMoreZeroes(double[][] matrix, int[] coveredRows, int[] coveredCols) {
		double minUncoveredValue = Double.MAX_VALUE;
		for (int i = 0; i < matrix.length; i++) {
			if (0 == coveredRows[i]) {
				for (int j = 0; j < matrix[i].length; j++) {
					if (0 == coveredCols[j] && matrix[i][j] < minUncoveredValue) {
						minUncoveredValue = matrix[i][j];
					}
				}
			}
		}
		
		for (int i = 0; i < coveredRows.length; i++) {
			if (1 == coveredRows[i]) {
				for (int j = 0; j < matrix[i].length; j++) {
					matrix[i][j] += minUncoveredValue;
				}
			}
		}
		
		for (int i = 0; i < coveredCols.length; i++) {
			if (0 == coveredCols[i]) {
				for (int j = 0; j < matrix.length; j++) {
					matrix[j][i] -= minUncoveredValue;
				}
			}
		}
	}
	
	private int[] primeSomeUncoveredZero(double[][] matrix, int[] primesByRow, int[] coveredRows, int[] coveredCols) {
		for (int i = 0; i < matrix.length; i++) {
			if (1 == coveredRows[i]) {
				continue;
			}
			
			for (int j = 0; j < matrix[i].length; j++) {
				if (0 == matrix[i][j] && 0 == coveredCols[j]) {
					primesByRow[i] = j;
					return new int[] {i, j};
				}
			}
		}
		
		return null;
	}
	
	private void coverColumnsOfStarredZeros(int[] starsByCol, int[] coveredCols) {
		for (int i = 0; i < starsByCol.length; i++) {
			coveredCols[i] = -1 == starsByCol[i] ? 0 : 1;
		}
	}
	
	private boolean allAreCovered(int[] coveredCols) {
		for (int covered : coveredCols) {
			if (0 == covered) return false;
		}
		return true;
	}
	
	/**
	 * init starred zeroes
	 * for each column find the first zero if there is no other starred zero in that row then star the zero,
	 * cover the column and row and go onto the next column
	 * 
	 * @param costMatrix
	 * @param starsByRow
	 * @param starsByCol
	 */
	private void initStars(double[][] costMatrix, int[] starsByRow, int[] starsByCol) {
		int[] rowHasStarredZero = new int[costMatrix.length];
		int[] colHasStarredZero = new int[costMatrix[0].length];
		
		for (int i = 0; i < costMatrix.length; i++) {
			for (int j = 0; j < costMatrix[i].length; j++) {
				if (0 == costMatrix[i][j] && 0 == rowHasStarredZero[i] && 0 == colHasStarredZero[j]) {
					starsByRow[i] = j;
					starsByCol[j] = i;
					rowHasStarredZero[i] = 1;
					colHasStarredZero[j] = 1;
					break; // move onto the next row
				}
			}
		}
	}
	
	/**
	 * the first step of the hungarian algorithm is to find the smallest element in each row and subtract it's
	 * values from all elements from that row
	 * 
	 * @param matrix
	 */
	private void reduceMatrix(double[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			double minValInRow = Double.MAX_VALUE;
			for (int j = 0; j < matrix[i].length; j++) {
				if (minValInRow > matrix[i][j]) {
					minValInRow = matrix[i][j];
				}
			}
			
			for (int j = 0; j < matrix[i].length; j++) {
				matrix[i][j] -= minValInRow;
			}
		}
		
		for (int i = 0; i < matrix[0].length; i++) {
			double minValInCol = Double.MAX_VALUE;
			for (double[] element : matrix) {
				if (minValInCol > element[i]) {
					minValInCol = element[i];
				}
			}
			
			for (int j = 0; j < matrix.length; j++) {
				matrix[j][i] -= minValInCol;
			}
		}
		
	}
	
} 
