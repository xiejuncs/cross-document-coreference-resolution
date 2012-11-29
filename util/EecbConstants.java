package edu.oregonstate.util;

import edu.oregonstate.experiment.ExperimentConstructor;

public class EecbConstants {

	// the corpus for debug
	public static final String DEBUG_CORPUS_PATH = ExperimentConstructor.corpusPath + "corpus/EECB1/data/";
	
	// the mention annotation for debug
	public static final String DEBUG_MENTION_ANNOTATION_PATH = ExperimentConstructor.corpusPath + "corpus/mentions-backup.txt";
	
	// the corpus for running the whole experiment
	public static final String WHOLE_CORPUS_PATH = ExperimentConstructor.corpusPath + "corpus/EECB1.0/data/";
	
	// the mention annotation for running the whole experiment
	public static final String WHOLE_MENTION_ANNOTATION_PATH = ExperimentConstructor.corpusPath + "corpus/mentions-backup.txt";
	
	/** the corpus for debugging within coreference resolution case */
	public static final String DEBUG_WITHIN_CORPUS_PATH = ExperimentConstructor.corpusPath + "corpus/EECB3.0/data/";
	
	/**the mention annotation for running the whole experiment */
	public static final String DEBUG_WITHIN_MENTION_ANNOTATION_PATH = ExperimentConstructor.corpusPath + "corpus/mentions-within.txt";
	
	// the similarity dictionary created by Lin
	public static final String WORD_SIMILARITY_PATH = ExperimentConstructor.corpusPath + "corpus/sims.lsp";
	
	// the configuration file for WORDNET
	public static final String WORD_NET_CONFIGURATION_PATH = ExperimentConstructor.corpusPath + "corpus/file_properties.xml";
	
	// store the serilize and deserize result
	//public static final String RESULT_PATH = ExperimentConstructor.corpusPath + "corpus/RESULT/";
	public static final String RESULT_PATH = ExperimentConstructor.corpusPath + "corpus/CROSS-RESULT/";
	
	/** Temporary result folder in order to further analysis */
	public static final String TEMPORY_RESULT_PATH = ExperimentConstructor.corpusPath + "corpus/TEMPORYRESUT/";
	
	// the path for SRL result
	public static final String TOKENS_OUTPUT_PATH = ExperimentConstructor.corpusPath + "corpus/tokenoutput/";
	
	// the path for single document SRL result
	//public static final String TOKENS_OUTPUT_PATH = ExperimentConstructor.corpusPath + "corpus/srloutputforsingledocument/2/";
	
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
	public static final String ADJACENT_INTERMEDIATE_RESULT_PATH = ExperimentConstructor.corpusPath + "corpus/ADJACENT-INTERMEDIATE-RESULT/";
	
	public static final String STOPWORD_PATH = ExperimentConstructor.corpusPath + "corpus/english.stop";
	
	public static final String CLASSIFIER = "classifier";
	
	public static final String COSTFUNCTION = "costfunction";
	
	public static final String LOSSFUNCTION = "lossfunction";
	
	public static final String CLUSTERING = "clustering";
	
	public static final String SEARCHMETHOD = "searchmethod";
	
	public static final String DATASET = "dataset";
	
	public static final String SRL = "srl";
}
