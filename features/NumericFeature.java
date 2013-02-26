package edu.oregonstate.features;

/**
 * Numeric Feature 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class NumericFeature extends Feature {

	@Override
	public boolean isNumeric() {
		return true;
	}

}
