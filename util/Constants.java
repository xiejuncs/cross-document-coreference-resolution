package edu.oregonstate.util;

public class Constants {

	// the corpus for debug
	public static final String DEBUG_CORPUS_PATH = "../corpus/EECB1/data/";
	
	// the mention annotation for debug
	public static final String DEBUG_MENTION_ANNOTATION_PATH = "../corpus/mentions-within.txt";
	
	// the corpus for running the whole experiment
	public static final String WHOLE_CORPUS_PATH = "../corpus/EECB1.0/data/";
	
	// the mention annotation for running the whole experiment
	public static final String WHOLE_MENTION_ANNOTATION_PATH = "../corpus/mentions-backup.txt";
	
	/** the corpus for debugging within coreference resolution case */
	public static final String DEBUG_WITHIN_CORPUS_PATH = "../corpus/EECB3.0/data/";
	
	/**the mention annotation for running the whole experiment */
	public static final String DEBUG_WITHIN_MENTION_ANNOTATION_PATH = "../corpus/mentions-within.txt";
	
	// the similarity dictionary created by Lin
	public static final String WORD_SIMILARITY_PATH = "../corpus/sims.lsp";
	
	// the configuration file for WORDNET
	public static final String WORD_NET_CONFIGURATION_PATH = "../corpus/file_properties.xml";
	
	// store the serilize and deserize result
	public static final String RESULT_PATH = "../corpus/RESULT/";
	
	/** Temporary result folder in order to further analysis */
	public static final String TEMPORY_RESULT_PATH = "../corpus/TEMPORYRESUTl/";
	
	// the path for SRL result
	public static final String TOKENS_OUTPUT_PATH = "../corpus/tokenoutput/";
	
	// Partial sieve configuration
	public static final String PARTIAL_SIEVE_STRING = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch";
	
	// Full sieve configuration
	public static final String FULL_SIEVE_STRING = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch, PronounMatch";
	
	/** configuration setting */
	public static final String EECB_PROP = "dcoref.eecb";
	
	public static final String SCORE_PROP = "dcoref.score";
	
	public static final String MENTION_FINDER_PROP = "dcoref.mentionFinder";
	
	/** if true, skip coreference resolution. do mention detection only */
	public static final boolean SKIP_COREF = false;
	
	/** store the intermediate result created during creating the adjacent states */
	public static final String ADJACENT_INTERMEDIATE_RESULT_PATH = "../corpus/ADJACENT-INTERMEDIATE-RESULT/";
}
