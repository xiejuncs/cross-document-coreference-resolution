package edu.oregonstate.experiment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;

import net.didion.jwnl.JWNL;
import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.CoNLLScorerHelper;
import edu.oregonstate.training.Train;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.StringUtils;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.experiment.dataset.DatasetFactory;
import edu.oregonstate.experiment.dataset.IDataSet;

/**
 * The abstract class of Experiment Setting
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 * 
 */
public abstract class ExperimentConstructor {
	
	//
	// properties of the experiment
	//
	public static Properties property;
	
	//
	// topics used for experiment, training and testing
	//
	/** the topics used for conducting the experiment */
	protected String[] experimentTopics;
	
	/** training topics */
	protected String[] trainingTopics;
	
	/** testing topics */
	protected String[] testingTopics;
	
	/** development topics */
	protected String[] developmentTopics;
	
	//
	// various path
	//
	/** experiment result folder */
	public static String experimentResultFolder;
	
	/** corpus path, just corpus directory, including all the stuffs used for the experiment */
	public static String corpusPath;
	
	/** store the mention result, used for gold mentions, delete after the experiment */
	protected String mentionRepositoryPath;
	
	/** current experiment folder, each topic is an directory, used for storing the intermediate results */
	public static String currentExperimentFolder;
	
	/** log file */
	public static String logFile;
	
	/** object file */
	public static String serializedOutput;
	
	/** violated file */
	protected String violatedFile;
	
	/** store the intermediate weights produced by the algorithm */
	protected String weightFile;
	
	/** SRL result path */
	public static String SRL_PATH;
	
	/** the folder path store the training set produced during the training phase */
	public static String linearRegressionTrainingPath;
	
	/** data path, which is used to access the file and read the document */
	public static String DATA_PATH;
	
	/** gold mention path */
	public static String MENTION_PATH;
	
	/** the similarity dictionary created by Lin */
	public static String WORD_SIMILARITY_PATH;
	
	/** the configuration file for WORDNET */
	public static String WORD_NET_CONFIGURATION_PATH;
	
	/** English Stop word path */
	public static String STOPWORD_PATH;
	
	/** scorer path */
	public static String conllScorerPath;
	
	/** conll output folder used for scoring */
	public static String conllResultPath;
	
	/** feature folder path */
	public static String featureFolderPath;
	
	/* output feature path */
	public static String featurePath;
	
	//
	// scoring results path
	//
	/** just output the train and test result, for example pairwise */
	public static String mscorePath;
	
	/* overall training result path */
	public static String trainingPath;
	
	/* overall testing result path */
	public static String testingPath;
	
	//
	// conditions
	//
	/** whether output feature for violated case */
	public static boolean outputFeature;
	
	/** whether output text for producing the SRL annotation */
	public static boolean outputText;
	
	/** Reading text when there is null pointer */
	public static boolean enableNull;
	
	/** whether normalize weight */
	public static boolean normalizeWeight;
	
	/** whether enforce the stopping criterion */
	public static boolean stoppingCriterion;

	/** just deal with gold mention cases */
	public static boolean goldOnly;
	
	/** whether we need to post-process the document, apply for the predicted mentions */
	public static boolean postProcess;
	
	/** whether the current experiment is Stanford experiment */
	public static boolean stanfordExperiment;
	
	/** generate features for the experiment */
	public static boolean oregonStateExperiment = true;
	
	/** whether put all documents of a topic together */
	public static boolean dataSetMode;
	
	/** whether enable all zero condition to stop the search during the search phase */
	public static boolean enableZeroCondition;
	
	/** whether debug or run the whole experiment */
	protected boolean mDebug;
	
	/** whether conduct validation on development set in order to tune the parameter */
	public static boolean tuningParameter;
	
	/** whether extend the feature */
	public static boolean extendFeature;
	
	/* train gold and test gold */
	protected boolean trainGoldOnly;
	protected boolean testGoldOnly;
	
	/* train post process and test post process */
	protected boolean trainPostProcess;
	protected boolean testPostProcess;
	
	/* HALT feature PA algorithm */
	public static boolean paHalt;
	
	/* only testing */
	protected boolean onlyTesting;
	
