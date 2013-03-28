package edu.oregonstate.util;

/**
 * defined the constant fileds which are used in the experiment
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbConstants {

	/* non-instantiable class */
	private EecbConstants() {
	}

	/** partial sieves, does not including pronoun sieve, which is the right configuration for Stanford experiment and our own experiment */
	public static final String PARTIAL_SIEVE_STRING = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch";

	/** full sieve, including Pronoun Sieve */
	public static final String FULL_SIEVE_STRING = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch, PronounMatch";

	public static final String FEATURE_NAMES = "Head, Lemma, Synonym, SrlAgreeCount, SrlA0, SrlA1, SrlA2, SrlAMLoc, SrlPA0, SrlPA1, SrlPA2, SrlPAMLoc, MentionWord, NEType, Animacy, Gender, Number";

	public static final String NFEATURE_NAMES = "Head, Lemma, Synonym, SrlAgreeCount, SrlA0, SrlA1, SrlA2, SrlAMLoc, SrlPA0, SrlPA1, SrlPA2, SrlPAMLoc, NSrlAgreeCount, " +
			"NSrlA0, NSrlA1, NSrlA2, NSrlAMLoc, NSrlPA0, NSrlPA1, NSrlPA2, NSrlPAMLoc, MentionWord, NEType, Animacy, Gender, Number";

	/** the topics used in the Stanford experiments
	 * "12", "22", "38",                                                        // development topics 
	 * */
	//	public static final String[] stanfordTotalTopics = {"5", "6", "8", "11", "12", "16", "22", "25",  "37", "38", "40", "44",     // training topics
	//														"1", "2", "3", "4", "7", "9", "10", "13", "14", "18", "19", "20",        // testing topics
	//														"21", "23", "24", "26", "27", "28", "29", "32", "33", "34", "35", 
	//														"36", "39", "41", "42", "45"};

	public static final String[] stanfordTotalTopics = {"5", "6", "8", "11", "16", "25", "30", "31", "37", "40", "43", "44",     // training topics
		"1", "2", "4", "7", "9", "10", "13", "14", "18", "19", "20",        // testing topics
		"21", "22", "23", "24", "26", "27", "28", "29", "32", "33", "34", "35", 
		"36", "39", "41", "42", "45"};

	/** development topics */
	//public static final String[] stanfordDevelopmentTopics = {"30", "31", "43"};
	public static final String[] stanfordDevelopmentTopics = {"3", "12", "38"}; //,

	/** the total topics */
	public static final String[] totalTopics = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", 
		"11", "12", "13", "14", "16", "18", "19", "20", "21", "22", 
		"23", "24", "25", "26", "27", "28", "29", "30", "31", "32", 
		"33", "34", "35", "36", "37", "38", "39", "40", "41", "42", 
		"43", "44", "45"};

	/** debug topics */

	public static final String[] debugTopics = {"6", "16", "10", "20"};
	//public static final String[] debugTopics = {"12", "22", "38"};
	//	public static final String[] debugTopics = {"5", "6", "8", "11", "16", "25", "30", "31", "37", "40", "43", "44",     // training topics
	//		"1", "2", "3", "4", "7", "9", "10", "13", "14", "18", "19", "20",        // testing topics
	//		"21", "23", "24", "26", "27", "28", "29", "32", "33", "34", "35", 
	//		"36", "39", "41", "42", "45"};

	/** debug development topics */
	public static final String[] debugDevelopmentTopics = {"3", "12", "38"}; //

	/** score Types */
	public static final String[] scoreTypes = {"Pairwise", "MUC", "Bcubed", "CEAF"};

	// traditional features
	public static String[] featuresName = {"Head-PROPER", "Head-NOMINAL", "Lemma", "Synonym", "Synonym-PROPER",
		"Synonym-NOMINAL", "SrlAgreeCount", "SrlAgreeCount-PROPER", "SrlAgreeCount-NOMINAL", "SrlA0", 
		"SrlA0-PROPER", "SrlA0-NOMINAL", "SrlA1", "SrlA1-PROPER", "SrlA1-NOMINAL", 
		"SrlA2", "SrlA2-PROPER", "SrlA2-NOMINAL", "SrlAM-LOC", "SrlAM-LOC-PROPER", 
		"SrlAM-LOC-NOMINAL", "SrlPA0-PROPER", "SrlPA0-NOMINAL", "SrlPA1-PROPER", "SrlPA1-NOMINAL",
		"SrlPA2-PROPER", "SrlPA2-NOMINAL", "SrlPAM-LOC-PROPER", "SrlPAM-LOC-NOMINAL", "MentionWord-PROPER",
		"MentionWord-NOMINAL", "NEType-PROPER", "Number-NOMINAL", "Animacy-PROPER", "Animacy-NOMINAL", 
		"Gender-PROPER", "Gender-NOMINAL", "Number-PROPER", "NEType-NOMINAL"};

	/** introduce the HALT feature */
	public static String[] extendFeaturesName = {"HEAD-PROPER", "HEAD-NOMINAL", "LEMMA", "SYNONYM", "SYNONYM-PROPER",
		"SYNONYM-NOMINAL", "SRLAGREECOUNT", "SRLAGREECOUNT-PROPER", "SRLAGREECOUNT-NOMINAL", "SRLROLES-A0", 
		"SRLROLES-A0-PROPER", "SRLROLES-A0-NOMINAL", "SRLROLES-A1", "SRLROLES-A1-PROPER", "SRLROLES-A1-NOMINAL", 
		"SRLROLES-A2", "SRLROLES-A2-PROPER", "SRLROLES-A2-NOMINAL", "SRLROLES-AM-LOC", "SRLROLES-AM-LOC-PROPER", 
		"SRLROLES-AM-LOC-NOMINAL", "SRLPRED-A0-PROPER", "SRLPRED-A0-NOMINAL", "SRLPRED-A1-PROPER", "SRLPRED-A1-NOMINAL",
		"SRLPRED-A2-PROPER", "SRLPRED-A2-NOMINAL", "SRLPRED-AM-LOC-PROPER", "SRLPRED-AM-LOC-NOMINAL", "MENTION_WORDS-PROPER",
		"MENTION_WORDS-NOMINAL", "NETYPE-PROPER", "NUMBER-NOMINAL", "ANIMACY-PROPER", "ANIMACY-NOMINAL", 
		"GENDER-PROPER", "GENDER-NOMINAL", "NUMBER-PROPER", "NETYPE-NOMINAL", "HALT"};

	// experiment name component
	// the vlaue for this property is shown as below:
	// dcoref.experiment = "lossfunction,lossscore";
	public static final String EXPERIMENT_PROP = "experiment";		// MUST

	// corpus path
	public static final String CORPUS_PROP = "corpus";		// MUST

	// CONLL scorer path  MUST
	public static final String CONLL_SCORER_PROP = "conll.scorer";		// MUST

	// whether the experiment is in the debug model or cluster model
	// used to print out the detail information, while in the real clustering
	// running, we would like to like faster by reducing the output
	public static final String DEBUG_PROP = "debug";		// MUST

	// WORDNET path
	public static final String WORDNET_PROP = "wordnet";		// MUST
	
	// whether use all sieves or all sieves except Pronoun sieve
	public static final String SIEVE_PROP = "sieve";
	
	// current phase
	public static final String PHASE_PROP = "phase";

	//
	// datageneration settings
	//
	// within (false) or cross (true) reading data
	public static final String DATAGENERATION_DATASET_PROP = "datageneration.dataset";
	// gold mention (true) or predicted mention (false)
	public static final String DATAGENERATION_GOLDMENTION_PROP = "datageneration.goldmention";
	// GOLD cluster post process
	public static final String DATAGENERATION_POSTPROCESS_GOLD_PROP = "datageneration.postprocess.gold";
	// Annotators used in the experiment
	public static final String DATAGENERATION_ANNOTATORS_PROP = "datageneration.annotators";
	// training set
	public static final String DATAGENERATION_TRAININGSET_PROP = "datageneration.trainingset";
	// testing set
	public static final String DATAGENERATION_TESTINGSET_PROP = "datageneration.testingset";
	// development set
	public static final String DATAGENERATION_DEVELOPMENTSET_PROP = "datageneration.developmentset";
	// stanford preprocessing
	public static final String DATAGENERATION_STANFORD_PREPROCESSING = "datageneration.stanford.preprocessing";
	
	//
	// search settings
	//
	// search type
	public static final String SEARCH_TYPE = "search.type";
	// search method
	public static final String SEARCH_METHOD = "search.method";
	public static final String SEARCH_METHOD_BEAMWIDTH = "search.method.beamwidth";
	public static final String SEARCH_METHOD_MAXIMUMSTEP = "search.method.maximumstep";
	// stopping criterion (if tune, then its stopping rate)
	public static final String SEARCH_STOPPINGCRITERION = "search.stoppingcriterion";
	public static final String SEARCH_STOPPINGRATE = "search.stoppingrate";
	// best state score 
	public static final String SEARCH_BESTSTATE = "search.beststate";		// MUST
	// average weight or latest weight
	public static final String SEARCH_WEIGHT = "search.weight";
	// constraints enable
	public static final String SEARCH_ENABLEPREVIOUSCCURRENTCCONSTRAINT_PROP = "search.enablepreviouscurrentconstraint";
	public static final String SEARCH_ENABLEBEAMCONSTRAINT_PROP = "search.enablebeamconstraint";
	public static final String SEARCH_ENABLEBEAMUNBEAMCONSTRAINT_PROP = "search.enablebeamunbeamconstraint";

	// use existed weight to do testing, whether do validation or do final testing
	public static final String EXISTEDWEIGHT_PROP = "dcoref.existedweight";
	// existed weight path
	public static final String EXISTEDWEIGHT_PATH_PROP = "dcoref.existedweight.path";

	//
	// classifier setting
	//
	public static final String CLASSIFIER_METHOD = "classifier";
	public static final String CLASSIFIER_EPOCH_PROP = "classifier.epoch";
	public static final String CLASSIFIER_ITERATION_RESULT = "classifier.iteration.result";
	public static final String CLASSIFIER_ITEARTION_GAP = "classifier.iteration.gap";
	// use which training method to train the algorithm, Online, OnlineToBatch, Batch
	public static final String CLASSIFIER_TRAINING_METHOD = "classifier.training.method";
	public static final String CLASSIFIER_TRAINING_NORMALIZE_WEIGHT = "classifier.training.normalize.weight";
	public static final String CLASSIFIER_TRAINING_INCORPORATE_ZERO_CASE = "classifier.training.incorporate.zero.case";
	// Perceptron started rate
	public static final String CLASSIFIER_TRAINING_PERCEPTRON_STARTRATE = "classifier.training.perceptron.startrate";
	public static final String CLASSIFIER_TRAINING_PERCEPTRON_LEARINGRATE_CONSTANT = "classifier.training.perceptron.learningrate.constant";
	// enable PA learning and its margin type
	public static final String CLASSIFIER_TRAINING_PA_RATE_LOSSSCORE = "classifier.training.pa.rate.lossscore";
	public static final String CLASSIFIER_TRAINING_PA_DISCREPANCY = "classifier.training.pa.discrepancy";
	public static final String CLASSIFIER_TRAINING_PA_MARGIN = "classifier.training.pa.margin";
	// experiment hyperparameter
	public static final String CLASSIFIER_TRAINING_HYPERPARAMETER = "classifier.training.hyperparameter";
	
	
	//
	// cost function setting
	//
	// cost function used, for example, linear 
	public static final String COSTFUNCTION_METHOD = "costfunction";

	//
	// Lost function setting
	//
	// loss function used score type
	public static final String LOSSFUNCTION_METHOD = "lossfunction"; 
	public static final String LOSSFUNCTION_SCORE_PROP = "lossfunction.score";
	
	//
	// feature setting
	//
	// state feature
	public static final String FEATURE_STATE = "feature.state";
	// Atomic features
	public static final String FEATURE_ATOMIC_NAMES = "feature.atomic.names";

	
	

	// Method configuration
	public static final String METHOD_PROP = "dcoref.method";
	public static final String METHOD_EPOCH_PROP = "dcoref.method.epoch";
	public static final String METHOD_FUNCTION_NUMBER_PROP = "dcoref.method.function.number";

	// use binary to write and read
	public static final String IO_BINARY_PROP = "dcoref.io.binary";

}