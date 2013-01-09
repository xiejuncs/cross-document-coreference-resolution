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
	
	/** configuration setting */
	public static final String DATASET_PROP = "dcoref.dataset";
	public static final String CLASSIFIER_PROP = "dcoref.classifier";
	public static final String CLASSIFIER_EPOCH_PROP = "dcoref.classifier.epoch";
	public static final String CLASSIFIER_PREVIOUSCURRENT_PROP = "dcoref.classifier.previouscurrent";
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
	
}
