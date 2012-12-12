package edu.oregonstate.experiment.stanfordexperiment;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

import Jama.Matrix;

import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerHelper;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
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

	public StanfordExperiment(String configurationPath) {
		super(configurationPath);
	}
	
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
			ResultOutput.deleteResult(mentionRepositoryPath);
		}
		
		ResultOutput.printTime();
	}
	
	public static void main(String[] args) {
		String configurationPath = "/scratch/JavaFile/stanford-corenlp-2012-05-22/src/edu/oregonstate/experimentconfigs/flat-stanford-gold.properties";
		StanfordExperiment sge = new StanfordExperiment(configurationPath);
		sge.performExperiment();
	}
}
