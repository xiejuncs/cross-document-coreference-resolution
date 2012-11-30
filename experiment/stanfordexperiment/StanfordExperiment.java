package edu.oregonstate.experiment.stanfordexperiment;

import java.util.Calendar;
import java.util.logging.Logger;

import Jama.Matrix;

import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerHelper;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;

/**
 * Stanford's experiment
 * 
 * Experiment Configuration
 * 1. classifier: linear regression (iteration no: 10)
 * 2. debug : true
 * 
 * <p>
 * The dataset is EECB 1.0, which is annotated by the Stanford NLP group on the basis of ECB 
 * corpus created by Bejan and Harabagiu (2010).
 * The idea of the paper, Joint Entity and Event Coreference Resolution across Documents, 
 * is to model entity and event jointly in an iterative way.
 * 
 * <p>
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */

public class StanfordExperiment extends ExperimentConstructor {

	/** Logger used for scoring */
	public static final Logger logger = Logger.getLogger(StanfordExperiment.class.getName());
	
	/** aggregate the result */
	private Document beforePronounSieveCorpus;
	private Document afterPronounSieveCorpus;
	
	/**
	 * add four fields to the corpus
	 * 
	 * @param document
	 */
	public void add(Document document, Document corpus) {
		addCorefCluster(document, corpus);
		addGoldCorefCluster(document, corpus);
		addPredictedMention(document, corpus);
		addGoldMention(document, corpus);
	}
	
	/** add coref cluster */
	public void addCorefCluster(Document document, Document corpus) {
		  for(Integer id : document.corefClusters.keySet()) {
			  corpus.addCorefCluster(id, document.corefClusters.get(id));
		  }
	}
	
