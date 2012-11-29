package edu.oregonstate.experiment.crosscoreferenceresolution.stochasticgradient;


import java.util.Calendar;

import edu.oregonstate.CorefSystem;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;

/**
 * Cross Coreference Resolution with Stochastic Gradient
 * 
 * Experiment Configuration
 * classifier: stochastic gradient (iteration no: 10)
 * cost function: linear function
 * loss function: pairwise
 * search method: beam search(beam width: 5, search step: 300)
 * clustering method: none
 * debug: true
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CrossCoreferenceResolutionStochasticGradientPredicted extends ExperimentConstructor {

	@Override
	/**
	 * perform the experiment
	 * 
	 */
	protected void performExperiment() {
		configureExperiment();
		
		int iteration = (Integer) getParameter(EecbConstants.CLASSIFIER, "noOfIteration");
		int noOfFeature = (Integer) getParameter(EecbConstants.CLASSIFIER, "noOfFeature");
		double[] weight = new double[noOfFeature];
		double[] totalWeight = new double[noOfFeature];
		int mTotalViolations = 0;
		int mTotalSearchSteps = 0;
		
		/** 
		 * if debug, print each iteration's detail information,
		 * else, just print the final test result given the final weight 
		 */
		if (mDebug) {
			for (int i = 0; i < iteration; i++) {
				ResultOutput.writeTextFile(logFile, "The " + i + "th iteration....");
				int mViolations = 0;
				int searchSteps = 0;
				
				// go through all the training examples
				for (int j = 0; j < trainingTopics.length; j++) {
					// get the document
					String topic = trainingTopics[j];
					ResultOutput.writeTextFile(logFile, "Starting to do training on " + topic);
					Document document = ResultOutput.deserialize(topic, serializedOutput, false);
					
					// before search parameters
					ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
					printParameters(document, topic);
					
					// configure dynamic file and folder path
					currentExperimentFolder = experimentResultFolder + "/" + topic;
					Command.createDirectory(currentExperimentFolder);
					mscorePath = currentExperimentFolder + "/" + "train-iteration" + (i + 1) + "-" + topic;
					mScoreDetailPath = currentExperimentFolder + "/" + "train-iteration" + (i + 1) + "-" + topic + "-scoredetail";
					
					// write all the information out
					ISearch mSearchMethod = createSearchMethod((String) getParameter(EecbConstants.SEARCHMETHOD, "model"));
					mSearchMethod.setWeight(weight);
					mSearchMethod.setTotalWeight(totalWeight);
					mSearchMethod.setDocument(document);
					mSearchMethod.trainingSearch();
					weight = mSearchMethod.getWeight();
					totalWeight = mSearchMethod.getTotalWeight();
					mViolations += mSearchMethod.getViolations();
					searchSteps += mSearchMethod.getSearchStep();
					
					// after search parameters
					ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after search");
					printParameters(document, topic);
				}
				
				
				// print weight information
				ResultOutput.writeTextFile(logFile, "violated constraints for " + i + " iteration is " + mViolations);
				ResultOutput.writeTextFile(logFile, "Search steps for " + i + " iteration is " + searchSteps);
				mTotalViolations += mViolations;
				mTotalSearchSteps += searchSteps;
				ResultOutput.writeTextFile(logFile, "total search step: " + mTotalSearchSteps);
				ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
				ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
				ResultOutput.writeTextFile(logFile, "total violation :" + mTotalViolations);
				double[] averageWeight = DoubleOperation.divide(totalWeight, mTotalViolations);
				ResultOutput.writeTextFile(logFile, "average weight vector : " + DoubleOperation.printArray(averageWeight));
				ResultOutput.writeTextFile(logFile, "\n");
				
				
				// go through all the testing example
				for (int j = 0; j < testingTopics.length; j++) {
					// get the document
					String topic = testingTopics[j];
					ResultOutput.writeTextFile(logFile, "Starting to do testing on " + topic);
					Document document = ResultOutput.deserialize(topic, serializedOutput, false);
					
					// before search parameters
					ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
					printParameters(document, topic);
					
					// configure dynamic file and folder path
					currentExperimentFolder = experimentResultFolder + "/" + topic;
					Command.createDirectory(currentExperimentFolder);
					mscorePath = currentExperimentFolder + "/" + "test-iteration" + (i + 1) + "-" + topic;
					mScoreDetailPath = currentExperimentFolder + "/" + "test-iteration" + (i + 1) + "-" + topic + "-scoredetail";
					
					// use search to do testing
					ISearch mSearchMethod = createSearchMethod((String) getParameter(EecbConstants.SEARCHMETHOD, "model"));
					mSearchMethod.setWeight(averageWeight);
					mSearchMethod.setDocument(document);
					mSearchMethod.testingSearch();
					
					// after search parameters
					ResultOutput.writeTextFile(logFile, topic +  "'s detail after search");
					printParameters(document, topic);
				}
				
			}
		} else {
		}
		
	}
	
	
	
	/**
	 * configuration the experiment
	 * 
	 */
	private void configureExperiment() {
		
		setDebugMode(true);
		
		boolean debugExperiment = false;
		if (debugExperiment) {
			experimentTopics = debugTopics;
			splitTopics(2);
			corpusPath = "../";
		} else {
			experimentTopics = totalTopics;
			splitTopics(12);
			corpusPath = "/nfs/guille/xfern/users/xie/Experiment/";
		}
		
		// define dataset 
		addParas(EecbConstants.DATASET, "corpusPath", corpusPath + "corpus/EECB1.0/data/");
		addParas(EecbConstants.DATASET, "srlpath", corpusPath + "corpus/tokenoutput/");
		addParas(EecbConstants.DATASET, "sieve", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch");
		addParas(EecbConstants.DATASET, "annotationPath", corpusPath + "corpus/mentions-backup.txt");
		addParas(EecbConstants.DATASET, "wordnetConfigurationPath", corpusPath + "corpus/file_properties.xml");
		addParas(EecbConstants.DATASET, "wordsimilaritypath", corpusPath + "corpus/sims.lsp");
		addParas(EecbConstants.DATASET, "outputPath", corpusPath + "corpus/TEMPORYRESUT/");
		addParas(EecbConstants.DATASET, "aligned", false);
		
		// define classifier parameter
		addParas(EecbConstants.CLASSIFIER, "noOfIteration", 10);
		addParas(EecbConstants.CLASSIFIER, "noOfFeature", Feature.featuresName.length);
		addParas(EecbConstants.CLASSIFIER, "model", "StochasticGradient");
		addParas(EecbConstants.CLASSIFIER, "learningRate", 0.01);

		// define search parameter
		addParas(EecbConstants.SEARCHMETHOD, "beamWidth", 1);
		addParas(EecbConstants.SEARCHMETHOD, "searchStep", 300);
		addParas(EecbConstants.SEARCHMETHOD, "model", "BeamSearch");

		// define cost function parameter
		addParas(EecbConstants.LOSSFUNCTION, "scoreType", ScoreType.Pairwise);
		addParas(EecbConstants.LOSSFUNCTION, "model", "MetricLossFunction");

		// define cost function
		addParas(EecbConstants.COSTFUNCTION, "model", "LinearCostFunction");
		
		configureJWordNet();
		configureWordSimilarity();
		
		// create a folder to contain all log information
		String outputPath = (String) getParameter(EecbConstants.DATASET, "outputPath");
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		experimentResultFolder = outputPath + timeStamp;
		Command.createDirectory(experimentResultFolder);
		
		// create mention result folder to store the mention serialization object
		mentionResultPath = experimentResultFolder + "/mentionResult";
		Command.createDirectory(mentionResultPath);
		
		/** configure other parameters */
		logFile = outputPath + timeStamp + "/" + "experimentlog";
		ResultOutput.writeTextFile(logFile, "corpus path : " + corpusPath);
		outputText = false;
		enableNull = false;
		incorporateTopicSRLResult = false;
		incorporateDocumentSRLResult = false;
		goldOnly = false;
		normalizeWeight = false;
		outputFeature = false;
		
		// print configure information
		ResultOutput.printTime();
		ResultOutput.writeTextFile(logFile, "corpus path : " + corpusPath);
		ResultOutput.writeTextFile(logFile, "experiment topics : " + buildString(experimentTopics));
		
		/** create document serialization folder which store document serialization object */
		serializedOutput = experimentResultFolder + "/documentobject";
		Command.createDirectory(serializedOutput);
		
		// create dataset model
		mDatasetMode = createDataSetModel("WithinCross");
		createDataSet();
		
		/** initialize the dictionary */
		CorefSystem cs = new CorefSystem();
		mdictionary = cs.corefSystem.dictionaries();
	}
	
	
	public static void main(String[] args) {
		CrossCoreferenceResolutionStochasticGradientPredicted ccrs = new CrossCoreferenceResolutionStochasticGradientPredicted();
		ccrs.performExperiment();
	}
}
