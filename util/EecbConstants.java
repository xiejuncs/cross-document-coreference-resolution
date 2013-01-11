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
	
	/** String constants */
	/** local corpus path, which is used for debug */
	public static final String LOCAL_CORPUS_PATH = "../";
	
	/** cluster corpus path, which is used to run the whole experiment */
	public static final String CLUSTER_CPRPUS_PATH = "/nfs/guille/xfern/users/xie/Experiment/";
	
	/** partial sieves, does not including pronoun sieve, which is the right configuration for Stanford experiment and our own experiment */
	public static final String PARTIAL_SIEVE_STRING = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch";

	/** full sieve, including Pronoun Sieve */
	public static final String FULL_SIEVE_STRING = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch, PronounMatch";
	
	/** the topics used in the Stanford experiments 
	 * "12", "22", "38",                                                        // development topics 
	 * */
	public static final String[] stanfordTotalTopics = {"5", "6", "8", "11", "16", "25", "30", "31", "37", "40", "43", "44",     // training topics
														"1", "2", "3", "4", "7", "9", "10", "13", "14", "18", "19", "20",        // testing topics
														"21", "23", "24", "26", "27", "28", "29", "32", "33", "34", "35", 
														"36", "39", "41", "42", "45"};
	
	/** development topics */
	public static final String[] stanfordDevelopmentTopics = {"12", "22", "38"};
	
	/** the total topics */
	public static final String[] totalTopics = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", 
											 "11", "12", "13", "14", "16", "18", "19", "20", "21", "22", 
											 "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", 
											 "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", 
											 "43", "44", "45"};
	
	/** debug topics */
	public static final String[] debugTopics = {"16", "10", "38", "20"};
	
	/** debug development topics */
	public static final String[] debugDevelopmentTopics = {"6"};
	
	/** score Types */
	public static final String[] scoreTypes = {"Pairwise", "MUC", "Bcubed", "CEAF"};
	
	public static String[] featuresName = {"HEAD-PROPER", "HEAD-NOMINAL", "LEMMA", "SYNONYM", "SYNONYM-PROPER",
		"SYNONYM-NOMINAL", "SRLAGREECOUNT", "SRLAGREECOUNT-PROPER", "SRLAGREECOUNT-NOMINAL", "SRLROLES-A0", 
		"SRLROLES-A0-PROPER", "SRLROLES-A0-NOMINAL", "SRLROLES-A1", "SRLROLES-A1-PROPER", "SRLROLES-A1-NOMINAL", 
		"SRLROLES-A2", "SRLROLES-A2-PROPER", "SRLROLES-A2-NOMINAL", "SRLROLES-AM-LOC", "SRLROLES-AM-LOC-PROPER", 
		"SRLROLES-AM-LOC-NOMINAL", "SRLPRED-A0-PROPER", "SRLPRED-A0-NOMINAL", "SRLPRED-A1-PROPER", "SRLPRED-A1-NOMINAL",
		"SRLPRED-A2-PROPER", "SRLPRED-A2-NOMINAL", "SRLPRED-AM-LOC-PROPER", "SRLPRED-AM-LOC-NOMINAL", "MENTION_WORDS-PROPER",
		"MENTION_WORDS-NOMINAL", "NETYPE-PROPER", "NUMBER-NOMINAL", "ANIMACY-PROPER", "ANIMACY-NOMINAL", 
		"GENDER-PROPER", "GENDER-NOMINAL", "NUMBER-PROPER", "NETYPE-NOMINAL"};
	
	/** introduce the HALT feature */
	public static String[] extendFeaturesName = {"HEAD-PROPER", "HEAD-NOMINAL", "LEMMA", "SYNONYM", "SYNONYM-PROPER",
		"SYNONYM-NOMINAL", "SRLAGREECOUNT", "SRLAGREECOUNT-PROPER", "SRLAGREECOUNT-NOMINAL", "SRLROLES-A0", 
		"SRLROLES-A0-PROPER", "SRLROLES-A0-NOMINAL", "SRLROLES-A1", "SRLROLES-A1-PROPER", "SRLROLES-A1-NOMINAL", 
		"SRLROLES-A2", "SRLROLES-A2-PROPER", "SRLROLES-A2-NOMINAL", "SRLROLES-AM-LOC", "SRLROLES-AM-LOC-PROPER", 
		"SRLROLES-AM-LOC-NOMINAL", "SRLPRED-A0-PROPER", "SRLPRED-A0-NOMINAL", "SRLPRED-A1-PROPER", "SRLPRED-A1-NOMINAL",
		"SRLPRED-A2-PROPER", "SRLPRED-A2-NOMINAL", "SRLPRED-AM-LOC-PROPER", "SRLPRED-AM-LOC-NOMINAL", "MENTION_WORDS-PROPER",
		"MENTION_WORDS-NOMINAL", "NETYPE-PROPER", "NUMBER-NOMINAL", "ANIMACY-PROPER", "ANIMACY-NOMINAL", 
		"GENDER-PROPER", "GENDER-NOMINAL", "NUMBER-PROPER", "NETYPE-NOMINAL", "HALT"};
	
	/** configuration setting */
	public static final String DATASET_PROP = "dcoref.dataset";
	public static final String DATASET_GENERATION_PROP = "dcoref.dataset.generation";
	public static final String CLASSIFIER_PROP = "dcoref.classifier";
	public static final String CLASSIFIER_EPOCH_PROP = "dcoref.classifier.epoch";
	public static final String CLASSIFIER_OPTIONS_PROP = "dcoref.classifier.options";
	public static final String CLASSIFIER_OUTPUTFEATURE_PROP = "dcoref.classifier.outputfeature";
	public static final String COSTFUNCTION_PROP = "dcoref.costfunction";
	public static final String LOSSFUNCTION_PROP = "dcoref.lossfunction";
	public static final String CLUSTERING_PROP = "dcoref.clustering";
	public static final String SEARCH_PROP = "dcoref.search";
	public static final String SEARCH_BEAMWIDTH_PROP = "dcoref.search.beamwidth";
	public static final String SEARCH_MAXIMUMSTEP_PROP = "dcoref.search.maximumstep";
	public static final String SRL_PROP = "dcoref.srl";
	public static final String STOPWORD_PROP = "dcoref.stopword";
	public static final String SIEVES_PROP = "dcoref.sievePasses";
	public static final String SCORE_PROP = "dcoref.score";
	public static final String TRAIN_GOLD_PROP = "dcoref.train.gold";
	public static final String TEST_GOLD_PROP = "dcoref.test.gold";
	public static final String DEBUG_PROP = "dcoref.debug";
	public static final String STOPPING_CRITERION = "dcoref.stoppingcriterion";
	public static final String STOPPING_RATE = "dcoref.stoppingrate";
	public static final String TRAINING_VALIDATION_PROP = "dcoref.training.testing";
	public static final String ANNOTATORS_PROP = "dcoref.annotators";
	public static final String FILTERSINGLETONS_PROP = "dcoref.filtersingletons";
	public static final String TRAIN_POSTPROCESS_PROP = "dcoref.train.postprocess";
	public static final String TEST_POSTPROCESS_PROP = "dcoref.test.postprocess";
	public static final String WEIGHT_PROP = "dcoref.weight";
	public static final String LOSSFUNCTION_SCORE_PROP = "dcoref.lossfunction.score";
	public static final String TUNING_PROP = "dcoref.tuning";
	public static final String FEATURE_EXTEND_PROP = "dcoref.feature.extend";
	public static final String STOPPING_PROP = "dcoref.stopping";
	public static final String INTERVAL_PROP = "dcoref.interval";
	public static final String HALT_PATRAINING_PROP = "dcoref.halt.patraining";
	public static final String TESTING_PROP = "dcoref.testing";
	public static final String TESTING_WEIGHTPATH_PROP = "dcoref.testing.weightpath";
	public static final String ENABLEPREVIOUSCCURRENTCCONSTRAINT_PROP = "dcoref.enablepreviouscurrentconstraint";
	public static final String ENABLEBEAMCONSTRAINT_PROP = "dcoref.enablebeamconstraint";
	public static final String ENABLEBEAMUNBEAMCONSTRAINT_PROP = "dcoref.enablebeamunbeamconstraint";
	public static final String STRUCTUREDPERCEPTRON_RATE_PROP = "dcoref.structuredperceptron.rate";
	public static final String METHOD_PROP = "dcoref.method";
	public static final String METHOD_EPOCH_PROP = "dcoref.method.epoch";
	public static final String METHOD_FUNCTION_NUMBER_PROP = "dcoref.method.function.number.prop";
	
}
