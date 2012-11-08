package edu.oregonstate.search;

import Jama.Matrix;
import java.util.*;

import edu.oregonstate.CDCR;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
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
public class StochasticGradient {
	
	/** learning rate \eta */
	private double mEta;
	
	/** initial weight we can update */
	private Matrix mInitialWeight;
	
	/** the previous largest loss function, as an indicator function for y(x_{i}) */
	private double mThreshold;
	
	/** states information */
	private Map<String, State<CorefCluster>> mStates;
	
	/** row of the initial weight */
	private int row;
	
	/** cost score */
	private double mcostScore;
	
	public void setCostScore(double costscore) {
		mcostScore = costscore;
	}
	
	/** return the final weight */
	public Matrix getInitialWeight() {
		return mInitialWeight;
	}
	
	/** return the learning rate, for debug */
	public double getEta() {
		return mEta;
	}

	public void setInitialWeight(Matrix initialWeight) {
		mInitialWeight = initialWeight;
	}
	
	/** cascade constructor */
	public StochasticGradient(double eta, double threshold, Map<String, State<CorefCluster>> states) {
		this(eta, threshold, states, null);
	}
	
	public StochasticGradient(double eta, double threshold, Map<String, State<CorefCluster>> states, Matrix initialWeight) {
		mEta = eta;
		mThreshold = threshold;
		mStates = states;
		mInitialWeight = initialWeight;
	}

	/**
	 * update the weight according to the update rule
	 */
	public void updateWeight() {
		System.out.println("update the stochastic gradient weight..................");
		row = mInitialWeight.getRowDimension();
		Matrix totalMatrix = calculateWeight().times(mEta);
		mInitialWeight = mInitialWeight.times(1 - mEta);
		mInitialWeight = mInitialWeight.plus(totalMatrix);
	}
	
	/** calculate the total weight */
	private Matrix calculateWeight() {
		System.out.println("calculate the total weight for the pairwise and signle case");
		Matrix pariwiseWeight = calculatePairwiseWeight();
		Matrix singleWeight = calculateSingleWeight();
		Matrix totalMatrix = pariwiseWeight.plus(singleWeight);
		//Matrix totalMatrix = singleWeight;
		return totalMatrix;
	}
	
	/** calculate the weight according to whether the score is one state is better than that of the other state */
	private Matrix calculatePairwiseWeight() {
		System.out.println("calculate the pariwise weight");
		
		Matrix pairwiseMatrix = new Matrix(row, 1);
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

				boolean violated = violated(ifscore, jfscore, icostscore, jcostscore);
				if (!violated) continue;
				
				int indicator = pairwiseIndicatorFunction(ifscore, jfscore);
				Matrix iMatrix = convertFeature(ifeature);
				Matrix jMatrix = convertFeature(jfeature);
				Matrix difference = iMatrix.minus(jMatrix);
				Matrix signeddifference = difference.times(indicator);
				pairwiseMatrix = pairwiseMatrix.plus(signeddifference);
			}
		}
		
		return pairwiseMatrix;
	}
	
	/** test whether cost score is consistent with the lost score */
	private boolean violated(double ifscore, double jfscore, double icostscore, double jcostscore) {
		boolean violated = true;
		if (ifscore >= jfscore) {
			if (icostscore >= jcostscore) {
				violated = false;
			}
		} else {
			if (icostscore < jcostscore) {
				violated = false;
			}
		}
		
		return violated;
	}
	
	/** indicator function */
	private int pairwiseIndicatorFunction(double ifscore, double jfscore) {
		return ifscore > jfscore ? 1 : -1;
	}
	
	/** calculate the weight according to whether the score of one state is better than the loss function */
	private Matrix calculateSingleWeight() {
		System.out.println("calculate the single weight");
		ResultOutput.writeTextFile(CDCR.outputFileName, "calculate the single weight");
		Matrix singleMatrix = new Matrix(row, 1);
		for (String key : mStates.keySet()) {
			State<CorefCluster> state = mStates.get(key);
			double fscore = state.getScore()[0];
			double costscore = state.getCostScore();
			boolean violated = violated(fscore, mThreshold, costscore, mcostScore);
			if (!violated) continue;
			int indicator = pairwiseIndicatorFunction(fscore, mThreshold);
			Counter<String> feature = state.getFeatures();
			Matrix featureMatrix = convertFeature(feature);
			featureMatrix = featureMatrix.times(indicator);
			singleMatrix = singleMatrix.plus(featureMatrix);
		}
		return singleMatrix;
	}
	
	/** convert counter feature to matrix feature according to the feature name */
	private Matrix convertFeature(Counter<String> feature) {
		Matrix matrix = new Matrix(row, 1);
		for (int i = 0; i < Feature.featuresName.length; i++) {
			double number = feature.getCount(Feature.featuresName[i]);
			if (Double.isNaN(number)) {
				number = 0.0;
			}
			matrix.set(i, 0, number);
		}
		
		return matrix;
	}
	
}
