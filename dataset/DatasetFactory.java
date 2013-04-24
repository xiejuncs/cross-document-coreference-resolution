package edu.oregonstate.dataset;

import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
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
	
	/** experiment result folder */
	private final String mExperimentResultFolder;
	
	/** document serialized output */
	private final String serializedOutput;
	
	/** loss type */
	private final ScoreType lossType;
	
	/** corpus statistics path */
	private final String corpusStatisticsPath;
	
	/** phase, for example the second round */
	private final String phaseIndex;
	
	/** conll result path */
	private final String conllResultPath;
	
	/** print the result with baseline 1 */
	private final boolean printResult;
	
	public DatasetFactory(Properties props) {
		super(props);
		mExperimentResultFolder = ExperimentConstructor.experimentFolder;
		corpusStatisticsPath = mExperimentResultFolder + "/corpusStat";
		
		serializedOutput = mExperimentResultFolder + "/document";
		
		lossType = ScoreType.valueOf(experimentProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise"));
		
		phaseIndex = props.getProperty(EecbConstants.PHASE_PROP, "0");
		
		// store into the phaseIndex folder
		conllResultPath = mExperimentResultFolder + "/conll/" + phaseIndex;
		Command.mkdir(conllResultPath);
		
		printResult = false;
	}
	
	/**
	 * generating data set
	 * 
	 */
	public void performExperiment() {
		generateDataSet();
	}
	
	/**
	 * generate all dataSet, including training set, testing set and development set
	 * 
	 */
	private void generateDataSet() {
		TopicGeneration topicGenerator = new TopicGeneration(experimentProps);
		String topicInformation = topicGenerator.topic();
		
		String[] element = topicInformation.split("-");
		String topic = element[0];
		String phase = element[1] + "-" + topic;
		
		// generate document
		generateDocument(topic, phase);
	}
	
	/**
	 * generate document
	 * 
	 * @param topic
	 * @param goldOnly
	 * @return
	 */
	private void generateDocument(String topic, String phase) {
		// generate CONLL file to store the Coreference Resolution result
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + phase;
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + phase;
		
		// gold only and whether post process
		boolean goldOnly = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP, "true"));
		String experimentLogFile = mExperimentResultFolder + "/" + topic + "/logfile";

		String currentExperimentFolder = mExperimentResultFolder + "/" + topic;
		Command.mkdir(currentExperimentFolder);
		ResultOutput.writeTextFile(experimentLogFile, "\ncreate data set for " + topic);
		
		IDataSet mDatasetMode = createDataSetMode();
		Document document = mDatasetMode.getData(topic, goldOnly);
		ResultOutput.serialize(document, topic, serializedOutput);
		
		// print result
		if (printResult) {
			// do pronoun coreference resolution
			CorefSystem cs = new CorefSystem();
			cs.applyPronounSieve(document);

			// whether post-process the document
			if (postProcess) {
				DocumentAlignment.postProcessDocument(document);
			}
		}
		
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
		
		ResultOutput.writeTextFile(experimentLogFile, "\n");
	}

	/* whether put all documents of a topic together, true CrossTopic, false WithinCross */
	private IDataSet createDataSetMode() {
		/* data set mode : within or cross topic */
		boolean dataSetMode = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATAGENERATION_DATASET_PROP, "true"));
		
		IDataSet mDatasetMode = null;
		
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
		ExperimentConstructor datasetGenerator = new DatasetFactory(props);
		datasetGenerator.performExperiment();
	}
	
}
