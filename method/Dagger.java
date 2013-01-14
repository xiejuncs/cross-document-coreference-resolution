package edu.oregonstate.method;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.classifier.IClassifier;
import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.dataset.CorefSystem;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.CoNLLScorerHelper;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.search.State;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;

/**
 * use Dagger method to train the whole experiment
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Dagger implements IMethod {

	/* experiment configuration */
	private final Properties mProps;
	
	/* method epoch */
	private final int methodEpoch;
	
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
		experimentResultFolder = ExperimentConstructor.experimentResultFolder;
		methodEpoch = Integer.parseInt(mProps.getProperty(EecbConstants.METHOD_EPOCH_PROP, "1"));
		logFile = ExperimentConstructor.logFile;
		numberOfFunctions = Integer.parseInt(mProps.getProperty(EecbConstants.METHOD_FUNCTION_NUMBER_PROP, "3"));
		trainingTopics = ExperimentConstructor.trainingTopics;
		testingTopics = ExperimentConstructor.testingTopics;
		searchMethod = mProps.getProperty(EecbConstants.SEARCH_PROP, "BeamSearch");
		serializeOutput = experimentResultFolder + "/documentobject";
		classificationMethod = mProps.getProperty(EecbConstants.CLASSIFIER_PROP, "StructuredPerceptron");
		conllResultPath = experimentResultFolder + "/conllResult";
		lossType = ScoreType.valueOf(mProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise"));
	}
	
	/**
	 * Learn the final weight
	 */
	public List<Parameter> executeMethod() {
		int length = FeatureFactory.getFeatures().length;
		List<Parameter> paras = new ArrayList<Parameter>();
		double[] weight = new double[length];
		double[] totalWeight = new double[length];
		int violation = 0;
		Parameter para = new Parameter(weight, totalWeight, violation);
		
		// in the current time, just 1 epoch
		for (int i = 1; i <= methodEpoch; i++) {
			// 0: the true loss function
			// 1- numberOfFunctions : the learned function
			for (int j = 0; j <= numberOfFunctions; j++) {
				// training
				ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Training Iteration Epoch : " + i + "; Training Model : " + j + "\n\n");
				ResultOutput.printParameter(para, logFile);
				para = trainModel(para, i, j);
				
				//testing
				ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Testing Iteration Epoch : " + i + "; Testing Model : " + j + "\n\n");
				ResultOutput.printParameter(para, logFile);
				testModel(generateWeightForTesting(para), i, j);
				
				// add returned parameter to the final parameters
				if (j != 0) {
					paras.add(para.makeCopy());
				}
				
			}
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
	private Parameter trainModel(Parameter para, int i, int j) {
		ISearch search = EecbConstructor.createSearchMethod(searchMethod);
		String phaseID = "training-" + i + "-" + j;
		
		// generate training data for classification
		for (String topic : trainingTopics) {
			ResultOutput.writeTextFile(logFile, "\n(Dagger) Training Iteration Epoch : " + i + "; Training Model : " + j + "; Document : " + topic + "\n");
			Document document = ResultOutput.deserialize(topic, serializeOutput, false);
			
			// j == 0 true loss function else the learned model
			if (j == 0) {
				// create training data directory
				String trainingDataPath = experimentResultFolder + "/" + document.getID() + "/data";
				Command.createDirectory(trainingDataPath);
				
				search.trainingBySearch(document, para, phaseID);
			} else {
				// stop at cluster no at 1, because there are pronoun case, the number of candidate sets is 0
				search.testingBySearch(document, generateWeightForTesting(para), phaseID, true);
			}
			
			// print its result to check with the final search result do by search
			ResultOutput.printDocumentScore(document, lossType, logFile, "single training document " + topic);
		}
		
		// train the model using the specified classifier for several iterations, using small learning rate
		IClassifier classifier = EecbConstructor.createClassifier(classificationMethod);
		List<String> filePaths = getPaths();
		ResultOutput.writeTextFile(experimentResultFolder + "/searchstep", "" + filePaths.size());
		ResultOutput.writeTextFile(logFile, "the total number of training files : " + filePaths.size());
		Parameter returnPara = classifier.train(filePaths);
		return returnPara.makeCopy();
	}
	
	/**
	 * get the path of training data
	 * 
	 * @param i
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
	 * @param i
	 * @param j
	 * @return
	 */
	private List<String> getPaths(String[] topics) {
		List<String> allfiles  = new ArrayList<String>();
		for (String topic : topics) {
			String topicPath = experimentResultFolder + "/" + topic + "/data/";
			List<String> files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
			List<String> filePaths = new ArrayList<String>();
			for (String file : files) {
				filePaths.add(topicPath + file);
			}
			
			ResultOutput.writeTextFile(experimentResultFolder + "/searchstep", "" + filePaths.size());
			allfiles.addAll(filePaths);
		}
		return allfiles;
	}
	
	/**
	 * test the model
	 * 
	 * @param para
	 * @param j
	 * @param i
	 */
	private void testModel(double[] weight, int i, int j) {
		boolean testTraining = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAINING_VALIDATION_PROP, "false"));
		
		if (testTraining) {
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) testing on training set\n\n");
			boolean trainPostProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
			testDocument(trainingTopics, weight, i, j, trainPostProcess, "training");
		}
		
		ResultOutput.writeTextFile(logFile, "\n\n(Dagger) testing on testing set\n\n");
		boolean testPostProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
		testDocument(testingTopics, weight, i, j, testPostProcess, "testing");
	}
	
	/**
	 * do testing on the topics
	 * 
	 * @param topics
	 * @param weight
	 * @param i
	 * @param j
	 * @param postProcess
	 * @param phase
	 */
	private void testDocument(String[] topics, double[] weight, int i, int j, boolean postProcess, String phase){
		// store the predicted mentions and gold mentions into corpus
		Document corpus = new Document();
		corpus.goldCorefClusters = new HashMap<Integer, CorefCluster>();
		
		// conll scoring files
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + i + "-" + j;
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + i + "-" + j;
		PrintWriter writerPredicted = null;
		PrintWriter writerGold = null;
		try {
			writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
			writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		String phaseID = "testing-" + i + "-" + j;
		for(String topic : topics) {
			
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Testing Iteration Epoch : " + phaseID + "; Document :" + topic + "\n\n");
		
			Document document = ResultOutput.deserialize(topic, serializeOutput, false);
			ISearch search = EecbConstructor.createSearchMethod(searchMethod);
			State<CorefCluster> bestLossState = search.testingBySearch(document, weight, phaseID, false);
			
			document.corefClusters = bestLossState.getState();
			DocumentAlignment.updateOrderedPredictedMentions(document);
			
			ResultOutput.printDocumentScore(document, lossType, logFile, "single " + phase + " document " + topic);
			
			try {
				DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
				CorefSystem cs = new CorefSystem();
				cs.getCorefSystem().coreference(document, pronounSieve);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			// whether post-process the document
			if (postProcess) {
				SieveCoreferenceSystem.postProcessing(document);
			}
			
			// add single document to the corpus
			DocumentAlignment.mergeDocument(document, corpus);
			
			SieveCoreferenceSystem.printConllOutput(document, writerPredicted, false, postProcess);
            SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
		}
		
		// Stanford scoring
		List<String[]> scoreInformation = ResultOutput.printDocumentScore(corpus, logFile, phase);
		String[] pairwiseScoreInformation = scoreInformation.get(0);
		
		// CoNLL scoring
		CoNLLScorerHelper conllScorerHelper = new CoNLLScorerHelper(i * 10 + j, logFile);
		conllScorerHelper.printFinalCoNLLScore(goldCorefCluster, predictedCorefCluster, phase);
		double predictedCoNLL = conllScorerHelper.getFinalCoNllF1Result();
		double mucPredictedScore = conllScorerHelper.getMUCScoreF1Result();
		double bcubedPredictedScore = conllScorerHelper.getBcubedScoreF1Result();
		double ceafPredictedScore = conllScorerHelper.getCEAFScoreF1Result();
		
		ResultOutput.writeTextFile(experimentResultFolder + "/" + phase + ".csv", pairwiseScoreInformation[0] + "\t" 
									+ mucPredictedScore + "\t" + bcubedPredictedScore + "\t" + ceafPredictedScore + "\t" + predictedCoNLL);
	}
	
	/**
	 * generate weight for testing, average weight or latest weight
	 * 
	 * @param para
	 * @return
	 */
	public double[] generateWeightForTesting(Parameter para) {
		boolean averageWeight = Boolean.parseBoolean(mProps.getProperty(EecbConstants.WEIGHT_PROP, "true"));
		Parameter finalPara = para.makeCopy();
		double[] learnedWeight;
		if (averageWeight) {
			learnedWeight = DoubleOperation.divide(finalPara.getTotalWeight(), finalPara.getNoOfViolation());
		} else {
			learnedWeight = finalPara.getWeight();
		}
		return learnedWeight;
	}
	
}
