package edu.oregonstate.cluster.agglomeration;

import edu.oregonstate.cluster.Cluster;
import edu.oregonstate.cluster.experiment.DissimilarityMeasure;
import edu.oregonstate.cluster.experiment.EecbDissimilarityMeasure;
import Jama.Matrix;
import edu.oregonstate.data.Document;
/**
 * average link or group average
 * <b>Formula</b>
 * 
 * dist(c_{i}, c_{j}) = 1/n_{i}n{j} \sum_{d_{r} \in c_{i}} \sum_{d_{s} \in c_{j}} dist(d_{r}, d_{s})
 * 
 * @author Jun Xie(xiejuncs@gmail.com)
 *
 */
public class AverageLinkage implements AgglomerationMethod {

	/** calculate the dissimilarity between two clusters */
	public double computeDissimilarity(Cluster c1, Cluster c2) {
		DissimilarityMeasure meausre = new EecbDissimilarityMeasure();
		double dissimilarity = 0.0;
		int n1 = c1.getDocuments().size();
		int n2 = c2.getDocuments().size();
		
		for (Document d1 : c1.getDocuments()) {
			for (Document d2 : c2.getDocuments()) {
				Matrix m1 = d1.vector;
				Matrix m2 = d2.vector;
				dissimilarity += 1 - meausre.cosineSimilarity(m1, m2);
			}
		}
		
		return dissimilarity / (n1 * n2);
	}

    public String toString() {
        return "Average";
    }

}