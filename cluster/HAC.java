package edu.oregonstate.cluster;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;

import edu.oregonstate.data.EecbClusterDocument;
import edu.oregonstate.cluster.Cluster;
import edu.oregonstate.cluster.agglomeration.AgglomerationMethod;
import edu.oregonstate.cluster.experiment.DissimilarityMeasure;

/**
 * implementation of Hierarchical Agglomerative clustering method
 * <b>STEPS</b>
 * 1. starts with each obj in a separate cluster;
 * 2. repeatedly joins the closeset pair of clusters
 * 3. uniti there is only one cluster
 * 
 * Jun Xie(xiejuncs@gmail.com)
 */
public class HAC {

	public List<Cluster> mClusters;
	public List<EecbClusterDocument> mDocuments;
	// interface to incorporate different dissimilarityMeasure and different agglomerationMethod
	private DissimilarityMeasure dissimilarityMeasure;
	private List<Cluster> mergeResult; // all dendrogram clusters
	private AgglomerationMethod method;
	public List<String> mergeSequence;
	
	public HAC(List<EecbClusterDocument> documents, DissimilarityMeasure dissimilarityMeasure, 
			AgglomerationMethod agglomerationMethod) {
		mDocuments = documents;
		mClusters = new ArrayList<Cluster>();
		this.dissimilarityMeasure = dissimilarityMeasure;
		mergeResult = new ArrayList<Cluster>();
		method = agglomerationMethod;
		mergeSequence = new ArrayList<String>();
		initialize();
	}
	
	public List<String> getSequence() {
		return mergeSequence;
	}
	
	public List<Cluster> getMergeResult() {
		return mergeResult;
	}

	/**
	 * makeing the clustering
	 */
	public void cluster() {
		Map<String, Double> dissimilarityMatrix = computeDissimilarityMatrix();
		String minIndex = minimum(dissimilarityMatrix);
		// merge until there is only one cluster
		boolean flag = true;
		while(flag) {
			String[] indexs = minIndex.split("-");
			int to = Integer.parseInt(indexs[0]);
			int from = Integer.parseInt(indexs[1]);
			mergeSequence.add(mClusters.get(to).getID() + "-" + mClusters.get(from).getID());
			Cluster.mergeClusters(mClusters.get(to), mClusters.get(from));
			
			
			Cluster intermediateResult = new Cluster(to);
			intermediateResult.addChildrens(mClusters.get(to).getChildren());
			intermediateResult.addDocuments(mClusters.get(to).getDocuments());
			// also need to deep copy the cluster object not just an ArrayList of cluster object
			mClusters.remove(from);
			mergeResult.add(intermediateResult); // soft copy, need deep copy
			dissimilarityMatrix = new HashMap<String, Double>();
			dissimilarityMatrix = computeDissimilaritywithDiffernetMethod();
			if (dissimilarityMatrix.size() == 0) break;
			minIndex = minimum(dissimilarityMatrix);
		}
	}
	
	private Map<String, Double> computeDissimilaritywithDiffernetMethod() {
        Map<String, Double> dissimilarityMatrix = new HashMap<String, Double>();
        /** calcluate the dissimilarity score for each pair (i,j) s.t. i != j*/
        for (int i = 0; i < mClusters.size(); i++) {
        	for (int j = 0; j < i; j++) {
        		double dissimilarity = method.computeDissimilarity(mClusters.get(i), mClusters.get(j));
        		dissimilarityMatrix.put(Integer.toString(i) + "-" + Integer.toString(j), dissimilarity);
        	}
        }
        
		return dissimilarityMatrix;
	}
	
	
	/*Compare HashMap to get the index with the minimum value*/
    public String minimum(Map<String, Double> scores) {
            Collection<Double> c = scores.values();
            Double minvalue = Collections.min(c);
            String minIndex = "";
            for (String key : scores.keySet()) {
            	Double value = scores.get(key);
            	if (value == minvalue) {
                	minIndex = key;
                	break;
                }
            }

            return minIndex;
    }
	
	
	private Map<String, Double> computeDissimilarityMatrix() {
        Map<String, Double> dissimilarityMatrix = new HashMap<String, Double>();
        /** calcluate the dissimilarity score for each pair (i,j) s.t. i != j*/
        for (int i = 0; i < mDocuments.size(); i++) {
        	for (int j = 0; j < i; j++) {
        		double dissimilarity = dissimilarityMeasure.computeDissimilarity(mDocuments, i, j);
        		dissimilarityMatrix.put(Integer.toString(i) + "-" + Integer.toString(j), dissimilarity);
        	}
        }
        
		return dissimilarityMatrix;
	}
	
	/**
	 * initialize each document in a separate cluster
	 */
	private void initialize() {
		for (int i = 0; i < mDocuments.size(); i++) {
			Cluster cluster = new Cluster(i);
			cluster.addDocument(mDocuments.get(i));
			mClusters.add(cluster);
		}
	}
}
