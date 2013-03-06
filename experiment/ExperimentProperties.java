package edu.oregonstate.experiment;

import java.lang.reflect.Field;

public class ExperimentProperties {

	public String EXPERIMENT_PROP = "dcoref.experiment";		// MUST
	
	// corpus path
	public String CORPUS_PROP = "dcoref.corpus";		// MUST

	// CONLL scorer path  MUST
	public String CONLL_SCORER_PROP = "dcoref.conll.scorer";		// MUST

	// whether the experiment is in the debug model or cluster model
	// used to print out the detail information, while in the real clustering
	// running, we would like to like faster by reducing the output
	public String DEBUG_PROP = "dcoref.debug";		// MUST

	// annotators used in the experiment
	public String ANNOTATORS_PROP = "dcoref.annotators";		// MUST

	// gold mention (true) or predicted mention (false)
	public String GOLDMENTION_PROP = "dcoref.goldmention";		// MUST
	
	// GOLD cluster post process
	public String POSTPROCESS_GOLD_PROP = "dcoref.postprocess.gold";
	
	// within (false) or cross (true) reading data
	public String DATASET_PROP = "dcoref.dataset";
	// training set
	public String TRAININGSET_PROP = "dcoref.trainingset";		// MUST
	// testing set
	public String TESTINGSET_PROP = "dcoref.testingset";		// MUST
	// development set
	public String DEVELOPMENTSET_PROP = "dcoref.developmentset";		// MUST

	// WORDNET path
	public String WORDNET_PROP = "dcoref.wordnet";		// MUST

	// best state score 
	public String BEST_STATE_PROP = "dcoref.best.state";		// MUST

	// whether use all sieves or all sieves except Pronoun sieve
	public String SIEVE_PROP = "dcoref.sieve";

	// do training to learn a weight
	public String DOTRAINING_PROP = "dcoref.dotraining";

	// use existed weight to do testing, whether do validation or do final testing
	public String EXISTEDWEIGHT_PROP = "dcoref.existedweight";
	// existed weight path
	public String EXISTEDWEIGHT_PATH_PROP = "dcoref.existedweight.path";

	// classifier
	public String CLASSIFIER_PROP = "dcoref.classifier";
	public String CLASSIFIER_EPOCH_PROP = "dcoref.classifier.epoch";
	public String CLASSIFIER_ITERATION_RESULT = "dcoref.classifier.iteration.result";
	public String CLASSIFIER__ITEARTION_GAP = "dcoref.classifier.iteration.gap";

	// cost function used, for example, linear 
	public String COSTFUNCTION_PROP = "dcoref.costfunction";

	// loss function used score type
	public String LOSSFUNCTION_PROP = "dcoref.lossfunction"; 
	public String LOSSFUNCTION_SCORE_PROP = "dcoref.lossfunction.score";

	// search, its beam width, maximum step
	public String SEARCH_PROP = "dcoref.search";
	public String SEARCH_BEAMWIDTH_PROP = "dcoref.search.beamwidth";
	public String SEARCH_MAXIMUMSTEP_PROP = "dcoref.search.maximumstep";

	// stopping criterion (if tune, then its stopping rate)
	public String STOPPING_CRITERION = "dcoref.stoppingcriterion";
	public String STOPPING_RATE = "dcoref.stoppingrate";

	// whether print the testing performance on training set
	public String TRAINING_VALIDATION_PROP = "dcoref.training.testing";

	// average weight or latest weight
	public String WEIGHT_PROP = "dcoref.weight";

	// constraints enable
	public String ENABLEPREVIOUSCCURRENTCCONSTRAINT_PROP = "dcoref.enablepreviouscurrentconstraint";
	public String ENABLEBEAMCONSTRAINT_PROP = "dcoref.enablebeamconstraint";
	public String ENABLEBEAMUNBEAMCONSTRAINT_PROP = "dcoref.enablebeamunbeamconstraint";

	// Perceptron started rate
	public String STRUCTUREDPERCEPTRON_STARTRATE_PROP = "dcoref.structuredperceptron.startrate";
	public String STRUCTUREDPERCEPTRON_LEARINGRATE_CONSTANT_PROP = "dcoref.structuredperceptron.learningrate.constant";

	// Method configuration
	public String METHOD_PROP = "dcoref.method";
	public String METHOD_EPOCH_PROP = "dcoref.method.epoch";
	public String METHOD_FUNCTION_NUMBER_PROP = "dcoref.method.function.number";

	// use binary to write and read
	public String IO_BINARY_PROP = "dcoref.io.binary";

	// use which training method to train the algorithm, Online, OnlineToBatch, Batch
	public String TRAINING_STYLE_PROP = "dcoref.training.style";
	public String TRAINING_NORMALIZE_WEIGHT = "dcoref.training.normalize.weight";
	public String TRAINING_INCORPORATE_ZERO_CASE_PROP = "dcoref.training.incorporate.zero.case";

	// stanford preprocessing
	public String STANFORD_PREPROCESSING = "dcoref.stanford.preprocessing";

	// enable PA learning and its margin type
	public String PA_LEARNING = "dcoref.pa.learning";
	public String PA_LEARNING_RATE_LOSSSCORE = "dcoref.pa.learning.rate.lossscore";
	public String PA_DISCREPANCY = "dcoref.pa.discrepancy";
	public String PA_MARGIN = "dcoref.pa.margin";

	// state feature
	public String STATE_FEATURE = "dcoref.state.feature";

	// experiment hyperparameter
	public String EXPERIMENT_HYPERPARAMETER = "dcoref.experiment.hyperparameter";

	// Atomic features
	public String FEATURE_ATOMIC_NAMES = "dcoref.feature.atomic.names";
	
	public static void main(String[] args) {
		ExperimentProperties arguments = new ExperimentProperties();
		Class generatorClass = arguments.getClass();
		
		Field[] fields = generatorClass.getFields();
		
		for (Field field : fields) {
			try {
				System.out.println(field.getName() + "--->" + field.get(arguments));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
	}
	
}
