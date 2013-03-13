package edu.oregonstate.method;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.classifier.IClassifier;
import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.dataset.CorefSystem;
import edu.oregonstate.dataset.TopicGeneration;
import edu.oregonstate.experiment.ExperimentConfigurationFactory;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;

/**
 * use Dagger method to train the whole experiment
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Dagger implements IMethod {

	/* experiment configuration */
	private final Properties mProps;

	/* number of member functions */
	private final int numberOfFunctions;

	/* training topics */
	private final String[] trainingTopics;

	/* testing topics */
	private final String[] testingTopics;

	/* search method */
	private final String searchMethod;

	/* serialized output */
	private final String serializeOutput;

	/* classification method */
	private final String classificationMethod;

	/* conll result folder */
	private final String conllResultPath;

	/* log File */
	private final String logFile;

	/* experiment result */
	private final String experimentResultFolder;

	/* loss type */
	private final ScoreType lossType;

	public Dagger() {
		mProps = ExperimentConstructor.experimentProps;
		experimentResultFolder = ExperimentConstructor.resultPath;
		logFile = ExperimentConstructor.logFile;
		numberOfFunctions = Integer.parseInt(mProps.getProperty(EecbConstants.METHOD_FUNCTION_NUMBER_PROP, "3"));
		
		TopicGeneration topicGenerator = new TopicGeneration(mProps);
		trainingTopics = topicGenerator.trainingTopics();
		testingTopics = topicGenerator.testingTopics();
		
		searchMethod = mProps.getProperty(EecbConstants.SEARCH_METHOD, "BeamSearch");
		serializeOutput = experimentResultFolder + "/document";
		classificationMethod = mProps.getProperty(EecbConstants.CLASSIFIER_METHOD, "StructuredPerceptron");
		conllResultPath = experimentResultFolder + "/conll";
		lossType = ScoreType.valueOf(mProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise"));
	}

	/**
	 * Learn the final weight, which can be used for 
	 */
	public List<Parameter> executeMethod() {
		int length = FeatureFactory.getFeatureTemplate().size();
		List<Parameter> paras = new ArrayList<Parameter>();
		double[] weight = new double[length];
		double[][] variance = DoubleOperation.generateIdentityMatrix(length);
		Parameter para = new Parameter(weight, variance);

		// 0: the true loss function
		// 1 - numberOfFunctions : the learned function
		for (int j = 0; j <= numberOfFunctions; j++) {
			// training
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Training Model : " + j + "\n\n");
			ResultOutput.printParameter(para, logFile);
			para = trainModel(para, j);

			//testing
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Testing Model : " + j + "\n\n");
			ResultOutput.printParameter(para, logFile);
			testModel(para.generateWeightForTesting(), j);

			// add returned parameter to the final parameters
			paras.add(para.makeCopy());
		}

		assert paras.size() == numberOfFunctions;
		return paras;
	}

	/**
	 * train the model
	 * 
	 * @param para
	 * @param j
	 * @param i
	 * @param phaseID
	 * @return
	 */
	private Parameter trainModel(Parameter para, int j) {
		ISearch search = EecbConstructor.createSearchMethod(searchMethod);
		String phase = "training-" + j;
		boolean postProcess = ExperimentConstructor.postProcess;
		// generate training data for classification
		if (j == 0) {
			Document corpus = new Document();
			corpus.goldCorefClusters = new HashMap<Integer, CorefCluster>();

			String goldCorefCluster = conllResultPath + "/goldCorefCluster-training" + "-"+ j;
			String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-training" + "-" + j;

			for (String topic : trainingTopics) {
				ResultOutput.writeTextFile(logFile, "\n(Dagger) Training Model : " + j + "; Document : " + topic + "\n");
				Document document = ResultOutput.deserialize(topic, serializeOutput, false);

				// create training data directory
				String trainingDataPath = experimentResultFolder + "/" + document.getID() + "/data";
				Command.mkdir(trainingDataPath);

				// conduct search using the true loss function
				search.trainingBySearch(document, para, phase);
				DocumentAlignment.alignDocument(document);
			
				// apply the pronoun sieve
				CorefSystem cs = new CorefSystem();
				cs.applyPronounSieve(document);

				// whether post-process the document
				if (postProcess) {
					DocumentAlignment.postProcessDocument(document);
				}
				
				ResultOutput.printDocumentScore(document, lossType, logFile, "single training" + " document " + topic);
				ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster);
				DocumentAlignment.mergeDocument(document, corpus);
			}

			// Stanford scoring
			String[] scoreInformation = ResultOutput.printDocumentScore(corpus, lossType, logFile, "training-with-true-loss-function");

			// CoNLL scoring
			double[] finalScores = ResultOutput.printCorpusResult(logFile, goldCorefCluster, predictedCorefCluster, "model generation");
			ResultOutput.writeTextFile(experimentResultFolder + "/trainingset.csv", scoreInformation[0] + "\t" + finalScores[0] + "\t" + finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);

		} else {

			// use average model to collect more data
			CoreferenceResolutionDecoding decoder = new CoreferenceResolutionDecoding(phase, trainingTopics, true, 0.0);
			decoder.decode(para.generateWeightForTesting());
		}

		// train the model using the specified classifier for several iterations, using small learning rate
		IClassifier classifier = EecbConstructor.createClassifier(classificationMethod);
		List<String> filePaths = getPaths();
		ResultOutput.writeTextFile(experimentResultFolder + "/searchstep", "" + filePaths.size());
		ResultOutput.writeTextFile(logFile, "the total number of training files : " + filePaths.size());
		Parameter returnPara = classifier.train(filePaths, j);
		return returnPara.makeCopy();
	}

	/**
	 * get the path of training data
	 * 
	 * @param j
	 * @return
	 */
	private List<String> getPaths() {
		List<String> filePaths = new ArrayList<String>();
		filePaths.addAll(getPaths(trainingTopics));

		return filePaths;
	}
	
	/**
	 * aggregate the training data
	 * 
	 * @param topics
	 * @param j
	 * @return
	 */
	private List<String> getPaths(String[] topics) {
		List<String> allfiles  = new ArrayList<String>();
		for (String topic : topics) {
			List<String> files = getDivisionPaths(topic);
			String topicPath = experimentResultFolder + "/" + topic + "/data/";
			List<String> filePaths = new ArrayList<String>();
			for (String file : files) {
				filePaths.add(topicPath + file);
			}

			allfiles.addAll(filePaths);
		}

		return allfiles;
	}

	// get a sequence of data file, such as 1, 2, 3, 4, 5
	private List<String> getDivisionPaths(String topic) {
		String topicPath = experimentResultFolder + "/" + topic + "/data/";
		List<String> files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));

		return files;
	}

	/**
	 * test the model
	 * 
	 * @param para
	 * @param j
	 * @param i
	 */
	private void testModel(double[] weight, int j) {
		// set stopping rate for tuning
		double stoppingrate = ExperimentConfigurationFactory.tuneStoppingRate(weight, j);
		String phase = "testing-" + j;
		// do testing on training set
		//boolean testTraining = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAINING_VALIDATION_PROP, "false"));
//		if (testTraining && (j == numberOfFunctions)) {
//			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) testing on training set\n\n");
//			
//			CoreferenceResolutionDecoding decoder = new CoreferenceResolutionDecoding(phase, trainingTopics, false, stoppingrate);
//			decoder.decode(weight);
//		}

		// do testing on testing set
		ResultOutput.writeTextFile(logFile, "\n\n(Dagger) testing on testing set\n\n");
		CoreferenceResolutionDecoding decoder = new CoreferenceResolutionDecoding(phase, testingTopics, false, stoppingrate);
		decoder.decode(weight);
	}
	
}
