package edu.oregonstate.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import Jama.Matrix;

import edu.oregonstate.cluster.experiment.DissimilarityMeasure;
import edu.oregonstate.cluster.experiment.EecbDissimilarityMeasure;
import edu.oregonstate.data.EecbClusterDocument;

/**
 * First, we use a hierarchical algorithm to generate the collection dendrogram. From the dendrogram 
 * clusters, the next component generates the candidates for the EM initial model as follows:
 * 
 * 1. All dendrogram clusters are sorted in descending order of their quality. Inituitvely, we define the cluster
 * quality as the likeliness that the cluster contains all and only documents from one domain.
 * 2. The top clusters that provide a converge of less or equal than rn documents, where n indicates the number 
 * documents in the collection, are selected
 * 3. The clusters are selected in the previous step are post-processed to remove the clusters that are already
 * included in other higher-randked clusters.
 * 
 * Jun Xie(xiejuncs@gmail.com)
 */
public class EvaluateModelCandidates {

	private List<Cluster> originalModel;
	public enum QUALITYMSURE {W, WB, WN, GW, GWB, GWN};
	private DissimilarityMeasure measure;
	private int termSize;
	
	public EvaluateModelCandidates (List<Cluster> model, int termSize) {
		originalModel = model;
		measure = new EecbDissimilarityMeasure();
		this.termSize = termSize;
	}
	
	/**
	 * according to the speficiation of the paper, evaluate each model created by HAC
	 * 
	 * @return return the model, which specifies the initial cluster number and the cluster result 
	 */
	/*
	public List<Cluster> evaluate() {
		double bestScore = 0.0;
		List<Cluster> bestModel = new ArrayList<Cluster>();
		for (QUALITYMSURE type : QUALITYMSURE.values()) {
			List<Cluster> model = new ArrayList<Cluster>();
			switch(type) {
				case W : 
							System.out.println("W....");
							W w = new W(originalModel);
							w.sort();
							model = w.getModel();
							break;
				case WB:
							System.out.println("WB....");
							WB wb = new WB(originalModel);
							wb.sort();
							model = wb.getModel();
							break;
				case WN:
							System.out.println("WN...");
							WN wn = new WN(originalModel);
							wn.sort();
							model = wn.getModel();
							break;
				case GW:
							System.out.println("GW....");
						 	GW gw = new GW(originalModel);
						 	gw.sort();
						 	model = gw.getModel();
						 	break;
				case GWB:
							System.out.println("GWB....");
							GWB gwb = new GWB(originalModel);
							gwb.sort();
							model = gwb.getModel();
							break;
				case GWN: 
							System.out.println("GWN....");
							GWN gwn = new GWN(originalModel);
							gwn.sort();
							model = gwn.getModel();
							break;
				default:
							System.out.println("non-valid quality measure");
							break;
			}
			
			double currentScore = 0.0;
			
			List<Double> results = findLocalMaximum(model);
			currentScore = results.get(0);
			if (currentScore > bestScore) {
				// best model part
				bestModel = model.subList(0, (results.get(1)).intValue());
				bestScore = currentScore;
			}
		}
		
		return bestModel;
	
	}
	*/
	
	/**
	 * currentScore = first local maximum of C as r decrease from 100% to 0%
	 * 
	 * every time we just decrease r as one cluster from the model
	 * 
	 * @param model
	 * @return
	 */
	private List<Double> findLocalMaximum(List<Cluster> model) {
		int size = model.size();
		double localMaximum = 0.0;
		int k = 0;
		List<Double> result = new ArrayList<Double>();
		for (int i = (size - 1); i > 0; i--) {
			List<Cluster> topClusters = model.subList(0, i);
			double cscore = calculateCScore(topClusters);
			if (cscore < localMaximum) {
				k = i - 1;
				break;
			}
			localMaximum = cscore;
		}
		result.add(localMaximum);
		result.add(Double.parseDouble(Integer.toString(k)));
		
		return result;
	}
	
	/**
	 * C = B(n - 1) / W(k - 1)
	 * 
	 * @param subModel
	 * @return
	 */
	private double calculateCScore(List<Cluster> subModel) {
		Matrix metaCentroid = new Matrix(termSize, 1);
		int n = 0;
		for (Cluster cluster : subModel) {
			List<EecbClusterDocument> documents = cluster.getDocuments();
			for(EecbClusterDocument document : documents) {
				metaCentroid = metaCentroid.plus(document.vector);
			}
			n = n + documents.size();
		}
		int k = subModel.size();
		double W = calculateWScore(subModel);
		double B = calculateBScore(subModel, metaCentroid);
		return (B * (n - k)) / (W * (k - 1));
	}
	
	private double calculateWScore(List<Cluster> subModel) {
		double W = 0.0;
		for (Cluster cluster : subModel) {
			Matrix centroid = new Matrix(termSize, 1);
			List<EecbClusterDocument> documents = cluster.getDocuments();
			for (EecbClusterDocument document : documents) {
				centroid = centroid.plus(document.vector);
			}
			for (EecbClusterDocument document : documents) {
				W += Math.pow(1 - measure.cosineSimilarity(centroid, document.vector), 2);
			}
		}
		return W;
	}
	
	private double calculateBScore(List<Cluster> subModel, Matrix metaCentroid) {
		double B = 0.0;
		for (Cluster cluster : subModel) {
			Matrix centroid = new Matrix(termSize, 1);
			List<EecbClusterDocument> documents = cluster.getDocuments();
			for (EecbClusterDocument document : documents) {
				centroid = centroid.plus(document.vector);
			}
			B += documents.size() * Math.pow(1 - measure.cosineSimilarity(centroid, metaCentroid), 2);
		}
		
		return B;
	}
	
	
}
