package edu.oregonstate.search;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import edu.oregonstate.features.FeatureFactory;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * represent the state  
 * 
 * every state consists of a set of CorefClusters, in order to make it generic
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class State<T> implements Serializable {
	
	private static final long serialVersionUID = 8666265337578515592L;
	
	/** state */
	private Map<Integer, T> state;
	
	/** id */
	private String id;
	
	/** features */
	private Counter<String> mfeatures;
	
	/* numerical feature used to calculate the heuristic score */
	private double[] numericalFeatures;
	
	/** metric score, respectively F1, Precision and Recall */
	private double[] mMetricScore;
	
	/** cost score */
	private double mCostScore;
	
	/** score detail information, PrecisionNum, PrecisionDen, RecallNum, RecallDen, which can be used to calculate the overall performance of the algorithm */
	private String scoreDetailInformation;
	
	/* feature string of the state which is used to debug */
	private String featureString;

	/* the metric F1 score, for example, if we use Pairwise as metric score, then the F1score is Pairwise F1 score */
	private double F1score;

	public State() {
		state = new HashMap<Integer, T>();
		id = "";
		mfeatures = new ClassicCounter<String>();
		mMetricScore = new double[3];
		mCostScore = 0.0;
		scoreDetailInformation = "";
		F1score = 0.0;
		featureString = "";
		numericalFeatures = new double[FeatureFactory.getFeatures().length];
	}
	
	/* set numerical feature which is used for Perceptron update */
	public void setNumericalFeatures(double[] features) {
		assert features.length == numericalFeatures.length;
		System.arraycopy(features, 0, numericalFeatures, 0, features.length);
	}
	
	/* get numerical feature */
	public double[] getNumericalFeatures() {
		return numericalFeatures;
	}
	
	/* set metric F1 score */
	public void setF1Score(double score) {
		F1score = score;
	}
	
	/* get metric F1 score */
	public double getF1Score() {
		return F1score;
	}
	
	/* set score detail information */
	public void setScoreDetailInformation(String scoreInformation) {
		scoreDetailInformation = scoreInformation;
	}
	
	/* get score detail information */
	public String getScoreDetailInformation() {
		return scoreDetailInformation;
	}
	
	/* set features */
	public void setFeatures(Counter<String> featrues) {
		mfeatures = featrues;
		
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		String[] features = FeatureFactory.getFeatures();
		for (int i = 0; i < features.length; i++) {
			String feature = features[i];
			double value = mfeatures.getCount(feature);
			if (i == features.length - 1) {
				sb.append(value);
			} else {
				sb.append(value + ", ");
			}
		}
		
		sb.append("]");
		
		featureString = sb.toString().trim();
	}
	
	/* get features */
	public Counter<String> getFeatures() {
		return mfeatures;
	}
	
	/* return feature string */
	public String featureString() {
		return featureString;
	}
	
	/* add element to state */
	public void add(Integer i, T element) {
		state.put(i, element);
	}

	/* get the ith index element */
	public T get(int i) {
		return state.get(i);
	}

	/* remove the ith index element */
	public void remove(int i) {
		state.remove(i);
	}
	
	/* get state */
	public Map<Integer, T> getState() {
		return state;
	}
	
	/* set the id for the state */
	public void setID(String val) {
		this.id = val;
	}
	
	/* get the id of the state */
	public String getID() {
		return this.id;
	}
	
	/* set metric score */
	public void setScore(double[] score) {
		mMetricScore = score;
	}
	
	/* get metric score */
	public double[] getScore() {
		return mMetricScore;
	}
	
	/* set cost score of the state */
	public void setCostScore(double score) {
		mCostScore = score;
	}
	
	/* get cost score of the state */
	public double getCostScore() {
		return mCostScore;
	}
	
	// This one needs to be take cared
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			@SuppressWarnings("unchecked")
			State<T> s = (State<T>) obj;
			Map<Integer, T> sList = s.state;
			if (sList.size() == state.size()) {
				Set<T> objValues = new HashSet<T>(s.getState().values());
				Set<T> stateValues = new HashSet<T>(state.values());
				boolean equal = objValues.equals(stateValues);
				return equal;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("state: \n");
		for (Integer key : state.keySet()) {
			sb.append(state.get(key).toString() + " \n");
		}
		
		return sb.toString().trim();
	}
	
}