	//
	// data used for experiment 
	//
	/** for mention words feature */
	public static Map<String, List<String>> datas;
	
	/** dictionary used for creating features */
	public static Dictionaries mdictionary;
	
	/** stopping rate */
	public static double stoppingRate;
	
	/** stochastic learning rate */
	public static double learningRate;
	
	/** features used in the experiment */
	public static String[] features;
	
	/* score interval */
	protected int mInterval;
	
	/* the loss score type */
	public static String lossScoreType;
	
	/**
	 * initialize the Parameters
	 */
	public ExperimentConstructor(String configurationFile) {
		
		//
		// read the property configuration file: property
		//
		String[] args = new String[] {"-props", configurationFile};
		property = StringUtils.argsToProperties(args);
		
		//
		// topics used for experiment: mDebug, experimentTopics, trainingTopics, testingTopics, corpusPath
		//
		mDebug = Boolean.parseBoolean(property.getProperty(EecbConstants.DEBUG_PROP, "false"));
		if (mDebug) {
			experimentTopics = EecbConstants.debugTopics;
			corpusPath = EecbConstants.LOCAL_CORPUS_PATH;
			splitTopics(2);
			developmentTopics = EecbConstants.debugDevelopmentTopics;
		} else {
			experimentTopics = EecbConstants.stanfordTotalTopics;
			corpusPath = EecbConstants.CLUSTER_CPRPUS_PATH;
			splitTopics(12);
			developmentTopics = EecbConstants.stanfordDevelopmentTopics;
		}
		
		//
		// various path: experimentResultFolder, stanfordExperiment
		//
		
		/** create a directory to store the output of the specific experiment */
		StringBuilder sb = new StringBuilder();
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		sb.append(corpusPath + "corpus/TEMPORYRESUT/" + timeStamp);
		
		/*gold mention or predicted mention */
		trainGoldOnly = Boolean.parseBoolean(property.getProperty(EecbConstants.TRAIN_GOLD_PROP, "true"));
		if (trainGoldOnly) {
			sb.append("-traingold");
		} else {
			sb.append("-trainpredicted");
		}
		
		testGoldOnly = Boolean.parseBoolean(property.getProperty(EecbConstants.TEST_GOLD_PROP, "true"));
		if (testGoldOnly) {
			sb.append("-testgold");
		} else {
			sb.append("-testpredicted");
		}
		
		/* flat or hierarchy */
		dataSetMode = Boolean.parseBoolean(property.getProperty(EecbConstants.DATASET_PROP, "true"));
		if (dataSetMode) {
			sb.append("-flat");
		} else {
			sb.append("-hierarchy");
		}
		
		/* Stanford experiment or Oregon State statement */
		if (property.containsKey(EecbConstants.SEARCH_PROP)) {
			String searchModel = property.getProperty(EecbConstants.SEARCH_PROP);
			String searchWidth = property.getProperty(EecbConstants.SEARCH_BEAMWIDTH_PROP);
			String searchStep = property.getProperty(EecbConstants.SEARCH_MAXIMUMSTEP_PROP);
			stanfordExperiment = false;
			sb.append("-oregonstate-" + searchModel + "-" + searchWidth + "-" + searchStep);
		} else {
			stanfordExperiment = true;
			sb.append("-stanford");
		}
		
		/* classifier method */
		String classifierLearningModel = property.getProperty(EecbConstants.CLASSIFIER_PROP);   // classification model
		String classifierNoOfIteration = property.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP);   // epoch of classification model
		if (classifierLearningModel.startsWith("Structured")) {
			sb.append("-pairwise");
		} else {
			sb.append("-pairwisesingleton");
		}
		sb.append("-" + classifierNoOfIteration);
		
		/* tuning or halt */
		String stopping = property.getProperty(EecbConstants.STOPPING_PROP, "none");
		if (stopping.equals("tuning")) {
			sb.append("-tuning");
			stoppingCriterion = true;
			tuningParameter = true;
			extendFeature = false;
		} else if (stopping.equals("halt")) {
			sb.append("-halt");
			stoppingCriterion = false;
			tuningParameter = false;
			extendFeature = true;
			
			paHalt = Boolean.parseBoolean(property.getProperty(EecbConstants.HALT_PATRAINING_PROP, "false"));
			if (paHalt) {
				sb.append("-PA");
			} else {
				sb.append("-notPA");
			}
		} else {
			sb.append("none");
			stoppingCriterion = false;
			tuningParameter = false;
			extendFeature = false;
		}
		
