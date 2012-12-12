package edu.oregonstate.classifier;

import java.util.*;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.general.PriorityQueue;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.State;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.stats.Counter;

/**
 * Implementation of Stochastic gradient algorithm.
 * 
 * The algorithm is specified below: 
 * $w = w - \eta * (w - \sum y(x_{i}, x_{j})) * (x_{i} - x_{j}) - \sum y(x_{i}) * x_{i}$
 * 
 * \eta is the learning rate
 * y(x_{i}) = { 1  if x_{i} action improve the loss 
              { -1 otherwise
               
   y(x_{i}, x_{j}) = { 1  if x_{i} is better than x_{j} according to the loss function
   					 { -1 otherwise
   					 
   The final result is
   w^{'} = (1 - \eta) * w + \eta * (\sum y(x_{i}, x_{j}) * (x_{i} - x_{j}) + \sum y(x_{i}) * x_{i})
 * 
 * we just need to update the weight according to the above equation
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class StochasticGradient implements IClassifier {
	
	/** learning rate \eta */
	private double mEta;
	
	/** initial weight we can update */
	private double[] mWeight;
	
	private double[] mTotalWeight;
	
	/** the previous largest loss function, as an indicator function for y(x_{i}) */
	private double mbestScore;
	
	/** states information */
	private Map<String, State<CorefCluster>> mStates;
	
	/** row of the initial weight */
	private int row;
	
	/** how many violations */
	private int mViolations;
	
	/** search index, useful for debugging */
	private int mSearchStep;
	
	public void setBestScore(double bestScore) {
		mbestScore = bestScore;
	}
	
	/** return the final weight */
	public double[] getInitialWeight() {
		return mWeight;
	}
	
	/** return the learning rate, for debug */
	public double getEta() {
		return mEta;
	}
	
	public void setRightLinks(Set<String> rightLinks){
	}
	
	public void setStatesLossFunction(PriorityQueue<State<CorefCluster>> statesLossFunction) {
	}
	
	public void setBeam(FixedSizePriorityQueue<State<CorefCluster>> beam){
		
	}
	
	public void setBestState(State<CorefCluster> bestState){
		
	}
	
	public int getViolations(){
		return mViolations;
	}
	
	public void setWeight(double[] weight) {
		mWeight = weight;
		row = mWeight.length;
	}
	
	public double[] getWeight() {
		return mWeight;
	}
	
	public void setTotalWeight(double[] totalWeight) {
		mTotalWeight = totalWeight;
	}
	
	public double[] getTotalWeight() {
		return mTotalWeight;
	}
	
	public void setState(Map<String, State<CorefCluster>> states) {
		mStates = states;
	}
	
	public void setSearchStep(int searchStep) {
		mSearchStep = searchStep;
	}
	
	/** cascade constructor */
	public StochasticGradient() {
		mEta = Double.parseDouble(ExperimentConstructor.property.getProperty("learningRate"));
	}
	
	public void setPreviousBestState(State<CorefCluster> previousBestState){
	}

	/**
	 * update the weight according to the update rule
	 */
	public void train() {
		double[] regularizer = calculateWeight();
		double[] weightedRegularizer = DoubleOperation.time(regularizer, mEta);
		mWeight = DoubleOperation.time(mWeight, 1 - mEta);
		mWeight = DoubleOperation.add(mWeight, weightedRegularizer);
		mTotalWeight = DoubleOperation.add(mTotalWeight, mWeight);
	}
	
	/** calculate the total weight */
	private double[] calculateWeight() {
		double[] pariwiseWeight = calculatePairwiseWeight();
		double[] singleWeight = calculateSingleWeight();
		double[] regularizer = DoubleOperation.add(singleWeight, pariwiseWeight);
		//Matrix totalMatrix = singleWeight;
		return regularizer;
	}
	
	/** calculate the weight according to whether the score is one state is better than that of the other state */
	private double[] calculatePairwiseWeight() {
		double[] pairwiseMatrix = new double[row];
		List<String> stateKeys = new ArrayList<String>();
		for (String key : mStates.keySet()) {
			stateKeys.add(key);
		}
		
		for (int i = 0; i < stateKeys.size(); i++) {
			State<CorefCluster> iState = mStates.get(stateKeys.get(i));
			double ifscore = iState.getScore()[0];
			double icostscore = iState.getCostScore();
			Counter<String> ifeature = iState.getFeatures();
			for (int j = 0; j < i; j++) {
				State<CorefCluster> jState = mStates.get(stateKeys.get(j));
				double jfscore = jState.getScore()[0];
				double jcostscore = jState.getCostScore();
				Counter<String> jfeature = jState.getFeatures();
				
				int indicator = pairwiseIndicatorFunction(ifscore, jfscore);
				double loss = Math.max(0.0, 1 - indicator * (icostscore - jcostscore));
				if (loss > 0) {
					double[] difference = convertFeature(ifeature, jfeature);
					double[] signeddifference = DoubleOperation.time(difference, indicator);
					pairwiseMatrix = DoubleOperation.add(pairwiseMatrix, signeddifference);
					mViolations++;
				}
			
			}
		}
		
		return pairwiseMatrix;
	}

	
	/** indicator function */
	private int pairwiseIndicatorFunction(double ifscore, double jfscore) {
		return ifscore > jfscore ? 1 : -1;
	}
	
	/** calculate the weight according to whether the score of one state is better than the loss function */
	private double[] calculateSingleWeight() {
		double[] singleMatrix = new double[row];
		for (String key : mStates.keySet()) {
			State<CorefCluster> state = mStates.get(key);
			double fscore = state.getScore()[0];
			double costscore = state.getCostScore();
			int indicator = pairwiseIndicatorFunction(fscore, mbestScore);
			double loss = 1 - costscore * indicator;
			double maximumLoss = Math.max(0.0, loss);
			if (maximumLoss > 0) {
				Counter<String> feature = state.getFeatures();
				double[] featureMatrix = convertSingleFeature(feature);
				featureMatrix = DoubleOperation.time(featureMatrix, indicator); 
				singleMatrix = DoubleOperation.add(singleMatrix, featureMatrix); 
				mViolations++;
			}
		}
		return singleMatrix;
	}
	
	/**
	 * convert feature
	 * 
	 * @param ifeature
	 * @return
	 */
	private double[] convertSingleFeature(Counter<String> ifeature) {
		double[] features = new double[row]; 
		for (int i = 0; i < Feature.featuresName.length; i++) {
			String feature = Feature.featuresName[i];
			double ivalue = ifeature.getCount(feature);
			features[i] = ivalue;
		}
		
		return features;
	}
	
	/** convert two feature vector to a double array */
	private double[] convertFeature(Counter<String> ifeature, Counter<String> jfeature) {
		double[] difference = new double[row]; 
		for (int i = 0; i < Feature.featuresName.length; i++) {
			String feature = Feature.featuresName[i];
			double ivalue = ifeature.getCount(feature);
			double jvalue = jfeature.getCount(feature);
			difference[i] = ivalue - jvalue;
		}
		
		return difference;
	}
	
	
	
}
