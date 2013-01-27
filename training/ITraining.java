package edu.oregonstate.training;

import java.util.List;
import java.util.Properties;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.LargetFileReading;
import edu.oregonstate.util.EecbConstants;

public abstract class ITraining {

	/* the length of the weight */
	protected final int length;
	
	/* largest file reader */
	protected final LargetFileReading reader;
	
	/* whether incorporate the zero good state */
	protected final boolean incorporateZeroVector;
	
	/* whether enable PA learning rate */
	protected final boolean enablePALearningRate;
	
	/* whether enable PA learning rate loss score */
	protected final boolean enablePALearningRateLossScore;
	
	public ITraining() {
		Properties mProps = ExperimentConstructor.experimentProps;
		length = FeatureFactory.getFeatures().length;
		reader = new LargetFileReading();
		incorporateZeroVector = Boolean.parseBoolean(mProps.getProperty(EecbConstants.INCORPORATE_ZERO_CASE_PROP, "true"));
		enablePALearningRate = Boolean.parseBoolean(mProps.getProperty(EecbConstants.ENABLE_PA_LEARNING_RATE, "false"));
		enablePALearningRateLossScore = Boolean.parseBoolean(mProps.getProperty(EecbConstants.ENABLE_PA_LEARNING_RATE_LOSSSCORE, "true"));
	}
	
	/* different weight update style */
	public abstract Parameter train(List<String> paths, Parameter para, double learningRate);
	
	/**
	 * According to Online Passive-Aggressive Paper, margin is the difference between gLossScore and bLossScore
	 * 
	 * @param gLossScore
	 * @param bLossScore
	 * @param direction
	 * @return
	 */
	protected double calculatePALossLearningRate(double gLossScore, double bLossScore, double[] direction, boolean lossScore) {
		double margin = 1.0;
		if (lossScore) {
			margin = gLossScore - bLossScore;
		}
		
		double length = DoubleOperation.calculateTwoNorm(direction);
		// length maybe 0, hence if length is 0, then the vectorLength just 1.
		double vectorLength = (length == 0.0) ? 1.0 : length;
		double learningRate = margin / vectorLength;
		
		return learningRate;
	}

	
}