		/* whether process or not process */
		trainPostProcess = Boolean.parseBoolean(property.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
		if (trainPostProcess) {
			sb.append("-trainPostProcess");
		} else {
			sb.append("-trainNotPostProcess");
		}
		testPostProcess = Boolean.parseBoolean(property.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
		if (testPostProcess) {
			sb.append("-testPostProcess");
		} else {
			sb.append("-testNotPostProcess");
		}
		
		/* score type */
		lossScoreType = property.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP);
		sb.append("-" + lossScoreType);
		
		outputFeature = Boolean.parseBoolean(property.getProperty(EecbConstants.CLASSIFIER_OUTPUTFEATURE_PROP, "false"));
		if (outputFeature) {
			sb.append("-outputfeature");
		} 
		
		experimentResultFolder = sb.toString();
		Command.createDirectory(experimentResultFolder);
		if (trainGoldOnly || testGoldOnly) {
			mentionRepositoryPath = experimentResultFolder + "/mentionResult";
			Command.createDirectory(mentionRepositoryPath);
		}
		
		logFile = experimentResultFolder + "/" + "experimentlog";
		trainingPath = experimentResultFolder + "/training.csv";
		testingPath = experimentResultFolder + "/testing.csv";
		featureFolderPath = experimentResultFolder + "/featureFolder";
		Command.createDirectory(featureFolderPath);
		
		/** create document serialization folder which store document serialization object */
		serializedOutput = experimentResultFolder + "/documentobject";
		Command.createDirectory(serializedOutput);
		
		SRL_PATH = corpusPath + "corpus/tokenoutput/";
		DATA_PATH = corpusPath + "corpus/EECB1.0/data/";
		MENTION_PATH = corpusPath + "corpus/mentions.txt";
		WORD_SIMILARITY_PATH = corpusPath + "corpus/sims.lsp";
		WORD_NET_CONFIGURATION_PATH = corpusPath + "corpus/file_properties.xml";
		STOPWORD_PATH = corpusPath + "corpus/english.stop";
		
		weightFile = experimentResultFolder + "/weights";
		violatedFile = experimentResultFolder + "/violatedFile";
		if (stanfordExperiment) {
			linearRegressionTrainingPath = experimentResultFolder + "/trainingSet";
			Command.createDirectory(linearRegressionTrainingPath);
		} else {
			conllResultPath = experimentResultFolder + "/conllResult";
			Command.createDirectory(conllResultPath);
		}
		
		/** some features are constant across all experiments */
		
		outputText = false;
		enableZeroCondition = false;
		enableNull = false;
		normalizeWeight = false;
		onlyTesting  = Boolean.parseBoolean(property.getProperty(EecbConstants.TESTING_PROP, "true"));
		// do not need tune parameter on development set
		if (onlyTesting) {
			tuningParameter = false;
		}
		
		mInterval = Integer.parseInt(property.getProperty(EecbConstants.INTERVAL_PROP));
		
		/** initialize the dictionary */
		CorefSystem cs = new CorefSystem();
		mdictionary = cs.corefSystem.dictionaries();

		/** configure the Wordnet and Lin's dictionary */
		configureJWordNet();
		configureWordSimilarity();
		
		/** read the Dataset */
		createDataSet();
		
		/** specify the features */
		if (extendFeature) {
			features = Feature.extendFeaturesName;
		} else {
			features = Feature.featuresName;
		}
		
	}
	
	/** all subclass need to implement the train method */
	protected abstract void performExperiment();
	
	/** split the topics */
	protected void splitTopics(int index) {
		trainingTopics = new String[index];
		testingTopics = new String[experimentTopics.length - index];
		for (int i = 0; i < experimentTopics.length; i++) {
			if (i < index) {
				trainingTopics[i] = experimentTopics[i];
			} else {
				testingTopics[i - index] = experimentTopics[i];
			}
		}
	}
	
