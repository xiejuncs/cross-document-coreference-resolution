package edu.oregonstate.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import edu.oregonstate.data.Document;
import java.util.Set;
import Jama.Matrix;


/**
 * Implementation of EM algorithm
 * 
 * The EM tutorial is shown http://www.seanborman.com/publications/EM_algorithm.pdf
 * 
 * Jun Xie(xiejuncs@gmail.com)
 */
public class EM {

	// initial model for each algorithm
	private List<Cluster> minitialModel;
	private List<Document> mDocuments;  // all documents
	private List<Double> classFrequency;
	private Map<Integer, Map<Integer, Double>> parameters;
	private Set<String> mDictionary;
	private List<Cluster> finalModel;
	private List<Cluster> intermediateModel;
	
	public EM(List<Cluster> initialModel, List<Document> documents, Set<String> dictionary) {
		minitialModel = initialModel;
		mDocuments = documents;
		classFrequency = new ArrayList<Double>();
		mDictionary = dictionary;
		parameters = new HashMap<Integer, Map<Integer,Double>>();
		intermediateModel = new ArrayList<Cluster>();
		finalModel = new ArrayList<Cluster>();
	}
	
	public List<Cluster> getModel() {
		return finalModel;
	}
	
	// train the model
	public void train() {
		System.out.println("initialization....");
		initialization();
		double loglikelihood = calculateLoglikelihood(minitialModel);
		for (Cluster cluster : minitialModel) {
			Cluster newcluster = new Cluster(cluster.mID);
			newcluster.addDocuments(cluster.documents);
			intermediateModel.add(newcluster);
		}
		while (true) {
			System.out.println("E step....");
			finalModel = E(intermediateModel);
			System.out.println("M Step....");
			classFrequency = new ArrayList<Double>();
			M(finalModel);
			double updateloglikelihood = calculateLoglikelihood(finalModel);
			System.out.println(loglikelihood + ".....update ...." + updateloglikelihood);
			if (loglikelihood < updateloglikelihood) {
				loglikelihood = updateloglikelihood;
			} else {
				break;
			}
			intermediateModel = new ArrayList<Cluster>();
			for (Cluster cluster : finalModel) {
				Cluster newcluster = new Cluster(cluster.mID);
				newcluster.addDocuments(cluster.documents);
				intermediateModel.add(newcluster);
			}

			finalModel = new ArrayList<Cluster>();
		}
	}
	
	public void M(List<Cluster> updateModel) {
		int n = 0;
		int q = updateModel.size();
		double denominator = 0.0;
		int v = mDictionary.size();
		for (Cluster cluster : updateModel) {
			n += cluster.getDocuments().size();
		}
		
		// calculate P(c | \hat{\theta})
		for (int i = 0; i < q; i++) {
			double frequency = (1.0 + updateModel.get(i).getDocuments().size()) / (q + n);
			classFrequency.add(frequency);
			Cluster cluster = updateModel.get(i);
			List<Document> documents = cluster.getDocuments();
			for (Document document : documents) {
				Matrix vector = document.vector;
				for (int k = 0; k < vector.getRowDimension(); k++) {
					denominator += vector.get(k, 0);
				}
			}
		}
		
		// calculate P(w | c, \hat{\theta})
		for (int i = 0; i < q; i++) {
			parameters.put(i, new HashMap<Integer, Double>());
			Cluster cluster = updateModel.get(i);
			List<Document> documents = cluster.getDocuments();
			int j = 0;
			for (String key : mDictionary) {
				double numerator = 0.0;
				for (Document document : documents) {
					Matrix vector = document.vector;
					numerator += vector.get(j, 0);
				}
				parameters.get(i).put(j, (1 + numerator) / (v + denominator));
				j += 1;
			}
		}
	}
	
