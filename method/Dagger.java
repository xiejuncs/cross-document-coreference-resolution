package edu.oregonstate.method;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	/* experiment result folder */
	private final String dataPath;
	
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
	
	public Dagger() {
		mProps = ExperimentConstructor.experimentProps;
		experimentResultFolder = ExperimentConstructor.experimentResultFolder;
		methodEpoch = Integer.parseInt(mProps.getProperty(EecbConstants.METHOD_EPOCH_PROP, "1"));
		logFile = ExperimentConstructor.logFile;
		dataPath = experimentResultFolder + "/datas";
		Command.createDirectory(dataPath);
		numberOfFunctions = Integer.parseInt(mProps.getProperty(EecbConstants.METHOD_FUNCTION_NUMBER_PROP, "3"));
		trainingTopics = ExperimentConstructor.trainingTopics;
		testingTopics = ExperimentConstructor.testingTopics;
		searchMethod = mProps.getProperty(EecbConstants.SEARCH_PROP, "BeamSearch");
		serializeOutput = experimentResultFolder + "/documentobject";
		classificationMethod = mProps.getProperty(EecbConstants.CLASSIFIER_PROP, "StructuredPerceptron");
		conllResultPath = experimentResultFolder + "/conllResult";
	}
	
	/**
	 * Learn the final weight
	 */
	public double[] executeMethod() {
		int length = FeatureFactory.getFeatures().length;
		double[] returnweight = new double[length];
		
		for (int i = 0; i < methodEpoch; i++) {
			double[] weight = new double[length];
			double[] totalWeight = new double[length];
			int violation = 0;
			Parameter para = new Parameter(weight, totalWeight, violation);
			
			// training
			for (int j = 0; j < numberOfFunctions; j++) {
				String phaseID = i + "-" + j;
				ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Training Iteration Epoch : " + i + "; Training Model : " + j + "\n\n");
				ResultOutput.printParameter(para, logFile);
				para = trainModel(para, i, j, phaseID);
				
				ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Testing Iteration Epoch : " + i + "; Testing Model : " + j + "\n\n");
				testModel(generateWeightForTesting(para), i, j);
			}
			
			if (i == methodEpoch - 1) {
				returnweight = generateWeightForTesting(para);
			}
		}
		
		return returnweight;
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
	private Parameter trainModel(Parameter para, int i, int j, String phaseID) {
		ISearch search = EecbConstructor.createSearchMethod(searchMethod);
		Parameter returnPara = para.makeCopy();
		for (String topic : trainingTopics) {
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Training Iteration Epoch : " + i + "; Training Model : " + j + "; Document : " + topic + "\n\n");
			Document document = ResultOutput.deserialize(topic, serializeOutput, false);
			
			ResultOutput.printParameter(returnPara, logFile);
			if (j == 0) {
				returnPara = search.trainingBySearch(document, returnPara, phaseID);
			} else {
				search.testingBySearch(document, generateWeightForTesting(returnPara), phaseID, true);
				IClassifier classifier = EecbConstructor.createClassifier(classificationMethod);
				List<String> filePaths = getPaths(i, j);
				returnPara = classifier.train(filePaths, returnPara);
			}
		}
		
		return returnPara.makeCopy();
	}
	
	private List<String> getPaths(int i, int j) {
		List<String> filePaths = new ArrayList<String>();
		filePaths.addAll(getPaths(trainingTopics, i, j));
		filePaths.addAll(getPaths(testingTopics, i, j));
		return filePaths;
	}
	
	private List<String> getPaths(String[] topics, int i, int j) {
		List<String> files  = new ArrayList<String>();
		for (String topic : topics) {
			String topicPath = experimentResultFolder + "/" + topic + "/";
			List<String> file = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
			
			Map<String, String> fileMap = new HashMap<String, String>();
			for (String f : file) {
				fileMap.put(f, "1");
			}
			
			for (int it = 0; it <= i; it++) {
				for (int jt = 0; jt <= j; jt++) {
					fileMap.remove(it + "-" + jt);
				}
			}
			
			List<String> editeFile = new ArrayList<String>();
			for (String key : fileMap.keySet()) {
				editeFile.add(topicPath + "/" + key);
			}
			
			files.addAll(editeFile);
		}
		return files;
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
	
	private void testDocument(String[] topics, double[] weight, int i, int j, boolean postProcess, String phase){
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
		
		String phaseID = i + "-" + j;
		for(String topic : topics) {
			
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Testing Iteration Epoch : " + phaseID + "; Document :" + topic + "\n\n");
		
			Document document = ResultOutput.deserialize(topic, serializeOutput, false);
			ISearch search = EecbConstructor.createSearchMethod(searchMethod);
			State<CorefCluster> bestLossState = search.testingBySearch(document, weight, phaseID, false);
			
			document.corefClusters = bestLossState.getState();
			DocumentAlignment.updateOrderedPredictedMentions(document);
			
			try {
				DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
				CorefSystem cs = new CorefSystem();
				cs.getCorefSystem().coreference(document, pronounSieve);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			if (postProcess) {
				SieveCoreferenceSystem.postProcessing(document);
			}
			
			SieveCoreferenceSystem.printConllOutput(document, writerPredicted, false, postProcess);
            SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
		}
		
		CoNLLScorerHelper conllScorerHelper = new CoNLLScorerHelper(i * 10 + j, logFile);
		conllScorerHelper.printFinalCoNLLScore(goldCorefCluster, predictedCorefCluster, phase);
		double predictedCoNLL = conllScorerHelper.getFinalCoNllF1Result();
		double predictedScore = conllScorerHelper.getLossScoreF1Result();
	}
	
	/**
	 * generate weight for testing, average weight or latest weight
	 * 
	 * @param para
	 * @return
	 */
	private double[] generateWeightForTesting(Parameter para) {
		boolean averageWeight = Boolean.parseBoolean(mProps.getProperty(EecbConstants.WEIGHT_PROP, "true"));
		double[] learnedWeight;
		if (averageWeight) {
			learnedWeight = DoubleOperation.divide(para.getTotalWeight(), para.getNoOfViolation());
		} else {
			learnedWeight = para.getWeight();
		}
		return learnedWeight;
	}
}