	/**
	 * create the training set and testing set according to topics
	 */
	protected void createDataSet() {
		int totalPredictedMentions = 0;
		int totalGoalMentions = 0;
		IDataSet mDatasetMode = DatasetFactory.createDataSet(dataSetMode);
		String corpusStatisticsPath = experimentResultFolder + "/corpusStatisticsPath";
		
		if (stanfordExperiment) {
			Train.currentOutputFileName = linearRegressionTrainingPath + "/initial.csv";
		}
		
		String initialResult = experimentResultFolder + "/initialResult.csv";
		// training set
		goldOnly = trainGoldOnly;
		int[] trainStatistics = generateDateSet(trainingTopics, "training", mDatasetMode, corpusStatisticsPath, trainPostProcess, initialResult);
		totalGoalMentions += trainStatistics[0];
		totalPredictedMentions += trainStatistics[1];
		
		Train.currentOutputFileName = "";
		if (tuningParameter) {
			int[] valiadtionStatistics = generateDateSet(developmentTopics, "validation", mDatasetMode, corpusStatisticsPath, trainPostProcess, initialResult);
			totalGoalMentions += valiadtionStatistics[0];
			totalPredictedMentions += valiadtionStatistics[1];
		}
		
		goldOnly = testGoldOnly;
		int[] testStatistics = generateDateSet(testingTopics, "testing", mDatasetMode, corpusStatisticsPath, testPostProcess, initialResult);
		totalGoalMentions += testStatistics[0];
		totalPredictedMentions += testStatistics[1];
		
		// total mentions : 7980
		ResultOutput.writeTextFile(logFile, "the total number of gold mentions :" + totalGoalMentions );
		ResultOutput.writeTextFile(logFile, "the total number of predicted mentions :" + totalPredictedMentions );
		ResultOutput.writeTextFile(corpusStatisticsPath, totalGoalMentions + " " + totalPredictedMentions);
	}
	
