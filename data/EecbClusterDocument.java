package edu.oregonstate.data;

import Jama.Matrix;

/**
 * Jun Xie(xiejuncs@gmail.com)
 */
public class EecbClusterDocument {

	public int mID;
	public String mPrefix;
	public Matrix vector;

	public EecbClusterDocument(int id, Matrix vec) {
		mID = id;
		vector = vec;
	}
	
	// set the prefix, the format as 1(cluster)-1(document),
	public void setPrefix(String prefix) {
		mPrefix = prefix;
	}

}
