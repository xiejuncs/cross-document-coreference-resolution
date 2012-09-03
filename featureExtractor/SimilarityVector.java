package edu.oregonstate.featureExtractor;

import java.util.HashMap;
import edu.oregonstate.util.CosineSimilarity;
import edu.stanford.nlp.stats.Counter;

/**
 * calculate similarity score for two similarity vector.
 * call the function in CosineSimilarity
 * <b>NOTE</b>
 * in order to use Cosine Similarity, convert from Counter data structure to Hash Map first 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SimilarityVector {

	private Counter<String> mcounter;
	
	public SimilarityVector(Counter<String> counter) {
		mcounter = counter;
	}
	
	public Counter<String> getCounter() {
		return mcounter;
	}
	
	/**
	 * calculate the cosine similarity of two similarity vector 
	 * 
	 * @param c1
	 * @param c2
	 * @return cosine similarity
	 */
	public static double getCosineSimilarity(SimilarityVector c1, SimilarityVector c2) {
		if (c1.mcounter.size() == 0 || c2.mcounter.size() == 0) return 0;
		Counter<String> counter1 = c1.mcounter;
		Counter<String> counter2 = c2.mcounter;
		HashMap<String, Double> hcounter1 = convertCounter(counter1);
		HashMap<String, Double> hcounter2 = convertCounter(counter2);
		double score = CosineSimilarity.calculateCosineSimilarity(hcounter1, hcounter2);
		return score;
	}
	
	/**
	 * convert from counter data structure to hash map data structure
	 * and then call the CosineSimilarity defined in the util package
	 * 
	 * @param counter
	 * @return
	 */
	public static HashMap<String, Double> convertCounter(Counter<String> counter) {
		HashMap<String, Double> hcounter = new HashMap<String, Double>();
		for (String key : counter.keySet()) {
			hcounter.put(key, counter.getCount(key));
		}
		return hcounter;
	}
	
}
