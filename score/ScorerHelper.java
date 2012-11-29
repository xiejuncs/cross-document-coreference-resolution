package edu.oregonstate.score;

import java.util.logging.Logger;

import edu.oregonstate.CorefSystem;
import edu.oregonstate.io.ResultOutput;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;

/**
 * All stuffs related to the Score Function. Now, there 
 * are four score metrics implemented, respectively Pairwise, 
 * MUC, BCubed and CEAF.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ScorerHelper {

	// we evaluate the score on a specific document
	private Document mDocument;
	private Logger mLogger;
	private String mPath;
	private boolean mPostProcess;

	public ScorerHelper(Document document, Logger logger, String path, boolean postProcess) {
		mDocument = document;
		mLogger = logger;
		mPath = path;
		mPostProcess = postProcess;
	}
	
	/** print score of the document, whether post-processing or not */
	public void printScore() {
		if (!mPostProcess) {
    		ResultOutput.writeTextFile(mPath, "do not postprocess the data");
    		
    		CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
        	score.calculateScore(mDocument);
        	score.printF1(mLogger, true);
        	
        	CorefScorer ceafscore = new ScorerCEAF();
        	ceafscore.calculateScore(mDocument);
        	ceafscore.printF1(mLogger, true);
    		
    		CorefScorer mucscore = new ScorerMUC();
    		mucscore.calculateScore(mDocument);
    		mucscore.printF1(mLogger, true);
    	
    		CorefScorer pairscore = new ScorerPairwise();
    		pairscore.calculateScore(mDocument);
    		pairscore.printF1(mLogger, true);
    		
    		// Average of MUC, B^{3} and CEAF-\phi_{4}.
    		double conllF1 = (score.getF1() + ceafscore.getF1() + mucscore.getF1()) / 3;
        	ResultOutput.writeTextFile(mPath, "conllF1:     " + conllF1);
    	} else {
    		ResultOutput.writeTextFile(mPath, "do postprocess the data");	
    		CorefSystem cs = new CorefSystem();
        	cs.corefSystem.postProcessing(mDocument);
        	
        	CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
        	score.calculateScore(mDocument);
        	score.printF1(mLogger, true);
        	
        	CorefScorer postmucscore = new ScorerMUC();
        	postmucscore.calculateScore(mDocument);
        	postmucscore.printF1(mLogger, true);
        	
        	CorefScorer postpairscore = new ScorerPairwise();
        	postpairscore.calculateScore(mDocument);
        	postpairscore.printF1(mLogger, true);
        	
        	CorefScorer ceafscore = new ScorerCEAF();
        	ceafscore.calculateScore(mDocument);
        	ceafscore.printF1(mLogger, true);
        	
        	
        	// Average of MUC, B^{3} and CEAF-\phi_{4}.
        	double conllF1 = (score.getF1() + ceafscore.getF1() + postmucscore.getF1()) / 3;
        	ResultOutput.writeTextFile(mPath, "conllF1:     " + conllF1);
    	}
	}
	
	
}
