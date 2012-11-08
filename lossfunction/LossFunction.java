package edu.oregonstate.lossfunction;

/**
 * the interface of Loss Functions
 * 
 * There are a lot of loss functions, for example, highe loss, 0-1 loss, hamming loss.
 * Through this class, given different object, the loss function can be calculated.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface LossFunction {

	public double calculateLossFunction(Object obj);
}
