package edu.oregonstate.features;

/**
 * Nominal Feature
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class NominalFeature extends Feature {

	@Override
	public boolean isNominal() {
		return true;
	}
	
	// the Nominal Features
	// For example, there is a weather nominal feature
	// the values of this feature can be hot, cold, or anything like that
	public abstract String[] getValues();
}
