package edu.oregonstate.experiment.crosscoreferenceresolution;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.training.Development;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;

/**
 * Experiment Description (according to the paper: Joint Entity and Event Coreference Resolution across Documents)
 * 
 * The framework of the experiment is Learning as Search Optimization. The main approach of the experiment is to do search.
 * During search step, update weight if there is violated constraint. Here is the detail steps for the framework
 * 
 * 1. Data Generation : According the Stanford's experiment, extract all mentions in one doc clustering. So, 
 * all documents are put together to form a topic, which is also represented as a Document, create singleton 
 * clusters for each mention, and then use the high precision sieves to do coreference resolution on all singleton 
 * clusters of a specific topic. The output of this step is coref clusters produced by coreference resolution.
 * 2. Search : Use the specified search method, such as Beam Search, to conduct search on the Stanford's output, 
 * In the current time, the action is just to merge two clusters. During search, update weight when there is a 
 * violated constraint In our case, we have two constraints, including pairwise and singleton constraints.
 * 3. Cost function : cost function is a linear function, $w \cdot x$
 * 4. Loss function : use Pairwise score as a loss score. 
 * 5. classification method : just use structured perceptron to update the weight. There are two constraints, one is 
 * singleton, the other one is pairwise.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CrossCoreferenceResolution extends ExperimentConstructor {

	/**
	 * define the configuration file path and instantiate the property which can be used
	 * in the whole experiment
	 * 
	 * @param configurationPath
	 */
	public CrossCoreferenceResolution(String configurationPath) {
		super(configurationPath);
	}
	
	@Override
	protected void performExperiment() {
		// set parameters
		int epoch = Integer.parseInt(property.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP));
		int numberOfFeatures = features.length;
		double[] weight = new double[numberOfFeatures];
		double[] totalWeight = new double[numberOfFeatures];
		int totalViolations = 0;
		boolean trainTesting = Boolean.parseBoolean(property.getProperty(EecbConstants.TRAINING_VALIDATION_PROP));
		String searchMethod = property.getProperty(EecbConstants.SEARCH_PROP);
		boolean experimentWeight = Boolean.parseBoolean(property.getProperty(EecbConstants.WEIGHT_PROP));    // true average weight, false latest weight
		double[] learningRates = DoubleOperation.createDescendingArray(1.0, 0.0, epoch);
		String classifierLearningModel = property.getProperty(EecbConstants.CLASSIFIER_PROP);   // classification model
		
		for (int i = 0; i < epoch; i++) {
			ResultOutput.writeTextFile(logFile, "The " + (i + 1) + "th iteration training....");
			int currentViolations = 0;
			learningRate = learningRates[i];
			
			// training part
			for (int j = 0; j < trainingTopics.length; j++) {
				String topic = trainingTopics[j];
				ResultOutput.writeTextFile(logFile, "Starting to do training on " + topic + " in the " + (i + 1) + " training phase" );
				Document document = ResultOutput.deserialize(topic, serializedOutput, false);
				
				// before search : document parameters
				ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
				printParameters(document, topic);
				
				// configure dynamic file and folder path
				currentExperimentFolder = experimentResultFolder + "/" + topic;
				Command.createDirectory(currentExperimentFolder);
				mscorePath = currentExperimentFolder + "/" + "train-iteration" + (i + 1) + "-" + topic;
				
				// use search to update weight
				ISearch mSearchMethod = createSearchMethod(searchMethod);
				mSearchMethod.setWeight(weight);
				mSearchMethod.setTotalWeight(totalWeight);
				mSearchMethod.setDocument(document);
				mSearchMethod.trainingSearch();
				weight = mSearchMethod.getWeight();
				totalWeight = mSearchMethod.getTotalWeight();
				currentViolations += mSearchMethod.getViolations();
				
				// after search : document parameters
				ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after search");
				printParameters(document, topic);
			}
				
			// print weight information
			ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
			ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
			ResultOutput.writeTextFile(logFile, "the number of violated constraints for the " + (i + 1) + "th iteration : " + currentViolations);
			ResultOutput.writeTextFile(violatedFile, currentViolations + "");
			totalViolations += currentViolations;
			ResultOutput.writeTextFile(logFile, "total violation : " + totalViolations + " until " + (i + 1) + "th iteration");
			
			/* pairwise: average weight is over the total number of violated constraints, while for 
			 * pairwise & singleton : average weight is over the number of iterations
			 * */
			double[] averageWeight;
			if (classifierLearningModel.startsWith("Structured")) {
				averageWeight = DoubleOperation.divide(totalWeight, totalViolations);
			} else {
				averageWeight = DoubleOperation.divide(totalWeight, (i + 1));
			}
			
			ResultOutput.writeTextFile(logFile, "average weight vector : " + DoubleOperation.printArray(averageWeight));
			ResultOutput.writeTextFile(logFile, "\n");
			
			// average weight or current weight
			double[] learnedWeight;
			if (experimentWeight) {
				learnedWeight = averageWeight;
			} else {
				learnedWeight = weight;
			}
			
			// do tuning parameter, focus on stopping rate
			if (tuningParameter) {
				Development development = new Development(developmentTopics, i, learnedWeight, 1.0, 3.0);
				development.tuning();
				stoppingRate = development.getStoppingRate();
				ResultOutput.writeTextFile(logFile, "After tuning (1.0 - 3.0), the stopping rate is :" + stoppingRate + " for the " + (i + 1) + " iteration");
			}
			
			// validation part
			if (trainTesting) {
				String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-validation-" + (i + 1);
				String goldCorefCluster = conllResultPath + "/goldCorefCluster-validation-" + (i + 1);
				
				PrintWriter writerPredicted = null;
				PrintWriter writerGold = null;
				try {
					writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
					writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				
				for (int j = 0; j < trainingTopics.length; j++) {
					String topic = trainingTopics[j];
					ResultOutput.writeTextFile(logFile, "Training Testing : Starting to do testing on " + topic);
					Document document = ResultOutput.deserialize(topic, serializedOutput, false);

					// before search parameters
					ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
					printParameters(document, topic);

					// configure dynamic file and folder path
					currentExperimentFolder = experimentResultFolder + "/" + topic;
					Command.createDirectory(currentExperimentFolder);
					mscorePath = currentExperimentFolder + "/" + "traintest-iteration" + (i + 1) + "-" + topic;

					// use search to update weight
					ISearch mSearchMethod = createSearchMethod(searchMethod);
					mSearchMethod.setWeight(learnedWeight);
					mSearchMethod.setDocument(document);
					mSearchMethod.testingSearch();

					updateOrderedPredictedMentions(document);
					
					// after search parameters
					ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after search");
					printParameters(document, topic);
					
					// do pronoun sieve on the document
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
				    SieveCoreferenceSystem.printConllOutput(document, writerPredicted, false, postProcess);
				    SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
				}
				
				try {
					ResultOutput.writeTextFile(logFile, "\n\n");
					ResultOutput.writeTextFile(logFile, "the score summary of resolution for train testing on the " + (i + 1) + "th iteration");
					String summary = SieveCoreferenceSystem.getConllEvalSummary(conllScorerPath, goldCorefCluster, predictedCorefCluster);
					printScoreSummary(summary, true);
					printFinalScore(summary);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				
				writerPredicted.close();
				writerGold.close();
			}
			
			
			// testing part
			String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-testing-" + (i + 1);
			String goldCorefCluster = conllResultPath + "/goldCorefCluster-testing-" + (i + 1);
			
			PrintWriter writerPredicted = null;
			PrintWriter writerGold = null;
			try {
				writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
				writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			for (int j = 0; j < testingTopics.length; j++) {
				String topic = testingTopics[j];
				ResultOutput.writeTextFile(logFile, "Starting to do testing on " + topic + " for the " + (i + 1) + "th iteration");
				Document document = ResultOutput.deserialize(topic, serializedOutput, false);
				
				// before search parameters
				ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
				printParameters(document, topic);
				
				// configure dynamic file and folder path
				currentExperimentFolder = experimentResultFolder + "/" + topic;
				Command.createDirectory(currentExperimentFolder);
				mscorePath = currentExperimentFolder + "/" + "test-iteration" + (i + 1) + "-" + topic;
				
				// use search to do testing
				ISearch mSearchMethod = createSearchMethod(searchMethod);
				mSearchMethod.setWeight(learnedWeight);
				mSearchMethod.setDocument(document);
				mSearchMethod.testingSearch();
				
				updateOrderedPredictedMentions(document);
				
				// after search parameters
				ResultOutput.writeTextFile(logFile, topic +  "'s detail after search");
				printParameters(document, topic);
				
				// do pronoun sieve on the document
				try {
			    	DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
				    CorefSystem cs = new CorefSystem();
			    	cs.corefSystem.coreference(document, pronounSieve);
			    } catch (Exception e) {
			    	throw new RuntimeException(e);
			    }
				
				// do post-process
				if (postProcess) {
			    	SieveCoreferenceSystem.postProcessing(document);
			    }
				
			    SieveCoreferenceSystem.printConllOutput(document, writerPredicted, false, postProcess);
			    SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
			}
			
			try {
				ResultOutput.writeTextFile(logFile, "\n\n");
				ResultOutput.writeTextFile(logFile, "the score summary of resolution for testing on the " + (i + 1) + "th iteration");
				String summary = SieveCoreferenceSystem.getConllEvalSummary(conllScorerPath, goldCorefCluster, predictedCorefCluster);
				printScoreSummary(summary, true);
				printFinalScore(summary);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			writerGold.close();
			writerPredicted.close();
		}
		
		// delete serialized objects and mention result
		ResultOutput.deleteResult(serializedOutput);
		if (goldOnly) {
			ResultOutput.deleteResult(mentionRepositoryPath);
		}

		ResultOutput.printTime();
	}
	
	/**
	 * Needs the following properties:
	 *  -props 'Location of coref.properties'
	 * @throws Exception
	 */
	public static void main(String[] args) {
		//TODO
		boolean debug = false;
		String configurationPath = "";
		if (debug) {
			configurationPath = "/scratch/JavaFile/stanford-corenlp-2012-05-22/src/edu/oregonstate/experimentconfigs/pairwise-flat-tuning/" +
								"debug-predicted-flat-oregonstatement-pairwise-tuning-postprocess-pairwise.properties";
		} else {
			if (args.length > 1) {
				System.out.println("there are more parameters, you just can specify one path parameter.....");
				System.exit(1);
			}
			
			configurationPath = args[0];
		}
		
		CrossCoreferenceResolution ccr = new CrossCoreferenceResolution(configurationPath);
		ccr.performExperiment();
	}
}
