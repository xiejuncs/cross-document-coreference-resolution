package edu.oregonstate.method;

import java.util.List;

import edu.oregonstate.classifier.Parameter;

/**
 * experiment framework
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface IMethod {

	/* according different method, execute different method */
	public List<Parameter> executeMethod();
}
