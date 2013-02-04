package edu.oregonstate.classifier;

/**
 * there are three fields for this class, including weights, totalWeights and violations
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public class Parameter {

	/* current weight */
	private double[] mWeight;
	
	/* the total weight */
	private double[] mTotalWeight;
	
	/* no of violations */
	private int mNoOfViolation;
	
	/* number of instance */
	private int mNumberofInstance;
	
	public Parameter() {
	}
	
	public Parameter(double[] weights) {
		this(weights, new double[weights.length]);
	}
	
	public Parameter(double[] weights, double[] totalWeights) {
		this(weights, totalWeights, 0, 0);
	}
	
	public Parameter(double[] weights, double[] totalWeights, int noOfViolations, int numberOfInstances) {
		mWeight = weights;
		mTotalWeight = totalWeights;
		mNoOfViolation = noOfViolations;
		mNumberofInstance = numberOfInstances;
	}
	
	public double[] getWeight() {
		return mWeight;
	}
	
	public double[] getTotalWeight() {
		return mTotalWeight;
	}
	
	public int getNoOfViolation() {
		return mNoOfViolation;
	}
	
	public int getNumberOfInstance() {
		return mNumberofInstance;
	}
	
	/**
	 * make a deep copy of the current object
	 * 
	 * @return
	 */
	public Parameter makeCopy() {
		int length = mWeight.length;
		double[] copyWeight = new double[length];
		double[] copyTotalWeight = new double[length];
		System.arraycopy(mWeight, 0, copyWeight, 0, length);
		System.arraycopy(mTotalWeight, 0, copyTotalWeight, 0, length);
		Parameter copyPara = new Parameter(copyWeight, copyTotalWeight, mNoOfViolation, mNumberofInstance);
		return copyPara;
	}
	
}
