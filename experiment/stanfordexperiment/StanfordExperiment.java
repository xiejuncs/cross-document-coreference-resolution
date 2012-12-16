package edu.oregonstate.experiment.stanfordexperiment;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import Jama.Matrix;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;

/**
 * Here, I just output the value, and then use the scorer program to run the scoring 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class StanfordExperiment extends ExperimentConstructor {

	public StanfordExperiment(String configurationPath) {
		super(configurationPath);
	}

	@Override
	protected void performExperiment() {

		// get the parameters
		int noOfFeature = features.length;
		
		// training part
		ResultOutput.writeTextFile(logFile, "Start to do training on training topics....");
		Matrix model = new Matrix(noOfFeature + 1, 1);
		Train train = new Train(trainingTopics);
		Matrix initialmodel = train.assignInitialWeights();    // train initial model
		model = train.train(initialmodel);                     // based on the initial model, train the final model
		ResultOutput.writeTextFile(logFile, "final weight: " + ResultOutput.printModel(model, features));
		
		String predictedCorefClusterWithoutPronoun = experimentResultFolder + "/predictedCorefClusterWithoutPronoun";
		String predictedCorefClusterWithPronoun = experimentResultFolder + "/predictedCorefClusterWithPronoun";
		String goldCorefCluster = experimentResultFolder + "/goldCorefCluster";
		PrintWriter writerPredictedWithoutPronoun = null;
		PrintWriter writerPredictedWithPronoun = null;
		PrintWriter writerGold = null;
		try {
			writerPredictedWithoutPronoun = new PrintWriter(new FileOutputStream(predictedCorefClusterWithoutPronoun));
			writerPredictedWithPronoun = new PrintWriter(new FileOutputStream(predictedCorefClusterWithPronoun));
			writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
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
	    	
		    if (postProcess) {
		    	SieveCoreferenceSystem.postProcessing(document);
		    }
		    SieveCoreferenceSystem.printConllOutput(document, writerPredictedWithoutPronoun, false, postProcess);
	    	
		    ResultOutput.writeTextFile(logFile, "topic " + topic + "'s after after merge");
			printParameters(document, topic);
		}
		
		writerPredictedWithoutPronoun.close();
		
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
		    
		    if (postProcess) {
		    	SieveCoreferenceSystem.postProcessing(document);
		    }
		    SieveCoreferenceSystem.printConllOutput(document, writerPredictedWithPronoun, false, postProcess);
		    SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
		    
		    ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after merge");
			printParameters(document, topic);
		}
		
		writerPredictedWithPronoun.close();
		writerGold.close();
		
		// do scoring
		try {
			ResultOutput.writeTextFile(logFile, "the score summary of resolution without pronoun resolution");
			String summaryWithoutPronoun = SieveCoreferenceSystem.getConllEvalSummary(conllScorerPath, goldCorefCluster, predictedCorefClusterWithoutPronoun);
			printScoreSummary(summaryWithoutPronoun, true);
			printFinalScore(summaryWithoutPronoun);
			
			ResultOutput.writeTextFile(logFile, "\n\n");
			ResultOutput.writeTextFile(logFile, "the score summary of resolution with pronoun resolution");
			String summaryWithPronoun = SieveCoreferenceSystem.getConllEvalSummary(conllScorerPath, goldCorefCluster, predictedCorefClusterWithPronoun);
			printScoreSummary(summaryWithPronoun, true);
			printFinalScore(summaryWithPronoun);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		// delete serialized objects and mention result
		ResultOutput.deleteResult(serializedOutput);
		if (goldOnly) {
			ResultOutput.deleteResult(mentionRepositoryPath);
		}
		
		ResultOutput.printTime();
	}
	
	public static void main(String[] args) {
		//TODO
		boolean debug = false;
		String configurationPath = "";
		if (debug) {
			configurationPath = "/scratch/JavaFile/stanford-corenlp-2012-05-22/src/edu/oregonstate/experimentconfigs/debug-hierarchy-stanford-predicted.properties";
		} else {
			if (args.length > 1) {
				System.out.println("there are more parameters, you just can specify one path parameter.....");
				System.exit(1);
			}
			
			configurationPath = args[0];
		}

		StanfordExperiment sge = new StanfordExperiment(configurationPath);
		sge.performExperiment();
	}
}
