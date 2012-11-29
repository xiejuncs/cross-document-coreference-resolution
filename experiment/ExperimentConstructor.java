package edu.oregonstate.experiment;


import java.io.FileInputStream;
import java.util.*;

import net.didion.jwnl.JWNL;

import edu.oregonstate.CorefSystem;
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

/**
 * 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class ExperimentConstructor {

	/**
	 * total number of topics: 43
	 * {"1", "2", "3", "4", "5", "6", "7", "8", "9"}   9
	 * {"10", "11", "12", "13", "14", "16", "18", "19"}   8
	 * {"20", "21", "22", "23", "24", "25", "26", "27", "28", "29"}  10
	 * {"30", "31", "32", "33", "34", "35", "36", "37", "38", "39"}  10
	 * {"40", "41", "42", "43", "44", "45"}  6
	 */
	
	/** total topics for running all experiments */
	protected static String[] stanfordTotalTopics = {"5", "6", "8", "11", "16", "25", "30", "31", "37", "38", "40", "43", "44", 
		"1", "2", "3", "4", "7", "9", "10", "13", "14", "18", "19", "20", "21", "23", "24", "26", "27", "28", "29", "32", "33", "34", "35", "36", "39", "41", "42", "45"};
	
	protected static String[] totalTopics = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", 
											 "11", "12", "13", "14", "16", "18", "19", "20", "21", "22", 
											 "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", 
											 "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", 
											 "43", "44", "45"};
	
	// for debug
	protected static String[] debugTopics = {"1", "38", "3", "20"};
	
	
	// for experiment case
	protected static String[] experimentTopics;
	
	/** experiment folder */
	public static String experimentResultFolder;
	
	/** corpus path */
	public static String corpusPath;
	
	/** store the mention result */
	public static String mentionResultPath;
	
	/** whether output feature for violated case */
	public static boolean outputFeature;
	
	/** whether output text for producing the SRL annotation */
	public static boolean outputText;
	
	/** Reading text when there is numm pointer */
	public static boolean enableNull;
	
	/** current experiment folder, output action information for future use */
	public static String currentExperimentFolder;
	
	/** whether normalize weight */
	public static boolean normalizeWeight;
	
	/** log file */
	public static String logFile;
	
	/** just output the train and test result, for example pairwise  */
	public static String mscorePath;
	
	/** output the detail information of each score, which can be used to calculate the total performance of all topics */
	public static String mScoreDetailPath;
	
	public static boolean stoppingCriterion;
	
	/** score Types */
	String[] scoreTypes = {"Pairwise", "MUC", "Bcubed", "CEAF"};
	
	/** muc detail information */
	public static String mMUCScoreDetailPath;
	
	/** Bcubed detail information */
	public static String mBcubedScoreDetailPath;
	
	/** ceaf detail information */
	public static String mCEAFScoreDetailPath;
	
	/** training topics */
	protected String[] trainingTopics;
	
	/** current corpus statistics */
	public static String currentCorpusStatistics;
	
	/** for mention words feature */
	public static Map<String, List<String>> datas;
	
	/** testing topics */
	protected String[] testingTopics;
	
	/** object file */
	public static String serializedOutput;
	
	/** create dataset model, for example, whether need to incorporate the SRL result */
	protected IDataSet mDatasetMode;
	
	/** parameter for different components, value can be any type */
	public static Map<String, Map<String, Object>> mParameters;
	
	/** whether to incorporate the SRL result */
	public static boolean incorporateTopicSRLResult;
	
	/** whether to incorporate the document srl result */
	public static boolean incorporateDocumentSRLResult;
	 
	/** just deal with gold mention cases */
	public static boolean goldOnly;
	
	/** dictionary used for creating features */
	public static Dictionaries mdictionary;
	
	/** violated file */
	protected String violatedFile;
	
	/** store the intermediate weights produced by the algorithm */
	protected String weightFile;
	
	/** stopping rate */
	public static double stoppingRate;
	
	/** whether update weight, training phase set as true, validation and testing phase set as false */
	public static boolean updateWeight;
	
	/** the folder path store the training set produced during the training phase */
	public static String linearRegressionTrainingPath;
	
	/** whether we need to post-process the document, apply for the predicted mentions */
	public static boolean postProcess;
	
	/** whether the current experiment is Stanford experiment */
	public static boolean stanfordExperiment;
	
	/** generate features for the experiment */
	public static boolean oregonStateExperiment = true;
	
	/** print final score */
	protected void printFinalScore(int iteration) {
		FinalScore finalScore = new FinalScore(trainingTopics, testingTopics, experimentResultFolder);
		for (int i = 1; i <= iteration; i++) {
			for (String scoreType : scoreTypes) {
				finalScore.set(i);
				finalScore.setScoreType(scoreType);
				finalScore.computePerformance();
			}
		}
	}
	
	/** get parameter */
	public static Object getParameter(String methodKey, String parameterKey) {
		
		
		if (mParameters == null) {
			return null;
		} else {
			return mParameters.get(methodKey).get(parameterKey);
		}
	}
	
	/** whether debug or run the whole experiment */
	protected boolean mDebug;
	
	/** initialize the Parameter */
	public ExperimentConstructor() {
		mParameters = new HashMap<String, Map<String, Object>>();
		stanfordExperiment = false;
	}
	
	/** set debug mode */
	protected void setDebugMode(boolean debugMode) {
		mDebug = debugMode;
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
	 * add parameter to the parameters
	 * 
	 * Through explicit add parameter to the parameters, it is easy to pinpoint the bug
	 * 
	 * @param methodkey
	 * @param parameterKey
	 * @param parameterValue
	 */
	protected void addParas(String methodkey, String parameterKey, Object parameterValue) {
		boolean containMethod = mParameters.containsKey(methodkey);
		if (!containMethod) {
			mParameters.put(methodkey, new HashMap<String, Object>());
		}
		
		mParameters.get(methodkey).put(parameterKey, parameterValue);
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
		ResultOutput.writeTextFile(logFile, "Numver of coref clusters of " + topic + " : " + document.corefClusters.size());
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
		
		String corpusStatisticsPath = experimentResultFolder + "/corpusStatisticsPath";
		
		if (stanfordExperiment) {
			Train.currentOutputFileName = linearRegressionTrainingPath + "/initial.csv";
		}
		
		// training set
		for(String topic : trainingTopics) {
			
			currentExperimentFolder = experimentResultFolder + "/" + topic;
			Command.createDirectory(currentExperimentFolder);
			
			ResultOutput.writeTextFile(logFile, "create data set for " + topic);
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
		
		// testing set
		for (String topic : testingTopics) {
			
			ResultOutput.writeTextFile(logFile, "create testing data set for " + topic);
			ResultOutput.writeTextFile(logFile, "\n");
			currentExperimentFolder = experimentResultFolder + "/" + topic;
			Command.createDirectory(currentExperimentFolder);
			
			String[] tops = {topic};
			Document document = mDatasetMode.getData(tops);
			ResultOutput.writeTextFile(logFile, "number of gold mentions : " + document.allGoldMentions.size());
			ResultOutput.writeTextFile(logFile, "number of mentions : " + document.allPredictedMentions.size());
			
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
						ResultOutput.writeTextFile(logFile, "training example " + ci.clusterID + " " + cj.clusterID);
						
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
			JWNL.initialize(new FileInputStream((String) getParameter(EecbConstants.DATASET, "wordnetConfigurationPath")));
			System.out.println("finish configure WORDNET");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
	/** configure word similarity matrix */
	protected void configureWordSimilarity() {
		WordSimilarity wordSimilarity = new WordSimilarity((String) getParameter(EecbConstants.DATASET, "wordsimilaritypath"));
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
	
}
