package edu.oregonstate.classifier;

import java.util.List;

/**
 * interface of classifier
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public interface IClassifier {
	
	/* train the model according to file path and parameters */
	public Parameter train(String path, Parameter para);
	
	/* train the model according to file paths and parameters */
	public Parameter train(List<String> path, Parameter para);
	
	/* use zero vector to train the model */
	public Parameter train(List<String> path, int modelIndex);
	
}
