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
	private final Properties mProps;
	
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
	
	/* whether generate training set */
	private boolean enableGenerateTrainingSet;
	
	// whether generate testing set
	private boolean enableGenerateTestingSet;
	
	// whether generate development set;
	private boolean enableGenerateDevelopmentSet;
	
	public DatasetFactory() {
		mExperimentResultFolder = ExperimentConstructor.experimentResultFolder;
		mProps = ExperimentConstructor.experimentProps;
		logFile = ExperimentConstructor.logFile;
		
		totalGoalMentions = 0;
		totalPredictedMentions = 0;
		corpusStatisticsPath = mExperimentResultFolder + "/corpusStatisticsPath";
		initialResult = mExperimentResultFolder + "/initialResult.csv";
		
		serializedOutput = mExperimentResultFolder + "/documentobject";
		Command.createDirectory(serializedOutput);
		
		enableStanfordPreprocessStep = Boolean.parseBoolean(mProps.getProperty(EecbConstants.ENABLE_STANFORD_PROCESSING_DURING_DATA_GENERATION, "true"));
		lossType = ScoreType.valueOf(mProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise"));
		
		enableGenerateTrainingSet = true;
		enableGenerateTestingSet = true;
		enableGenerateDevelopmentSet = true;
		updateFlags();
		
	}
	
	// update the flags for generating set
	private void updateFlags() {
		// dcoref.training.without.tune
		boolean trainingWithoutTune = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAINING_WITHOUT_TUNE, "false"));
		if (trainingWithoutTune) {
			enableGenerateDevelopmentSet = false;
		}
		
		// dcoref.just.tune.parameter
		boolean justTuneParameter = Boolean.parseBoolean(mProps.getProperty(EecbConstants.JUST_TUNE_PARAMETER, "false"));
		if (justTuneParameter) {
			enableGenerateTrainingSet = false;
			enableGenerateTestingSet = false;
		}
		
	}
	
	/**
	 * generate the dataset
	 */
	public void generateDataSet() {
		
		List<String[]> topics = new ArrayList<String[]>();
		topics.add(ExperimentConstructor.trainingTopics);
		topics.add(ExperimentConstructor.testingTopics);
		topics.add(ExperimentConstructor.developmentTopics);
		generateSet(topics);                      // generate Dataset
		
		//
		// debug information: 7980 in total for all documents
		//
		ResultOutput.writeTextFile(logFile, "the total number of gold mentions :" + totalGoalMentions );
		ResultOutput.writeTextFile(logFile, "the total number of predicted mentions :" + totalPredictedMentions );
		ResultOutput.writeTextFile(corpusStatisticsPath, totalGoalMentions + " " + totalPredictedMentions);
	}
	
	/**
	 * generate all dataset, including training set, testing set and development set
	 * 
	 * @param topics
	 * @param developmentTopics
	 */
	private void generateSet(List<String[]> topics) {
		String[] trainingTopics = topics.get(0);
		String[] testingTopics = topics.get(1);
		String[] developmentTopics = topics.get(2);
		
		// training set
		if (enableGenerateTrainingSet) {
			generateSingleSet(trainingTopics, "training-generation");
		}
		
		// validation set
		if (enableGenerateDevelopmentSet) {
			generateSingleSet(developmentTopics, "validation-generation");
		}
		
		// testing set
		if (enableGenerateTestingSet) {
			generateSingleSet(testingTopics, "testing-generation");
		}
	}
	
	/**
	 * create single set
	 * 
	 * @param topics
	 * @param phase
	 */
	private void generateSingleSet(String[] topics, String phase) {
		String conllResultPath = mExperimentResultFolder + "/conllResult";
		Document corpus = new Document();
		corpus.goldCorefClusters = new HashMap<Integer, CorefCluster>();
		
		// gold only and whether post process
		boolean goldOnly;
		boolean postProcess;
		if (phase.equals("testing-generation")) {
			goldOnly = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TEST_GOLD_PROP, "true"));
			postProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
		} else {
			goldOnly = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAIN_GOLD_PROP, "true"));
			postProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
		}
		
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
				

				ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster, postProcess);
			}
		}
		
		// Stanford scoring
		String[] scoreInformation = ResultOutput.printDocumentScore(corpus, lossType, logFile, phase);
		//
		// print the final result for the single set
		//
		if (enableStanfordPreprocessStep) {
			double[] finalScores = ResultOutput.printCorpusResult(0, logFile, goldCorefCluster, predictedCorefCluster, "data generation");
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
		Command.createDirectory(currentExperimentFolder);
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
		boolean dataSetMode = Boolean.parseBoolean(mProps.getProperty(EecbConstants.DATASET_PROP, "true"));
		
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
