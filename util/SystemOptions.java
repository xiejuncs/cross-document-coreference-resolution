package edu.oregonstate.util;

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
	
	/** whether to incorporate the SRL result */
	public boolean SRL_RESULT;
	
	/** EecbTopic documentSentence Null case */
	public boolean ENABLE_NULL;
	
	/**
	 * configuration of the experiments
	 */
	public SystemOptions() {
		// TODO
		DEBUG = true;
		FULL_SIEVE = false;
		REPLICATE_STANFORD_EXPERIMENT = false;
		WITHIN_COREFERNECE_RESOLUTION = false;
		BEAM_SEARCH = true;
		GOLD_ONLY = true;
		ALIGN_PREDICTED_GOLD = false;
		ALIGN_PREDICTED_GOLD_EQUAL = false;
		ALIGN_PREDICTED_GOLD_PARTIAL = false;
		RESTRICTED = false;
		POST_PROCESS = false;
		SRL_RESULT = false;
		ENABLE_NULL = false;
	}
}
