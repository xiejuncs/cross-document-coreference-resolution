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
	
	/* variance */
	private double[][] mVariance;
	
	public Parameter(double[] weights, double[][] variance) {
		this(weights, variance, new double[weights.length]);
	}
	
	public Parameter(double[] weights, double[][] variance, double[] totalWeights) {
		this(weights, variance, totalWeights, 0, 0);
	}
	
	public Parameter(double[] weights, double[][] variance, double[] totalWeights, int noOfViolations, int numberOfInstances) {
		mWeight = weights;
		mTotalWeight = totalWeights;
		mNoOfViolation = noOfViolations;
		mNumberofInstance = numberOfInstances;
		mVariance = variance;
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
	
	public double[][] getVariance() {
		return mVariance;
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
		double[][] copyVariance = new double[length][length];
		System.arraycopy(mWeight, 0, copyWeight, 0, length);
		System.arraycopy(mTotalWeight, 0, copyTotalWeight, 0, length);
		
		for (int index = 0; index < length; index++) {
			System.arraycopy(mVariance[index], 0, copyVariance[index], 0, length);
		}
		
		Parameter copyPara = new Parameter(copyWeight, copyVariance, copyTotalWeight, mNoOfViolation, mNumberofInstance);
		return copyPara;
	}
	
}
