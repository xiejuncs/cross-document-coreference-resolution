package edu.oregonstate.cluster.agglomeration;

import edu.oregonstate.cluster.Cluster;

/**
 * 
 * 
 * @author Jun Xie(xiejuncs@gmail.com)
 *
 */

public interface AgglomerationMethod {

	/**
	 * Compute the dissimilarity between two clusters
	 * 
	 * @return dissimilarity between cluster (i,j).
	 */
	public double computeDissimilarity(Cluster c1, Cluster c2);
}
