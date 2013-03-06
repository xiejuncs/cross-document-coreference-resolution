package edu.oregonstate.experiment;

import java.lang.reflect.Field;

public class ExperimentArguments {

	public String[] EXPERIMENT_PROP = {"debug, method.function.number, feature.atomic.names"};		// MUST

	// corpus path
	public String[] CORPUS_PROP = {"/nfs/guille/xfern/users/xie/Experiment/corpus"};		// MUST

	// CONLL scorer path  MUST
	public String[] CONLL_SCORER_PROP = {"/nfs/guille/xfern/users/xie/Experiment/corpus/scorer/v4/scorer.pl"};		// MUST

	// whether the experiment is in the debug model or cluster model
	// used to print out the detail information, while in the real clustering
	// running, we would like to like faster by reducing the output
	public String[] DEBUG_PROP = {"false"};		// MUST

	// annotators used in the experiment
	public String[] ANNOTATORS_PROP = {"tokenize, ssplit, pos, lemma, ner, parse, dcoref"};		// MUST

	// gold mention (true) or predicted mention (false)
	public String[] GOLDMENTION_PROP = {"true", "false"};		// MUST
	
	// GOLD cluster post process
	public String[] POSTPROCESS_GOLD_PROP = {"false"};

	// within (false) or cross (true) reading data
	public String[] DATASET_PROP = {"true"};
	// training set
	public String[] TRAININGSET_PROP = {"5, 6, 8, 11, 16, 25, 30, 31, 37, 40, 43, 44"};		// MUST
	// testing set
	public String[] TESTINGSET_PROP = {"1, 2, 4, 7, 9, 10, 13, 14, 18, 19, 20, 21, 22, 23, 24, 26, 27, 28, 29, 32, 33, 34, 35, 36, 39, 41, 42, 45"};		// MUST
	
	// WORDNET path
	public String[] WORDNET_PROP = {"/nfs/guille/xfern/users/xie/Experiment/corpus/WordNet-3.0/dict"};		// MUST

	// best state score 
	public String[] BEST_STATE_PROP = {"true"};		// MUST

	// whether use all sieves or all sieves except Pronoun sieve
	public String[] SIEVE_PROP = {"partial"};

	// do training to learn a weight
	public String[] DOTRAINING_PROP = {"true"};

	// use existed weight to do testing, whether do validation or do final testing
	public String[] EXISTEDWEIGHT_PROP = {"false"};

	// classifier
	public String[] CLASSIFIER_PROP = {"StructuredPerceptron"};
	public String[] CLASSIFIER_EPOCH_PROP = {"10"};

	// cost function used, for example, linear 
	public String[] COSTFUNCTION_PROP = {"LinearCostFunction"};

	// loss function used score type
	public String[] LOSSFUNCTION_PROP = {"MetricLossFunction"}; 
	public String[] LOSSFUNCTION_SCORE_PROP = {"Pairwise"};

	// search, its beam width, maximum step
	public String[] SEARCH_PROP = {"BeamSearch"};
	public String[] SEARCH_BEAMWIDTH_PROP = {"1"};
	public String[] SEARCH_MAXIMUMSTEP_PROP = {"600"};

	// stopping criterion (if tune, then its stopping rate)
	public String[] STOPPING_CRITERION = {"none"};

	// whether print the testing performance on training set
	public String[] TRAINING_VALIDATION_PROP = {"true"};

	// average weight or latest weight
	public String[] WEIGHT_PROP = {"true"};

	// Method configuration
	public String[] METHOD_PROP = {"Dagger"};
	public String[] METHOD_FUNCTION_NUMBER_PROP = {"1", "3", "5"};

	// use which training method to train the algorithm, Online, OnlineToBatch, Batch
	public String[] TRAINING_STYLE_PROP = {"AROWOnline"};
	public String[] TRAINING_NORMALIZE_WEIGHT = {"false"};

	// stanford preprocessing
	public String[] STANFORD_PREPROCESSING = {"true"};

	// state feature
	public String[] STATE_FEATURE = {"false"};

	// Atomic features
	public String[] FEATURE_ATOMIC_NAMES = {"F", "N"};

	public static void main(String[] args) {
		ExperimentArguments generator = new ExperimentArguments();
		Class generatorClass = generator.getClass();

		Field[] fields = generatorClass.getFields();

		for (Field field : fields) {
			try {
				System.out.println(field.getName() + "--->" + field.get(generator));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

	}

}