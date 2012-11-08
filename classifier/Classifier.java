package edu.oregonstate.classifier;

import Jama.Matrix;

/**
 * The superclass of classifier
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface Classifier {

	public void train(Class<?> trainFile, Matrix initialModel);
	
	public void test(Class<?> testFile, Matrix model);
}
