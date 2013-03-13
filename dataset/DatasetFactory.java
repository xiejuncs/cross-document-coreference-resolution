package edu.oregonstate.dataset;

import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.util.StringUtils;

/**
 * Create a series of Document objects which are serialized
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class DatasetFactory extends ExperimentConstructor {
	
	/* experiment result folder */
	private final String mExperimentResultFolder;
	
	/* document serialized output */
	private final String serializedOutput;
	
	/* loss type */
	private final ScoreType lossType;
	
	/** corpus statistics path */
	private final String corpusStatisticsPath;
	
	public DatasetFactory(Properties props) {
		super(props);
		mExperimentResultFolder = ExperimentConstructor.resultPath;
		corpusStatisticsPath = mExperimentResultFolder + "/corpusStat";
		
		serializedOutput = mExperimentResultFolder + "/document";
		Command.mkdir(serializedOutput);
		
		lossType = ScoreType.valueOf(experimentProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise"));
	}
	
	/**
	 * generating data set
	 * 
	 */
	public void performExperiment() {
		//ResultOutput.printTime(experimentLogFile, "The start of the experiment: ");
		generateDataSet();
		//ResultOutput.printTime(experimentLogFile, "The end of the experiment");
	}
	
	/**
	 * generate all dataset, including training set, testing set and development set
	 * 
	 * @param topics
	 * @param developmentTopics
	 */
	public void generateDataSet() {
		TopicGeneration topicGenerator = new TopicGeneration(experimentProps);
		
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
	}
	
	/**
	 * create single set
	 * 
	 * @param topics
	 * @param phase
	 */
	private void generateSingleSet(String[] topics, String phase) {
		String conllResultPath = mExperimentResultFolder + "/conll";
		
		// gold only and whether post process
		boolean goldOnly = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP, "true"));
		
		// generate file
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + phase;
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + phase;
		
		//
		// generate data set
		//
		for(String topic : topics) {
			Document document = generateDocument(topic, goldOnly, goldCorefCluster, predictedCorefCluster);
		}
		
	}
	
	/**
	 * generate document
	 * 
	 * @param topic
	 * @param goldOnly
	 * @return
	 */
	private Document generateDocument(String topic, boolean goldOnly, String goldCorefCluster, String predictedCorefCluster) {
		String experimentLogFile = mExperimentResultFolder + "/" + topic + "/logfile";

		String currentExperimentFolder = mExperimentResultFolder + "/" + topic;
		Command.mkdir(currentExperimentFolder);
		ResultOutput.writeTextFile(experimentLogFile, "\ncreate data set for " + topic);
		
		IDataSet mDatasetMode = createDataSetMode();
		Document document = mDatasetMode.getData(topic, goldOnly);
		
		ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster);
		
		// do scoring
		String[] scoreInformation = ResultOutput.printDocumentScore(document, lossType, experimentLogFile, "single");
		double[] finalScores = ResultOutput.printCorpusResult(experimentLogFile, goldCorefCluster, predictedCorefCluster, "data generation");
		ResultOutput.writeTextFile(experimentLogFile, scoreInformation[0] + "\t" + finalScores[0] + "\t" + 
				finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);
		
		// print the related information about the topic
		ResultOutput.writeTextFile(experimentLogFile, "number of gold mentions : " + document.allGoldMentions.size());
		ResultOutput.writeTextFile(experimentLogFile, "number of predicted mentions : " + document.allPredictedMentions.size());
		ResultOutput.writeTextFile(corpusStatisticsPath, topic + " " + document.allGoldMentions.size() + " " + document.goldCorefClusters.size() + " " + document.allPredictedMentions.size() + " " +
				document.corefClusters.size());
		ResultOutput.serialize(document, topic, serializedOutput);
		ResultOutput.writeTextFile(experimentLogFile, "\n");
		
		return document;
	}

	/* whether put all documents of a topic together, true CrossTopic, false WithinCross */
	private IDataSet createDataSetMode() {
		/* data set mode : within or cross topic */
		boolean dataSetMode = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATAGENERATION_DATASET_PROP, "true"));
		
		IDataSet mDatasetMode;
		
		// initialize the DatasetMode
		if (dataSetMode) {
			mDatasetMode = EecbConstructor.createDataSetModel("CrossTopic");
		} else {
			mDatasetMode = EecbConstructor.createDataSetModel("WithinCross");
		}
	
		return mDatasetMode;
	}
	
	/**
	 * main entry point for data generation module
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 1) {
			 System.out.println("there are more parameters, you just can specify one path parameter.....");
            System.exit(1);
		}
		
		if (args.length == 0) {
			// run the experiment in the local machine for debugging
			args = new String[1];
			args[0] = "../corpus/config.properties";
		}
		
		String[] propArgs = new String[]{"-props", args[0]};
		
		Properties props = StringUtils.argsToProperties(propArgs);
		ExperimentConstructor experiment = new DatasetFactory(props);
		experiment.performExperiment();
	}
	
}
