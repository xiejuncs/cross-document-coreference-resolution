package edu.oregonstate.cluster.experiment;

import java.util.List;

import Jama.Matrix;
import edu.oregonstate.data.Document;

/**
 * Jun Xie(xiejuncs@gmail.com)
 */
public class EecbDissimilarityMeasure implements DissimilarityMeasure {

	/**
	 * how to calculate the dis similarity function
	 */
	public double computeDissimilarity(List<Document> vectors, int observation1, int observation2) {
		double similarity = 0.0;
		Matrix obs1 = vectors.get(observation1).vector;
		Matrix obs2 = vectors.get(observation2).vector;
		similarity = cosineSimilarity(obs1, obs2);
		return 1 - similarity;
	}
	
	/**
	 * use cosine similarity to compute dissimilarity
	 * 
	 * @param obs1
	 * @param obs2
	 * @return
	 */
	public double cosineSimilarity(Matrix obs1, Matrix obs2) {
		double sum = 0.0;
		for (int i = 0; i < obs1.getRowDimension(); i++) {
			sum = obs1.get(i, 0) * obs2.get(i, 0);
		}
		double norm1 = add(obs1);
		double norm2 = add(obs2);
		
		return sum / Math.sqrt(norm1 * norm2);
	}

	public double add(Matrix obs) {
		double sum = 0.0;
		for (int i = 0; i < obs.getRowDimension(); i++) {
			sum += obs.get(i, 0) * obs.get(i, 0);
		}
		return sum;
	}
}
