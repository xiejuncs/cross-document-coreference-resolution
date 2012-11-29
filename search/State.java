package edu.oregonstate.search;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import edu.oregonstate.features.Feature;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * represent the state  
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class State<T> implements Serializable {
	
	private static final long serialVersionUID = 8666265337578515592L;
	// every state consists of a set of CorefClusters, in order to make it generic
	
	/** state */
	private Map<Integer, T> state;
	
	/** id */
	private String id;
	
	/** features */
	private Counter<String> mfeatures;
	
	/** metric score, respectively F1, Precision and Recall */
	private double[] mMetricScore;
	
	/** cost score */
	private double mCostScore;
	
	/** score detail information, PrecisionNum, PrecisionDen, RecallNum, RecallDen, which can be used to calculate the overall performance of the algorithm */
	private String scoreDetailInformation;
	
	private String featureString;

	public State() {
		state = new HashMap<Integer, T>();
		id = "";
		mfeatures = new ClassicCounter<String>();
		mMetricScore = new double[3];
		mCostScore = 0.0;
		scoreDetailInformation = "";
	}
	
	public void setScoreDetailInformation(String scoreInformation) {
		scoreDetailInformation = scoreInformation;
	}
	
	public String getScoreDetailInformation() {
		return scoreDetailInformation;
	}
	
	public void setFeatures(Counter<String> featrues) {
		mfeatures = featrues;
		
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int i = 0; i < Feature.featuresName.length; i++) {
			String feature = Feature.featuresName[i];
			double value = mfeatures.getCount(feature);
			if (i == Feature.featuresName.length - 1) {
				sb.append(value);
			} else {
				sb.append(value + ", ");
			}
		}
		
		sb.append("]");
		
		featureString = sb.toString().trim();
	}
	
	public Counter<String> getFeatures() {
		return mfeatures;
	}
	
	public String featureString() {
		
		return featureString;
	}
	
	public void add(Integer i, T element) {
		state.put(i, element);
	}

	public T get(int i) {
		return state.get(i);
	}

	public void remove(int i) {
		state.remove(i);
	} 

	public Map<Integer, T> getState() {
		return state;
	}
	
	public void setID(String val) {
		this.id = val;
	}
	
	public String getID() {
		return this.id;
	}
	
	public void setScore(double[] score) {
		mMetricScore = score;
	}
	
	public double[] getScore() {
		return mMetricScore;
	}
	
	public void setCostScore(double score) {
		mCostScore = score;
	}
	
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
