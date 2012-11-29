package edu.oregonstate.lossfunction;

import java.util.Map;
import java.util.ResourceBundle;

import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.dcoref.Document;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.State;
import edu.oregonstate.util.DocumentMerge;
import edu.oregonstate.util.EecbConstants;

public class MetricLossFunction implements ILossFunction {

	private ScoreType mtype;
	
	private Document mdocument;
	
	private State<CorefCluster> mstate;
	
	private double[] scores;
	
	private double precisionNumSum;
    private double precisionDenSum;
    private double recallNumSum;
    private double recallDenSum;
	
	public MetricLossFunction() {
		mtype = (ScoreType) ExperimentConstructor.getParameter(EecbConstants.LOSSFUNCTION, "scoreType");
		scores = new double[3];
	}
	
	public double[] getLossScore(){
		return scores;
	}
	
	public void setDocument(Document document) {
		mdocument = document;
	}
	
	public void setState(State<CorefCluster> state) {
		mstate = state;
	}
	
	public void calculateLossFunction() {
		Document documentState = new Document();
		DocumentMerge dm = new DocumentMerge(mdocument, documentState);
		dm.addDocument();
    	setNextDocument(documentState, mstate);
    	scores = calculateF1(documentState, mtype);
	}
	
	private void setNextDocument(Document documentState, State<CorefCluster> state) {
		documentState.corefClusters = state.getState();
		for (Integer id : documentState.corefClusters.keySet()) {
			CorefCluster cluster = documentState.corefClusters.get(id);
			for (Mention m : cluster.corefMentions) {
				int mentionID = m.mentionID;
				Mention correspondingMention = documentState.allPredictedMentions.get(mentionID);
				int clusterid = id;
				correspondingMention.corefClusterID = clusterid;
			}
		}
	}
	
	// calculate F1, Precision and Recall according to the Score Type
    private double[] calculateF1(Document document, ScoreType type) {
        double F1 = 0.0;
        CorefScorer score;
        switch(type) {
            case MUC:
                score = new ScorerMUC();
                break;
            case BCubed:
                score = new ScorerBCubed(BCubedType.Bconll);
                break;
            case CEAF:
                score = new ScorerCEAF();
                break;
            case Pairwise:
                score = new ScorerPairwise();
                break;
            default:
                score = new ScorerMUC();
                break;
        }
        
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
    
    public String getDetailScoreInformation() {
    	return precisionNumSum + " " + precisionDenSum + " " + recallNumSum + " " + recallDenSum;
    }
    
    public double[] getMetricScore() {
    	return calculateF1(mdocument, mtype);
    }
}
