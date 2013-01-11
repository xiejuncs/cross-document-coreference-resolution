package edu.oregonstate.lossfunction;

import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.Document;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.search.State;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;

/**
 * Loss Function used to calculate the loss score
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class MetricLossFunction implements ILossFunction {

	/* score type: Pairwise */
	private ScoreType mtype;
	
	/* numerator and denominator of  precision and recall */
	private double precisionNumSum;
    private double precisionDenSum;
    private double recallNumSum;
    private double recallDenSum;
	
	public MetricLossFunction() {
		mtype = CorefScorer.ScoreType.valueOf(ExperimentConstructor.experimentProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP));
	}
	
	/* calculate loss function according to different state, but with the same document */
	public double[] calculateLossFunction(Document document, State<CorefCluster> state) {
    	Command.generateStateDocument(document, state);
    	double[] scores = calculateF1(document, mtype);
    	return scores;
	}
	
	/* calculate F1, Precision and Recall according to the Score Type */
    private double[] calculateF1(Document document, ScoreType type) {
        double F1 = 0.0;
        CorefScorer score = EecbConstructor.createCorefScorer(type);
        
        score.calculateScore(document);
        F1 = score.getF1();
        double precision = score.getPrecision();
        double recall = score.getRecall();
        
        precisionNumSum = score.precisionNumSum;
        precisionDenSum = score.precisionDenSum;
        recallNumSum = score.recallNumSum;
        recallDenSum = score.recallDenSum;
        
        double[] result = {DoubleOperation.transformNaN(F1), DoubleOperation.transformNaN(precision), DoubleOperation.transformNaN(recall)};
        return result;
    }
    
    /* the detail information of a score */
    public String getDetailScoreInformation() {
    	return precisionNumSum + " " + precisionDenSum + " " + recallNumSum + " " + recallDenSum;
    }
    
    /* scoring the document at the first time */
    public double[] getMetricScore(Document document) {
    	return calculateF1(document, mtype);
    }
}
