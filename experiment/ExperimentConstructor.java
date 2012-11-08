package edu.oregonstate.experiment;


import java.io.FileInputStream;
import java.util.*;

import net.didion.jwnl.JWNL;

import edu.oregonstate.classifier.Classifier;
import edu.oregonstate.cluster.Clustering;
import edu.oregonstate.costfunction.CostFunction;
import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.lossfunction.LossFunction;
import edu.oregonstate.search.Search;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;

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
	
	/** total topics */
	protected static String[] topics = {"1", "2", "3", "4", "5", "6", "7", "8", "9", 
		  								"10", "11", "12", "13", "14", "16", "18", "19", 
		  								"20", "21", "22", "23", "24", "25", "26", "27", "28", "29", 
		  								"30", "31", "32", "33", "34", "35", "36", "37", "38", "39", 
		  								"40", "41", "42", "43", "44", "45"};
	
	/** training topics */
	protected String[] trainingTopics;
	
	/** for mention words feature */
	public static Map<String, List<String>> datas;
	
	/** testing topics */
	protected String[] testingTopics;
	
	/** training set */
	protected List<Document> trainingSet;
	
	/** testing set */
	protected List<Document> testingSet;
	
	/** create dataset model, for example, whether need to incorporate the SRL result */	
	protected IDataSet mDatasetMode;
	
	/** classification model */
	protected Classifier mclassifier;
	
	/** cost function */
	protected CostFunction mCostFunction;
	
	/** loss function */
	protected LossFunction mLossFunction;
	
	/** search method */
	protected Search mSearchMethod;
	
	/** clustering method */
	protected Clustering mClusteringModel;
	
	/** parameter for different components, value can be any type */
	public static Map<String, Map<String, Object>> mParameters;
	
	public static Object getParameter(String methodKey, String parameterKey) {
		return mParameters.get(methodKey).get(parameterKey);
	}
	
	/** whether debug or run the whole experiment */
	protected boolean mDebug;
	
	/** initialize the Parameter */
	public ExperimentConstructor() {
		mParameters = new HashMap<String, Map<String, Object>>();
		trainingSet = new ArrayList<Document>();
		testingSet = new ArrayList<Document>();
	}
	
	protected void setDebugMode(boolean debugMode) {
		mDebug = debugMode;
	}
	
	
	/** all subclass need to implement the train method */
	protected abstract void performExperiment();
	
	protected IDataSet createDataSet(String datasetModel) {
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
	protected Classifier createClassifier(String classficationModel) {
		if (classficationModel == null) throw new RuntimeException("classifier not specified");
		
		if (!classficationModel.contains(".")) {
			classficationModel = "edu.oregonstate.classifier." + classficationModel;
		}
		
		try{
			Class classifierClass = Class.forName(classficationModel);
			Classifier classifier = (Classifier) classifierClass.newInstance();
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
	protected CostFunction createCostFunction(String costfunction) {
		if (costfunction == null) throw new RuntimeException("cost function not specified");
		
		if (!costfunction.contains(".")) {
			costfunction = "edu.oregonstate.costfunction." + costfunction;
		}
		
		try {
			Class costfunctionClass = Class.forName(costfunction);
			CostFunction costFunction = (CostFunction) costfunctionClass.newInstance();
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
	protected LossFunction createLostFunction(String lossFunction) {
		if (lossFunction == null) throw new RuntimeException("loss function not specified");
		
		if (!lossFunction.contains(".")) {
			lossFunction = "edu.oregonstate.lossfunction." + lossFunction;
		}
		
		try {
			Class lossfunctionClass = Class.forName(lossFunction);
			LossFunction mlossFunction = (LossFunction) lossfunctionClass.newInstance();
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
	protected Search createSearchMethod(String searchMethod) {
		if (searchMethod == null) throw new RuntimeException("search method not specified");
		
		if (!searchMethod.contains(".")) {
			searchMethod = "edu.oregonstate.search" + searchMethod;
		}
		
		try {
			Class searchMethodClass = Class.forName(searchMethod);
			Search SearchMethod = (Search) searchMethodClass.newInstance();
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
	protected Clustering createClusteringModel(String clusteringModel) {
		if (clusteringModel == null) throw new RuntimeException("clustering method not specified");
		
		if (!clusteringModel.contains(".")) {
			clusteringModel = "edu.oregonstate.cluster" + clusteringModel;
		}
		
		try {
			Class clusteringClass = Class.forName(clusteringModel);
			Clustering ClusteringModel = (Clustering) clusteringClass.newInstance();
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
	 * write the information to the output file
	 */
	public void printParameters() {
		
	}
	
	/** split the topics */
	protected void splitTopics(int index) {
		trainingTopics = new String[index];
		testingTopics = new String[topics.length - index]; 
		for (int i = 0; i < topics.length; i++) {
			if (i < index) {
				trainingTopics[i] = topics[i];
			} else {
				testingTopics[i - index] = topics[i];
			}
		}
	}
	
	/**
	 * create the training set and testing set according to topics
	 */
	protected void createDataSet() {
		// training set
		for(String topic : trainingTopics) {
			String[] tops = {topic};
			Document document = mDatasetMode.getData(tops);
			trainingSet.add(document);
		}
		
		// testing set
		for (String topic : testingTopics) {
			String[] tops = {topic};
			Document document = mDatasetMode.getData(tops);
			testingSet.add(document);
		}
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
}
