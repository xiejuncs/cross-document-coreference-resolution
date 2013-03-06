package edu.oregonstate.method;

/**
 * used the learned weight to do decoding
 * 
 * Take Coreference Resolution as an example, the decoding part is 
 * to find a coreference resolution chain using search algorithm
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class Decoding {
	
	// decoding phase, used for define the output file name and debug information
	// for example: training-1
	protected String decodingPhase;
	
	public Decoding(String phase) {
		decodingPhase = phase;
	}
	
	/**
	 * decode according to different application
	 * 
	 * @param weight
	 */
	public abstract void decode(double[] weight);
}
