package edu.oregonstate.score;

public class AssignmentProblem {

	private final double[][] costMatrix;
	
	public AssignmentProblem(double[][] aCostMatrix) {
		costMatrix = aCostMatrix;
	}
	
	private double[][] copyOfMatrix() {
		double[][] retval = new double[costMatrix.length][];
		for (int i = 0; i < costMatrix.length; i++) {
			retval[i] = new double[costMatrix[i].length];
			System.arraycopy(costMatrix[i], 0, retval[i], 0, costMatrix[i].length);
		}
		return retval;
	}
	
	public int[][] solve(AssignmentAlgorithm algorithm) {
		double[][] costMatrix = copyOfMatrix();
		return algorithm.computeAssignments(costMatrix);
	}
}
