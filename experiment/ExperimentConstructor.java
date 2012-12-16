package edu.oregonstate.experiment;

import java.io.FileInputStream;
import java.util.*;
import java.util.prefs.PreferenceChangeEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.didion.jwnl.JWNL;

import edu.oregonstate.classifier.IClassifier;
import edu.oregonstate.cluster.IClustering;
import edu.oregonstate.costfunction.ICostFunction;
import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.general.FinalScore;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.lossfunction.ILossFunction;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.training.Train;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.StringUtils;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.experiment.dataset.IDataSet;

/**
 * The abstract class of Experiment Setting
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 * 
 */
public abstract class ExperimentConstructor {
	
	//
	// properties for the experiment
	//
	public static Properties property;
	
	//
	// topics used for experiment, training and testing
	//
	/** the topics used for conducting the experiment */
	protected static String[] experimentTopics;
	
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
	public static String mentionRepositoryPath;
	
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
	
	//
	// scoring results path
	//
	/** just output the train and test result, for example pairwise */
	public static String mscorePath;
	
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
		goldOnly = Boolean.parseBoolean(property.getProperty(EecbConstants.GOLD_PROP, "false"));
		if (goldOnly) {
			sb.append("-gold");
		} else {
			sb.append("-predicted");
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
		String stopping = property.getProperty(EecbConstants.STOPPING_PROP);
		if (stopping.equals("tuning")) {
			sb.append("-tuning");
			stoppingCriterion = true;
			tuningParameter = true;
			extendFeature = false;
		} else {
			sb.append("-halt");
			stoppingCriterion = false;
			tuningParameter = false;
			extendFeature = true;
		}
		
		/* whether process or not process */
		postProcess = Boolean.parseBoolean(property.getProperty(EecbConstants.POSTPROCESS_PROP, "false"));
		if (postProcess) {
			sb.append("-PostProcess");
		} else {
			sb.append("-notPostProcess");
		}
		
		/* score type */
		String lossScoreType = property.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP);
		sb.append("-" + lossScoreType);
		
		experimentResultFolder = sb.toString();
		Command.createDirectory(experimentResultFolder);
		if (goldOnly) {
			mentionRepositoryPath = experimentResultFolder + "/mentionResult";
			Command.createDirectory(mentionRepositoryPath);
		}
		
		logFile = experimentResultFolder + "/" + "experimentlog";
		
		/** create document serialization folder which store document serialization object */
		serializedOutput = experimentResultFolder + "/documentobject";
		Command.createDirectory(serializedOutput);
		
		SRL_PATH = corpusPath + "corpus/tokenoutput/";
		DATA_PATH = corpusPath + "corpus/EECB1.0/data/";
		MENTION_PATH = corpusPath + "corpus/mentions.txt";
		WORD_SIMILARITY_PATH = corpusPath + "corpus/sims.lsp";
		WORD_NET_CONFIGURATION_PATH = corpusPath + "corpus/file_properties.xml";
		STOPWORD_PATH = corpusPath + "corpus/english.stop";
		conllScorerPath = "/nfs/guille/xfern/users/xie/Experiment/corpus/scorer/v4/scorer.pl";
		
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
		outputFeature = false;
		outputText = false;
		enableZeroCondition = false;
		enableNull = false;
		normalizeWeight = false;
		
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
	
