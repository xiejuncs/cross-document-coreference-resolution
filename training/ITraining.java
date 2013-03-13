package edu.oregonstate.training;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.LargetFileReading;
import edu.oregonstate.util.EecbConstants;

/**
 * whether incorporate the negative instance, according to the paper : Tuning as Ranking
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class ITraining {

	/* the length of the weight */
	protected final int length;
	
	/* largest file reader */
	protected final LargetFileReading reader;
	
	/* whether incorporate the zero good state */
	protected final boolean incorporateZeroVector;
	
	/* hyperparameter for AROW */
	protected final double mHyperParameter;
	
	/* eanble PA learning */
	protected final boolean enablePALearning;
	
	/* whether enable PA learning rate loss score */
	private final boolean enablePALearningRateLossScore;
	
	/* enable discrepancy */
	private final boolean enablePADiscrepancy;
	
	/* enable margin */
	private final boolean enablePAMargin;
	
	/* enable normalize the weight */
	protected final boolean enableNormalizeWeight;
	
	public ITraining() {
		Properties mProps = ExperimentConstructor.experimentProps;
		length = FeatureFactory.getFeatureTemplate().size();
		reader = new LargetFileReading();
		incorporateZeroVector = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_INCORPORATE_ZERO_CASE, "true"));
		enablePALearning = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_PA, "false"));
		enablePALearningRateLossScore = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_PA_RATE_LOSSSCORE, "true"));
		enablePADiscrepancy = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_PA_DISCREPANCY, "true"));
		enablePAMargin = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_PA_MARGIN, "true"));
		enableNormalizeWeight = Boolean.parseBoolean(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_NORMALIZE_WEIGHT, "true"));
		mHyperParameter = Double.parseDouble(mProps.getProperty(EecbConstants.CLASSIFIER_TRAINING_HYPERPARAMETER, "1.0"));
	}
	
	/* different weight update styles, including Batch, Online and OnlineToBatch */
	public abstract Parameter train(List<String> paths, Parameter para, double learningRate);
	
	/**
	 * create random integer list
	 * 
	 * @param size
	 * @return
	 */
	protected List<Integer> createRandomIndex(int size) {
		List<Integer> arrays = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			arrays.add(i);
		}
		
		Collections.shuffle(arrays);
		return arrays;
	}
	
	/**
	 * calculate the loss
	 * 
	 * @param gLossScore
	 * @param bLossScore
	 * @param gNumericalFeatures
	 * @param bNumericalFeatures
	 * @param weight
	 * @return
	 */
	protected double calculatePALoss(double gLossScore, double bLossScore, double[] gNumericalFeatures, 
			 						 double[] bNumericalFeatures, double[] weight) {
		double loss = 0.0;
		
		// calculate margin
		if (enablePAMargin) {
			double margin = 1.0;
			if (enablePALearningRateLossScore) {
				margin = gLossScore - bLossScore;
			}
			loss += margin;
		}
		
		// calculate the discrepancy
		if (enablePADiscrepancy) {
			double[] weightForCalculatingCost = null;
			if (enableNormalizeWeight) {
				weightForCalculatingCost = DoubleOperation.normalize(weight);
			} else {
				weightForCalculatingCost = weight;
			}
		
			double bCostScore = DoubleOperation.time(bNumericalFeatures, weightForCalculatingCost);
			double gCostScore = DoubleOperation.time(gNumericalFeatures, weightForCalculatingCost);
			loss += bCostScore - gCostScore;
		}
		
		return loss;
	}
	
}