	/** add gold coref cluster */
	public void addGoldCorefCluster(Document document, Document corpus) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefCluster(id, document.goldCorefClusters.get(id));
		}
	}
	
	/** add predicted mentions */
	public void addPredictedMention(Document document, Document corpus) {
		for (Integer id : document.allPredictedMentions.keySet()) {
			corpus.addPredictedMention(id, document.allPredictedMentions.get(id));
		}
	}

	/** add gold mentions */
	public void addGoldMention(Document document, Document corpus) {
		for (Integer id : document.allGoldMentions.keySet()) {
			corpus.addGoldMention(id, document.allGoldMentions.get(id));
		}
	}
	
	@Override
	protected void performExperiment() {
		configureExperiment();
		beforePronounSieveCorpus = new Document();
		afterPronounSieveCorpus = new Document();
		
		// get the parameters
		int noOfFeature = Feature.featuresName.length;
		
		// training part
		ResultOutput.writeTextFile(logFile, "Start to do training on training topics....");
		Matrix model = new Matrix(noOfFeature + 1, 1);
		Train train = new Train(trainingTopics);
		Matrix initialmodel = train.assignInitialWeights();    // train initial model
		model = train.train(initialmodel);                     // based on the initial model, train the final model
		ResultOutput.writeTextFile(logFile, "final weight: " + ResultOutput.printModel(model, Feature.featuresName));
		
		// testing part
		// without pronoun sieve
		for (String topic : testingTopics) {
			ResultOutput.writeTextFile(logFile, "Starting to do testing on " + topic + " without pronuon sieve");
			Document document = ResultOutput.deserialize(topic, serializedOutput, false);
			
			// before search parameters
			ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before merge");
			printParameters(document, topic);
			
			JointCoreferenceResolution ir = new JointCoreferenceResolution(document, model);
	    	ir.merge();   	
		    
		    // add the four fields into the corpus data structure
		    add(document, beforePronounSieveCorpus);
		    ResultOutput.writeTextFile(logFile, "topic " + topic + "'s after after merge");
			printParameters(document, topic);
		}
		
		printParameters(beforePronounSieveCorpus, "the overall topics without pronoun sieve");
		ScorerHelper beforesh = new ScorerHelper(beforePronounSieveCorpus, logger, logFile, postProcess);
		beforesh.printScore();
		
		// with pronoun sieve
		for (String topic : testingTopics) {
			ResultOutput.writeTextFile(logFile, "Starting to do testing on " + topic + " with pronuon sieve");
			Document document = ResultOutput.deserialize(topic, serializedOutput, false);
			
			// before search parameters
			ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before merge");
			printParameters(document, topic);
			
			JointCoreferenceResolution ir = new JointCoreferenceResolution(document, model);
	    	ir.merge();   	

		    // pronoun sieves
		    try {
		    	DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
			    CorefSystem cs = new CorefSystem();
		    	cs.corefSystem.coreference(document, pronounSieve);
		    } catch (Exception e) {
		    	throw new RuntimeException(e);
		    }
		    
		    // add the four fields into the corpus data structure
		    add(document, afterPronounSieveCorpus);
		    
		    ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after merge");
			printParameters(document, topic);
		}
		
		printParameters(afterPronounSieveCorpus, "the overall topics with pronoun sieve");
		ScorerHelper sh = new ScorerHelper(afterPronounSieveCorpus, logger, logFile, postProcess);
		sh.printScore();
		
		// delete serialized objects and mention result
		ResultOutput.deleteResult(serializedOutput);
		if (goldOnly) {
			ResultOutput.deleteResult(mentionResultPath);
		}
		
		ResultOutput.printTime();
	}
	
	/** 
	 * Experiment Configuration
	 * classifier: linear regression (iteration no: 10)
	 * debug: true
	 * Dataset: gold mentions
	 * 
	 */
	private void configureExperiment() {
		// configure topics and dataSet
		setDebugMode(true);
		
		//TODO
		boolean debugExperiment = false;
		if (debugExperiment) {
			experimentTopics = debugTopics;
			splitTopics(2);
			corpusPath = "../";
		} else {
			experimentTopics = stanfordTotalTopics;
			splitTopics(12);
			corpusPath = "/nfs/guille/xfern/users/xie/Experiment/";
		}
		
		// define Data set
		addParas(EecbConstants.DATASET, "corpusPath", corpusPath + "corpus/EECB1.0/data/");
		addParas(EecbConstants.DATASET, "srlpath", corpusPath + "corpus/tokenoutput/");
		addParas(EecbConstants.DATASET, "sieve", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch");
		addParas(EecbConstants.DATASET, "annotationPath", corpusPath + "corpus/mentions.txt");
		addParas(EecbConstants.DATASET, "wordnetConfigurationPath", corpusPath + "corpus/file_properties.xml");
		addParas(EecbConstants.DATASET, "wordsimilaritypath", corpusPath + "corpus/sims.lsp");
		addParas(EecbConstants.DATASET, "outputPath", corpusPath + "corpus/TEMPORYRESUT/");
		addParas(EecbConstants.DATASET, "aligned", false);
		
		// define classifier parameter
		addParas(EecbConstants.CLASSIFIER, "noOfIteration", 10);
		addParas(EecbConstants.CLASSIFIER, "noOfFeature", Feature.featuresName.length);
		addParas(EecbConstants.CLASSIFIER, "model", "LinearRegression");
		addParas(EecbConstants.CLASSIFIER, "coefficient", 1.0);
		addParas(EecbConstants.CLASSIFIER, "interPolationWeight", 0.7);
		
		// configure Word Net and Lin's dictionary
		configureJWordNet();
		configureWordSimilarity();
		
		// get the according configuration parameters
		String classifierLearningModel = (String) getParameter(EecbConstants.CLASSIFIER, "model");
		int classifierNoOfIteration = (Integer) getParameter(EecbConstants.CLASSIFIER, "noOfIteration");
		
		// create a folder to contain all log information
		String outputPath = (String) getParameter(EecbConstants.DATASET, "outputPath");
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		//TODO
		goldOnly = true;
		if (goldOnly) {
			// create mention result folder to store the mention serialization object
			experimentResultFolder = outputPath + timeStamp + "-" + this.getClass().getSimpleName() + "-gold" + "-" + classifierLearningModel + "-" + classifierNoOfIteration;
			Command.createDirectory(experimentResultFolder);
			
			// store the mention serialization method
			mentionResultPath = experimentResultFolder + "/mentionResult";
			Command.createDirectory(mentionResultPath);
			postProcess = false;
		} else {
			experimentResultFolder = outputPath + timeStamp + "-" + this.getClass().getSimpleName() + "-predicted" + "-" + classifierLearningModel + "-" + classifierNoOfIteration;
			Command.createDirectory(experimentResultFolder);
			
			// post process the mentions 
			postProcess = true;
		}
		
		/** configure other parameters */
		logFile = experimentResultFolder + "/" + "experimentlog";
		weightFile = experimentResultFolder + "/weights";
		outputText = false;
		enableNull = false;
		incorporateTopicSRLResult = false;
		incorporateDocumentSRLResult = false;
		stanfordExperiment = true;

		normalizeWeight = false;
		outputFeature = false;
		linearRegressionTrainingPath = experimentResultFolder + "/trainingSet";
		Command.createDirectory(linearRegressionTrainingPath);
		
		// print configure information
		ResultOutput.printTime();
		ResultOutput.writeTextFile(logFile, "corpus path : " + corpusPath);
		ResultOutput.writeTextFile(logFile, "classification : " + classifierLearningModel + "-" + classifierNoOfIteration);
		ResultOutput.writeTextFile(logFile, buildString(Feature.featuresName));
		ResultOutput.writeTextFile(logFile, "experiment topics : " + buildString(experimentTopics));

		/** create document serialization folder which store document serialization object */
		serializedOutput = experimentResultFolder + "/documentobject";
		Command.createDirectory(serializedOutput);
		
		// define dataset model
		/** initialize the dictionary */
		CorefSystem cs = new CorefSystem();
		mdictionary = cs.corefSystem.dictionaries();
		
		mDatasetMode = createDataSetModel("CrossTopic");
		createDataSet();
	}
	
	public static void main(String[] args) {
		StanfordExperiment sge = new StanfordExperiment();
		sge.performExperiment();
	}
}