	/**
	 * create dataset model
	 * 
	 * @param datasetModel
	 * @return
	 */
	protected IDataSet createDataSetModel(String datasetModel) {
		if (datasetModel == null) throw new RuntimeException("dataset model not specified");
		
		if (!datasetModel.contains(".")) {
			datasetModel = "edu.oregonstate.experiment.dataset." + datasetModel;
		}
		
		try {
			Class datasetClass = Class.forName(datasetModel);
			IDataSet dataset = (IDataSet) datasetClass.newInstance();
			return dataset;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * according to the model name, create a classifier to do classification
	 * 
	 * @param modelName
	 * @return
	 */
	public static IClassifier createClassifier(String classficationModel) {
		if (classficationModel == null) throw new RuntimeException("classifier not specified");
		
		if (!classficationModel.contains(".")) {
			classficationModel = "edu.oregonstate.classifier." + classficationModel;
		}
		
		try{
			Class classifierClass = Class.forName(classficationModel);
			IClassifier classifier = (IClassifier) classifierClass.newInstance();
			return classifier;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * according to the cost function name, create a cost function
	 * 
	 * @param costfunction
	 */
	public static ICostFunction createCostFunction(String costfunction) {
		if (costfunction == null) throw new RuntimeException("cost function not specified");
		
		if (!costfunction.contains(".")) {
			costfunction = "edu.oregonstate.costfunction." + costfunction;
		}
		
		try {
			Class costfunctionClass = Class.forName(costfunction);
			ICostFunction costFunction = (ICostFunction) costfunctionClass.newInstance();
			return costFunction;
		} catch (Exception e) {
			throw new RuntimeException("e");
		}
	}
	
	/**
	 * create a loss function
	 * 
	 * @param lossFunction
	 */
	public static ILossFunction createLossFunction(String lossFunction) {
		if (lossFunction == null) throw new RuntimeException("loss function not specified");
		
		if (!lossFunction.contains(".")) {
			lossFunction = "edu.oregonstate.lossfunction." + lossFunction;
		}
		
		try {
			Class lossfunctionClass = Class.forName(lossFunction);
			ILossFunction mlossFunction = (ILossFunction) lossfunctionClass.newInstance();
			return mlossFunction;
		} catch (Exception e) {
			throw new RuntimeException("e");
		}
	}
	
	/**
	 * create a search method
	 * 
	 * @param searchMethod
	 */
	public static ISearch createSearchMethod(String searchMethod) {
		if (searchMethod == null) throw new RuntimeException("search method not specified");
		
		if (!searchMethod.contains(".")) {
			searchMethod = "edu.oregonstate.search." + searchMethod;
		}
		
		try {
			Class searchMethodClass = Class.forName(searchMethod);
			ISearch SearchMethod = (ISearch) searchMethodClass.newInstance();
			return SearchMethod;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * create a clustering method
	 * 
	 * @param clusteringModel
	 */
	public static IClustering createClusteringModel(String clusteringModel) {
		if (clusteringModel == null) throw new RuntimeException("clustering method not specified");
		
		if (!clusteringModel.contains(".")) {
			clusteringModel = "edu.oregonstate.cluster" + clusteringModel;
		}
		
		try {
			Class clusteringClass = Class.forName(clusteringModel);
			IClustering ClusteringModel = (IClustering) clusteringClass.newInstance();
			return ClusteringModel;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * print debug information for topic
	 * 
	 * @param document
	 * @param topic
	 */
	public static void printParameters(Document document, String topic) {
		ResultOutput.writeTextFile(logFile, "Number of Gold Mentions of " + topic +  " : " + document.allGoldMentions.size());
		ResultOutput.writeTextFile(logFile, "Number of predicted Mentions of " + topic +  " : " + document.allPredictedMentions.size());
		ResultOutput.writeTextFile(logFile, "Number of gold clusters of " + topic + " : " + document.goldCorefClusters.size());
		ResultOutput.writeTextFile(logFile, "Gold clusters : \n" + ResultOutput.printCluster(document.goldCorefClusters));
		ResultOutput.writeTextFile(logFile, "Number of coref clusters of " + topic + " : " + document.corefClusters.size());
		ResultOutput.writeTextFile(logFile, "Coref Clusters: \n" + ResultOutput.printCluster(document.corefClusters));
	}
	
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
		
		IDataSet mDatasetMode;
		
		if (dataSetMode) {
			mDatasetMode = createDataSetModel("CrossTopic");
		} else {
			mDatasetMode = createDataSetModel("WithinCross");
		}
		
		String corpusStatisticsPath = experimentResultFolder + "/corpusStatisticsPath";
		
		if (stanfordExperiment) {
			Train.currentOutputFileName = linearRegressionTrainingPath + "/initial.csv";
		}
		
		// training set
		for(String topic : trainingTopics) {
			
			currentExperimentFolder = experimentResultFolder + "/" + topic;
			Command.createDirectory(currentExperimentFolder);
			
			ResultOutput.writeTextFile(logFile, "create training data set for " + topic);
			ResultOutput.writeTextFile(logFile, "\n");
			
			String[] tops = {topic};
			Document document = mDatasetMode.getData(tops);
			
			// create the training examples
			if (stanfordExperiment) {
				ResultOutput.writeTextFile(logFile, "create verb training examples for " + topic+ "................");
				
				// for verb pair case, noun pair case is generating during seven high precision sieves
				trainingVerbPairExample(document, Train.currentOutputFileName);
			}

			ResultOutput.writeTextFile(logFile, "number of gold mentions : " + document.allGoldMentions.size());
			ResultOutput.writeTextFile(logFile, "number of predicted mentions : " + document.allPredictedMentions.size());
			ResultOutput.writeTextFile(corpusStatisticsPath, topic + " " + document.allGoldMentions.size() + " " + document.goldCorefClusters.size() + " " + document.allPredictedMentions.size() + " " +
					document.corefClusters.size());
			
			totalGoalMentions += document.allGoldMentions.size();
			totalPredictedMentions += document.allPredictedMentions.size();
			ResultOutput.serialize(document, topic, serializedOutput);
			ResultOutput.writeTextFile(logFile, "\n");
		}
		
		Train.currentOutputFileName = "";
		
		// validation set
		if (tuningParameter) {
			for (String topic : developmentTopics) {
				ResultOutput.writeTextFile(logFile, "create validation data set for " + topic);
				ResultOutput.writeTextFile(logFile, "\n");
				currentExperimentFolder = experimentResultFolder + "/" + topic;
				Command.createDirectory(currentExperimentFolder);
				
				String[] tops = {topic};
				Document document = mDatasetMode.getData(tops);
				ResultOutput.writeTextFile(logFile, "number of gold mentions : " + document.allGoldMentions.size());
				ResultOutput.writeTextFile(logFile, "number of predicted mentions : " + document.allPredictedMentions.size());
				
				ResultOutput.writeTextFile(corpusStatisticsPath, topic + " " + document.allGoldMentions.size() + " " + document.goldCorefClusters.size() + " " + document.allPredictedMentions.size() + " " +
						document.corefClusters.size());
				totalPredictedMentions += document.allPredictedMentions.size();
				totalGoalMentions += document.allGoldMentions.size();
				ResultOutput.serialize(document, topic, serializedOutput);
				
				ResultOutput.writeTextFile(logFile, "\n");
			}
		}
		
		// testing set
		for (String topic : testingTopics) {
			
			ResultOutput.writeTextFile(logFile, "create testing data set for " + topic);
			ResultOutput.writeTextFile(logFile, "\n");
			currentExperimentFolder = experimentResultFolder + "/" + topic;
			Command.createDirectory(currentExperimentFolder);
			
			String[] tops = {topic};
			Document document = mDatasetMode.getData(tops);
			ResultOutput.writeTextFile(logFile, "number of gold mentions : " + document.allGoldMentions.size());
			ResultOutput.writeTextFile(logFile, "number of predicted mentions : " + document.allPredictedMentions.size());
			
			ResultOutput.writeTextFile(corpusStatisticsPath, topic + " " + document.allGoldMentions.size() + " " + document.goldCorefClusters.size() + " " + document.allPredictedMentions.size() + " " +
					document.corefClusters.size());
			totalPredictedMentions += document.allPredictedMentions.size();
			totalGoalMentions += document.allGoldMentions.size();
			ResultOutput.serialize(document, topic, serializedOutput);
			
			ResultOutput.writeTextFile(logFile, "\n");
		}
		// total mentions : 7980
		
		ResultOutput.writeTextFile(logFile, "the total number of gold mentions :" + totalGoalMentions );
		ResultOutput.writeTextFile(logFile, "the total number of predicted mentions :" + totalPredictedMentions );
		ResultOutput.writeTextFile(corpusStatisticsPath, totalGoalMentions + " " + totalPredictedMentions);
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
	    		Counter<String> features = Feature.getFeatures(document, ci, cj, false, ExperimentConstructor.mdictionary); // get the feature
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
	 * print objects
	 * 
	 * @param objects
	 * @return
	 */
	protected String buildString(Object[] objects) {
		StringBuffer sb = new StringBuffer();
		assert objects.length > 0;
		sb.append("[ ");
		for (int i = 0; i < objects.length; i++) {
			Object object = objects[i];
			if (i == objects.length - 1) {
				sb.append(object);
			} else {
				sb.append(object + ", ");
			}
		}
		sb.append(" ]");
		return sb.toString();
	}
	
	/** configure the WORDNET */
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
	
	/** configure word similarity matrix */
	protected void configureWordSimilarity() {
		WordSimilarity wordSimilarity = new WordSimilarity(WORD_SIMILARITY_PATH);
		wordSimilarity.initialize();
		datas = wordSimilarity.datas;
	}
	
	protected void printConfiguration() {
		
	}
	
	/**
	 * calculate weights
	 * 
	 * @param weights
	 */
	protected void calcualateWeightDifference(List<double[]> weights) {
		for (int i = 0; i < weights.size() - 1; i++) {
			double[] differences = DoubleOperation.minus(weights.get(i), weights.get(i+1));
			double sum = 0.0;
			for (double difference : differences) {
				sum += difference * difference;
			}
			double length = Math.sqrt(sum);
			ResultOutput.writeTextFile(weightFile, length + "");
		}
	}
	
	public static void printScoreSummary(String summary, boolean afterPostProcessing) {
		String[] lines = summary.split("\n");
		if(!afterPostProcessing) {
			for(String line : lines) {
				if(line.startsWith("Identification of Mentions")) {
					ResultOutput.writeTextFile(logFile, line);
					return;
				}
			}
		} else {
			StringBuilder sb = new StringBuilder();
			for(String line : lines) {
				if(line.startsWith("METRIC")) sb.append(line);
				if(!line.startsWith("Identification of Mentions") && line.contains("Recall")) {
					sb.append(line).append("\n");
				}
			}
			ResultOutput.writeTextFile(logFile, sb.toString());
		}
	}
	
	/** Print average F1 of MUC, B^3, CEAF_E */
	protected void printFinalScore(String summary) {
		Pattern f1 = Pattern.compile("Coreference:.*F1: (.*)%");
		Matcher f1Matcher = f1.matcher(summary);
		double[] F1s = new double[5];
		int i = 0;
		while (f1Matcher.find()) {
			F1s[i++] = Double.parseDouble(f1Matcher.group(1));
		}
		ResultOutput.writeTextFile(logFile, "Final score ((muc+bcub+ceafe)/3) = "+(F1s[0]+F1s[1]+F1s[3])/3);
	}
	
	/**
	 * update the corefcluster ID of each mention in the orderedPredictionMentions
	 * 
	 * @param document
	 */
	public static void updateOrderedPredictedMentions(Document document) {
		List<List<Mention>> predictedOrderedMentionsBySentence = document.getOrderedMentions();
		Map<Integer, CorefCluster> corefClusters = document.corefClusters;
		for (Integer clusterID : corefClusters.keySet()) {
			CorefCluster cluster = corefClusters.get(clusterID);
			for (Mention m : cluster.getCorefMentions()) {
				int sentenceID = m.sentNum;
				List<Mention> mentions = predictedOrderedMentionsBySentence.get(sentenceID);
				int mStartIndex = m.startIndex;
				int mEndIndex = m.endIndex;
				for (Mention mention : mentions) {
					int mentionStartIndex = mention.startIndex;
					int mentionEndIndex = mention.endIndex;
					if (mentionStartIndex == mStartIndex && mentionEndIndex == mEndIndex) {
						mention.corefClusterID = m.corefClusterID;
					}
				}
				
				int mentionID = m.mentionID;
				Mention correspondingMention = document.allPredictedMentions.get(mentionID);
				correspondingMention.corefClusterID = clusterID;
			}
		}
	}
	
}
