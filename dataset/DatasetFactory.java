package edu.oregonstate.dataset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;

/**
 * Create a series of Document objects which are serialized
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class DatasetFactory {
	
	/* experiment result folder */
	private final String mExperimentResultFolder;
	
	/* property file */
	private final Properties experimentProperties;
	
	/* total number of gold mentions */
	private int totalGoalMentions;
	
	/* total number of predicted mentions */
	private int totalPredictedMentions;
	
	/* corpus statistics file path */
	private final String corpusStatisticsPath;
	
	/* initial result file path */
	private final String initialResult;
	
	/* log file  */
	private final String logFile;
	
	/* document serialized output */
	private final String serializedOutput;
	
	/* loss type */
	private final ScoreType lossType;
	
	/* enable Stanford pre-process step during data generation */
	private final boolean enableStanfordPreprocessStep;
	
	public DatasetFactory() {
		mExperimentResultFolder = ExperimentConstructor.resultPath;
		experimentProperties = ExperimentConstructor.experimentProps;
		logFile = ExperimentConstructor.logFile;
		
		totalGoalMentions = 0;
		totalPredictedMentions = 0;
		corpusStatisticsPath = mExperimentResultFolder + "/corpusStatisticsPath";
		initialResult = mExperimentResultFolder + "/initialResult.csv";
		
		serializedOutput = mExperimentResultFolder + "/document";
		Command.mkdir(serializedOutput);
		
		enableStanfordPreprocessStep = Boolean.parseBoolean(experimentProperties.getProperty(EecbConstants.STANFORD_PREPROCESSING, "true"));
		lossType = ScoreType.valueOf(experimentProperties.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise"));
		
	}
	
	/**
	 * generate all dataset, including training set, testing set and development set
	 * 
	 * @param topics
	 * @param developmentTopics
	 */
	public void generateDataSet() {
		TopicGeneration topicGenerator = new TopicGeneration(experimentProperties);
		
		String[] trainingTopics = topicGenerator.trainingTopics();
		String[] testingTopics = topicGenerator.testingTopics();
		String[] developmentTopics = topicGenerator.developmentTopics();
		
		
		// training set
		if (trainingTopics != null) {
			generateSingleSet(trainingTopics, "training-generation");
		}
		
		// validation set
		if (testingTopics != null) {
			generateSingleSet(testingTopics, "testing-generation");
		}
		
		// testing set
		if (developmentTopics != null) {
			generateSingleSet(developmentTopics, "validation-generation");
		}
		
		//
		// debug information: 7980 in total for all documents
		//
		ResultOutput.writeTextFile(logFile, "the total number of gold mentions :" + totalGoalMentions );
		ResultOutput.writeTextFile(logFile, "the total number of predicted mentions :" + totalPredictedMentions );
		ResultOutput.writeTextFile(corpusStatisticsPath, totalGoalMentions + " " + totalPredictedMentions);
	}
	
	/**
	 * create single set
	 * 
	 * @param topics
	 * @param phase
	 */
	private void generateSingleSet(String[] topics, String phase) {
		String conllResultPath = mExperimentResultFolder + "/conll";
		Document corpus = new Document();
		corpus.goldCorefClusters = new HashMap<Integer, CorefCluster>();
		
		// gold only and whether post process
		boolean goldOnly = Boolean.parseBoolean(experimentProperties.getProperty(EecbConstants.GOLDMENTION_PROP, "true"));
		boolean postProcess= ExperimentConstructor.postProcess;
		
		// generate file
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + phase;
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + phase;
		
		//
		// generate data set
		//
		for(String topic : topics) {
			ResultOutput.writeTextFile(logFile, "\ncreate " + phase + " data set for " + topic);
			Document document = generateDocument(topic, goldOnly);
			totalGoalMentions += document.allGoldMentions.size();
			totalPredictedMentions += document.allPredictedMentions.size();
			
			// enable Stanford pre-process step
			if (enableStanfordPreprocessStep) {

				// do pronoun coreference resolution
				// all predicted mention id can change according to the merge process
				CorefSystem cs = new CorefSystem();
				cs.applyPronounSieve(document);

				// do post process
				if (postProcess) {
					DocumentAlignment.postProcessDocument(document);
				}
				
				DocumentAlignment.mergeDocument(document, corpus);
				// Stanford scoring
				

				ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster);
			}
		}
		
		// Stanford scoring
		String[] scoreInformation = ResultOutput.printDocumentScore(corpus, lossType, logFile, phase);
		//
		// print the final result for the single set
		//
		if (enableStanfordPreprocessStep) {
			double[] finalScores = ResultOutput.printCorpusResult(logFile, goldCorefCluster, predictedCorefCluster, "data generation");
			ResultOutput.writeTextFile(initialResult, scoreInformation[0] + "\t" + finalScores[0] + "\t" + 
					finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);
		}
	}
	
	/**
	 * generate document
	 * 
	 * @param topic
	 * @param goldOnly
	 * @return
	 */
	private Document generateDocument(String topic, boolean goldOnly) {
		String currentExperimentFolder = mExperimentResultFolder + "/" + topic;
		Command.mkdir(currentExperimentFolder);
		ResultOutput.writeTextFile(logFile, "\ncreate data set for " + topic);
		
		IDataSet mDatasetMode = createDataSetMode();
		Document document = mDatasetMode.getData(topic, goldOnly);
		
		ResultOutput.writeTextFile(logFile, "number of gold mentions : " + document.allGoldMentions.size());
		ResultOutput.writeTextFile(logFile, "number of predicted mentions : " + document.allPredictedMentions.size());
		ResultOutput.writeTextFile(corpusStatisticsPath, topic + " " + document.allGoldMentions.size() + " " + document.goldCorefClusters.size() + " " + document.allPredictedMentions.size() + " " +
				document.corefClusters.size());
		ResultOutput.serialize(document, topic, serializedOutput);
		ResultOutput.writeTextFile(logFile, "\n");
		
		return document;
	}

	/* whether put all documents of a topic together, true CrossTopic, false WithinCross */
	private IDataSet createDataSetMode() {
		/* data set mode : within or cross topic */
		boolean dataSetMode = Boolean.parseBoolean(experimentProperties.getProperty(EecbConstants.DATASET_PROP, "true"));
		
		IDataSet mDatasetMode;
		
		// initialize the DatasetMode
		if (dataSetMode) {
			mDatasetMode = EecbConstructor.createDataSetModel("CrossTopic");
		} else {
			mDatasetMode = EecbConstructor.createDataSetModel("WithinCross");
		}
	
		return mDatasetMode;
	}
	
}