	/**
	 * genearate the dataset according to different phases
	 * 
	 * @param topics training, testing, or validation
	 * @param phase
	 * @param mDatasetMode
	 * @param corpusStatisticsPath
	 * @param doPostProcess
	 * @param initialResult
	 * @return
	 */
	private int[] generateDateSet(String[] topics, String phase, IDataSet mDatasetMode, String corpusStatisticsPath, boolean doPostProcess, String initialResult) {
		int totalGoalMentions = 0;
		int totalPredictedMentions = 0;
		boolean training = false;
		if (phase.equals("training")) {
			training = true;
		}
		
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + phase;
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + phase;
		PrintWriter writerPredicted = null;
		PrintWriter writerGold = null;
		try {
			writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
			writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		for(String topic : topics) {
			ResultOutput.writeTextFile(logFile, "create " + phase + " data set for " + topic);
			Document document = createDocument(topic, mDatasetMode, training, corpusStatisticsPath);
			totalGoalMentions += document.allGoldMentions.size();
			totalPredictedMentions += document.allPredictedMentions.size();
			DocumentAlignment.updateOrderedPredictedMentions(document);
			SieveCoreferenceSystem.printConllOutput(document, writerPredicted, false, doPostProcess);
		    SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
		}
		
		CoNLLScorerHelper conllScorerHelper = new CoNLLScorerHelper(0, logFile);
		conllScorerHelper.printFinalCoNLLScore(goldCorefCluster, predictedCorefCluster, phase);
		double predictedCoNLL = conllScorerHelper.getFinalCoNllF1Result();
		double predictedScore = conllScorerHelper.getLossScoreF1Result();
		
		ResultOutput.writeTextFile(initialResult, phase + "," + predictedCoNLL + "," + predictedScore);
		return new int[]{totalGoalMentions, totalPredictedMentions};
	}
	
	/**
	 * create document according to the topic and serialize the document
	 * 
	 * @param topic
	 * @param training
	 * @return
	 */
	private Document createDocument(String topic, IDataSet mDatasetMode, boolean training, String corpusStatisticsPath) {
		currentExperimentFolder = experimentResultFolder + "/" + topic;
		Command.createDirectory(currentExperimentFolder);
		ResultOutput.writeTextFile(logFile, "create data set for " + topic);

		String[] tops = {topic};
		Document document = mDatasetMode.getData(tops);

		// create the training examples
		if (stanfordExperiment && training) {
			ResultOutput.writeTextFile(logFile, "create verb training examples for " + topic+ "................");
			// for verb pair case, noun pair case is generating during seven high precision sieves
			trainingVerbPairExample(document, Train.currentOutputFileName);
		}

		ResultOutput.writeTextFile(logFile, "number of gold mentions : " + document.allGoldMentions.size());
		ResultOutput.writeTextFile(logFile, "number of predicted mentions : " + document.allPredictedMentions.size());
		ResultOutput.writeTextFile(corpusStatisticsPath, topic + " " + document.allGoldMentions.size() + " " + document.goldCorefClusters.size() + " " + document.allPredictedMentions.size() + " " +
				document.corefClusters.size());
		ResultOutput.serialize(document, topic, serializedOutput);
		ResultOutput.writeTextFile(logFile, "\n");
		
		return document;
	}
	
	/**
	 * generate training examples for verb pairs during the initial training phase
	 * 
	 * @param document
	 * @param currentOutputFileName
	 */
	private void trainingVerbPairExample(Document document, String currentOutputFileName) {
		 // inspect all the pairs of singleton verbal clusters
	    Map<Integer, CorefCluster> corefClusters = document.corefClusters;
	    List<CorefCluster> verbSingletonCluster = new ArrayList<CorefCluster>();  // add the singleton verbal cluster
	    for (Integer clusterid : corefClusters.keySet()) {
	    	CorefCluster cluster = corefClusters.get(clusterid);
	    	Set<Mention> mentions = cluster.getCorefMentions();
	    	if (mentions.size() > 1) {
	    		continue;
	    	} else {
	    		Mention mention = cluster.firstMention;
	    		boolean isVerb = mention.isVerb;
	    		if (isVerb) {
	    			verbSingletonCluster.add(cluster);
	    		}
	    	}
	    }
	    
	    // generate training data for Event
	    Map<Integer, Mention> goldMentions = document.allGoldMentions; // use the gold coref cluster to calculate the quality for this merge
	    for (int i = 0; i < verbSingletonCluster.size(); i++) {
	    	for (int j = 0; j < i; j++) {
	    		CorefCluster ci = verbSingletonCluster.get(i);
	    		CorefCluster cj = verbSingletonCluster.get(j);
	    		Counter<String> features = Feature.getFeatures(document, ci, cj, false, mdictionary); // get the feature
	    		Mention ciFirstMention = ci.getFirstMention();
	    		Mention cjFirstMention = cj.getFirstMention();
	    		double correct = 0.0;
	    		double total = 1.0;
	    		
	    		if (goldMentions.containsKey(ciFirstMention.mentionID) && goldMentions.containsKey(cjFirstMention.mentionID)) {
					if (goldMentions.get(ciFirstMention.mentionID).goldCorefClusterID == goldMentions.get(cjFirstMention.mentionID).goldCorefClusterID) {
						ResultOutput.writeTextFile(logFile, "verb training example " + ci.clusterID + " " + cj.clusterID);
						
						correct += 1.0;
					}
	    		}
	    		double quality = correct / total;
	    		String record = ResultOutput.buildString(features, quality);
	    		ResultOutput.writeTextFilewithoutNewline(currentOutputFileName, record);
	    	}
	    } 
	}
	
	/**
	 * configure the WORDNET
	 */
	protected void configureJWordNet() {
		try {
			System.out.println("begin configure WORDNET");
			JWNL.initialize(new FileInputStream(WORD_NET_CONFIGURATION_PATH));
			System.out.println("finish configure WORDNET");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
	/** 
	 * configure word similarity matrix 
	 */
	protected void configureWordSimilarity() {
		WordSimilarity wordSimilarity = new WordSimilarity(WORD_SIMILARITY_PATH);
		wordSimilarity.initialize();
		datas = wordSimilarity.datas;
	}
	
}
