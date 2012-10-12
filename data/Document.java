package edu.oregonstate.data;

import Jama.Matrix;

/**
 * Jun Xie(xiejuncs@gmail.com)
 */
public class Document {

	public int mID;
	public String mPrefix;
	public Matrix vector;

	public Document(int id, Matrix vec) {
		mID = id;
		vector = vec;
	}
	
	// set the prefix, the format as 1(cluster)-1(document),
	public void setPrefix(String prefix) {
		mPrefix = prefix;
	}

}