	/**
	 * calcualte the loglikehood
	 * @return
	 */
	public double calculateLoglikelihood(List<Cluster> model) {
		double loglikelihood = 0.0;
		for (int i = 0; i < model.size(); i++) {
			Cluster cluster = model.get(i);
			List<Document> documents = cluster.getDocuments();
			for (Document document: documents) {
				loglikelihood += Math.log(classFrequency.get(i));
				Matrix vector = document.vector;
				for (int j = 0; j < vector.getRowDimension(); j++) {
					if (vector.get(j, 0) > 0) loglikelihood += Math.log(vector.get(j, 0));
				}
			}
		}
		
		return loglikelihood;
	}
	
	/**
	 * the NB classifier is used to assign probability-weighted category labels to all documents,
	 * including previously unlabeled documents
	 * <b>NOTE</b>
	 * in order to avoid the overflow, we at first use ln to calculate the probability, and then use
	 * e to convert back
	 * 
	 */
	public List<Cluster> E(List<Cluster> model) {
		List<Cluster> updateModel = new ArrayList<Cluster>();
		for (Cluster cluster : model) {
			Cluster newcluster = new Cluster(cluster.mID);
			updateModel.add(newcluster);
		}
		
		for (Document document : mDocuments) {
			List<Double> scores = new ArrayList<Double>();
			Matrix vector = document.vector;
			double denominator = 0.0;
			for (int i = 0; i < updateModel.size(); i++) {
				double numerator = 0.0;
				numerator += Math.log(classFrequency.get(i));
				for (int j = 0; j < vector.getRowDimension(); j++) {
					double value = vector.get(j, 0);
					if (value > 0) {
						numerator += Math.log(parameters.get(i).get(j));
					}
				}
				scores.add(numerator);
				denominator += Math.exp(numerator);
			}
			
			// normalize
			for (int i = 0; i < scores.size(); i++) {
				scores.set(i, Math.exp(scores.get(i)) / denominator);
			}
			
			int classLabel = maximum(scores);
			
			Cluster cluster = updateModel.get(classLabel);
			cluster.addDocument(document);
		}
		
		return updateModel;
	}

	/**
	 * find the maximum index with the maximum value from the list
	 * @param scores
	 * @return
	 */
	public int maximum(List<Double> scores) {
		double maximumValue = Collections.max(scores);
		int index = 0;
		for (int i = 0; i < scores.size() - 1; i++) {
			Double score = scores.get(i);
			if (score == maximumValue) {
				index = i;
				break;
			}
		}
		
		return index;
	}
	
	/**
	 * the model parameters are estimated using only documents labeled in the proposed initial
	 * model
	 * <p>
	 * intialization step is the same as M step, so I just use this method
	 * 
	 */
	public void initialization() {
		int n = 0;
		int q = minitialModel.size();
		double denominator = 0.0;
		int v = mDictionary.size();
		for (Cluster cluster : minitialModel) {
			n += cluster.getDocuments().size();
		}
		
		// calculate P(c | \hat{\theta})
		for (int i = 0; i < q; i++) {
			double frequency = (1.0 + minitialModel.get(i).getDocuments().size()) / (q + n);
			classFrequency.add(frequency);
			Cluster cluster = minitialModel.get(i);
			List<Document> documents = cluster.getDocuments();
			for (Document document : documents) {
				Matrix vector = document.vector;
				for (int k = 0; k < vector.getRowDimension(); k++) {
					denominator += vector.get(k, 0);
				}
			}
		}
		
		// calculate P(w | c, \hat{\theta})
		for (int i = 0; i < q; i++) {
			parameters.put(i, new HashMap<Integer, Double>());
			Cluster cluster = minitialModel.get(i);
			List<Document> documents = cluster.getDocuments();
			int j = 0;
			for (String key : mDictionary) {
				double numerator = 0.0;
				for (Document document : documents) {
					Matrix vector = document.vector;
					numerator += vector.get(j, 0);
				}
				parameters.get(i).put(j, (1 + numerator) / (v + denominator));
				j += 1;
			}
		}
		
	}

	
}
