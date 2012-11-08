package edu.oregonstate.util;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;

/** 
 * Define the global constant in this class:
 * global constant can be file path
 * @author xie
 *
 */
public class SystemOptions {

	// whether debug or run the whole experiment
	public boolean DEBUG;
	
	// whether use FULL_SIEVE or PARTIAL SIEVE
	public boolean FULL_SIEVE;
	
	// Replicate Stanford Experiment
	public boolean REPLICATE_STANFORD_EXPERIMENT;
	
	// Do within coreference resolution first and then do coreference resolution
	public boolean WITHIN_COREFERNECE_RESOLUTION;
	
	// Do beam search
	public boolean BEAM_SEARCH;
	
	// Just use gold mentions
	public boolean GOLD_ONLY;
	
	// Do not consider align the predicted mentions and gold mentions
	public boolean ALIGN_PREDICTED_GOLD;
	
	// Align the predicted mentions and gold mentions by making the gold mentions and gold mentions having the equal mentions
	public boolean ALIGN_PREDICTED_GOLD_EQUAL;
	
	// Align the predicted mentions and gold mentions by putting spurious predicted mentions as singleton clusters in gold clusters
	public boolean ALIGN_PREDICTED_GOLD_PARTIAL;
	
	/** restricted or unrestricted case */
	public boolean RESTRICTED;
	
	/** do post process */
	public boolean POST_PROCESS;
	
	/** whether to incorporate the SRL result across the topic */
	public boolean TOPIC_SRL_RESULT;
	
	/** whether to incorporate the SRL result just within the document */
	public boolean DOCUMENT_SRL_RESULT;
	
	/** EecbTopic documentSentence Null case */
	public boolean ENABLE_NULL;
	
	/** output text to SRL software */
	public boolean OUTPUTTEXT;
	
	/** 
	 * Right now, we have four cases to run
	 * 1: STANFORDEXPERIMENT
	 * 2: DIRECTCROSS
	 * 3: JOINTWITHINCROSS
	 * 4: WITHINCROSS
	 */
	public int EXPERIMENTN;

	public List<ScoreType> scoreTypes;
	
	
	/**
	 * configuration of the experiments
	 */
	public SystemOptions() {
		// TODO
		DEBUG = true;
		FULL_SIEVE = false;
		REPLICATE_STANFORD_EXPERIMENT = true;
		WITHIN_COREFERNECE_RESOLUTION = false;
		BEAM_SEARCH = true;
		GOLD_ONLY = true;
		ALIGN_PREDICTED_GOLD = false;
		ALIGN_PREDICTED_GOLD_EQUAL = false;
		ALIGN_PREDICTED_GOLD_PARTIAL = false;
		RESTRICTED = false;
		POST_PROCESS = false;
		TOPIC_SRL_RESULT = false;
		DOCUMENT_SRL_RESULT = false;
		ENABLE_NULL = false;
		OUTPUTTEXT = false;
		EXPERIMENTN = 4;
		scoreTypes = new ArrayList<CorefScorer.ScoreType>();
		scoreTypes.add(ScoreType.Pairwise);
		//scoreTypes.add(ScoreType.BCubed);
		//scoreTypes.add(ScoreType.MUC);
		//scoreTypes.add(ScoreType.CEAF);
	}
}
