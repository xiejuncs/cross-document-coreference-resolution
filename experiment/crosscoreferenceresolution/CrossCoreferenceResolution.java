package edu.oregonstate.experiment.crosscoreferenceresolution;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.CoNLLScorerHelper;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.search.State;
import edu.oregonstate.training.Development;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
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

	/* used for print the score */
	public static final Logger logger = Logger.getLogger(CrossCoreferenceResolution.class.getName());
	
	private double[] weight;
	private double[] totalWeight;
	private int totalViolations;
	private List<double[]> learnedWeights;
	
	/* configure the experiment */
	public CrossCoreferenceResolution(String configurationPath) {
		super(configurationPath);
	}
	
	/**
	 * learn the weight according to the true loss function
	 * 
	 * @param currentEpoch
	 * @param searchMethod
	 * @return
	 */
	private int trainingBySearch(int currentEpoch, String searchMethod) {
		// training part
		int currentViolations = 0;
		for (int j = 0; j < trainingTopics.length; j++) {
			ResultOutput.writeTextFile(logFile, "\n");
			String topic = trainingTopics[j];
			ResultOutput.writeTextFile(logFile, "Starting to do training on " + topic + " in the " + currentEpoch + "th training phase" );
			Document document = ResultOutput.deserialize(topic, serializedOutput, false);
			
			// before search : document parameters
			ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
			ResultOutput.printParameters(document, topic, logFile);
			
			// configure dynamic file and folder path
			currentExperimentFolder = experimentResultFolder + "/" + topic;
			Command.createDirectory(currentExperimentFolder);
			mscorePath = currentExperimentFolder + "/" + "train-iteration" + currentEpoch + "-" + topic;
			
			// use search to update weight
			ISearch mSearchMethod = EecbConstructor.createSearchMethod(searchMethod);
			mSearchMethod.setWeight(weight);
			mSearchMethod.setTotalWeight(totalWeight);
			mSearchMethod.setDocument(document);
			mSearchMethod.trainingSearch();
			weight = mSearchMethod.getWeight();
			totalWeight = mSearchMethod.getTotalWeight();
			currentViolations += mSearchMethod.getViolations();
			Document documentState = mSearchMethod.getDocument();
			ResultOutput.printDocumentScoreInformation(documentState, "document after search: ", logFile, logger);
			
			// after search : document parameters
			ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after search");
			ResultOutput.printParameters(documentState, topic, logFile);
		}
		return currentViolations;
	}
	
	/**
	 * train the model using the Dagger algorithm
	 * 
	 * @param currentEpoch
	 * @param searchMethod
	 * @param totalEpoch
	 * @param N
	 */
	private void trainUsingDagger(int currentEpoch, String searchMethod, int totalEpoch, int N) {
		boolean trainTesting = Boolean.parseBoolean(property.getProperty(EecbConstants.TRAINING_VALIDATION_PROP));
		String classifierLearningModel = property.getProperty(EecbConstants.CLASSIFIER_PROP);   // classification model
		List<double[]> leanredWeights = new ArrayList<double[]>();
		int currentViolations = trainingBySearch(currentEpoch, searchMethod);
		// print weight information
		ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
		ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
		ResultOutput.writeTextFile(logFile, "the number of violated constraints for the " + currentEpoch + "th iteration : " + currentViolations);
		ResultOutput.writeTextFile(violatedFile, currentViolations + "");
		totalViolations += currentViolations;
		ResultOutput.writeTextFile(logFile, "total violation : " + totalViolations + " until " + currentEpoch + "th iteration");
		double[] averageWeight;
		if (classifierLearningModel.startsWith("Structured")) {
			averageWeight = DoubleOperation.divide(totalWeight, totalViolations);
		} else {
			averageWeight = DoubleOperation.divide(totalWeight, currentEpoch);
		}
		
		ResultOutput.writeTextFile(logFile, "average weight vector : " + DoubleOperation.printArray(averageWeight));
		ResultOutput.writeTextFile(logFile, "\n");
		leanredWeights.add(averageWeight);
		
		for (int i = 0; i < totalEpoch; i++ ) {
			for (int j = 0; j < N; j++) {
				double[] learnedWeight = learnedWeights.get(i);
				
				postProcess = trainPostProcess;
				
				// validation part
				if (trainTesting) {
					doTesting(currentEpoch, learnedWeight, searchMethod, "trainTesting", trainingTopics);
				}
				
				postProcess = testPostProcess;
				doTesting(currentEpoch, learnedWeight, searchMethod, "testing", testingTopics);
				
				currentViolations = trainingByFile(currentEpoch, featurePath);
				
				ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
				ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
				ResultOutput.writeTextFile(logFile, "the number of violated constraints for the " + currentEpoch + "th iteration : " + currentViolations);
				ResultOutput.writeTextFile(violatedFile, currentViolations + "");
				totalViolations += currentViolations;
				ResultOutput.writeTextFile(logFile, "total violation : " + totalViolations + " until " + currentEpoch + "th iteration");
				if (classifierLearningModel.startsWith("Structured")) {
					averageWeight = DoubleOperation.divide(totalWeight, totalViolations);
				} else {
					averageWeight = DoubleOperation.divide(totalWeight, currentEpoch);
				}
				
				ResultOutput.writeTextFile(logFile, "average weight vector : " + DoubleOperation.printArray(averageWeight));
				ResultOutput.writeTextFile(logFile, "\n");
				leanredWeights.add(averageWeight);
			}
		}
	}
	
	/**
	 * learn the weight based on file
	 * 
	 * @param currentEpoch
	 * @param path
	 * @return
	 */
	private int trainingByFile(int currentEpoch, String path) {
		int currentViolations = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String currentLine;
			while((currentLine = br.readLine()) != null) {
				String[] segments = currentLine.split("-");
				String goodStateFeatures = segments[0];
				String badStateFeatures = segments[1];
				double[] goodNumericFeatures = DoubleOperation.transformString(goodStateFeatures, ",");
				double[] badNumericFeatures = DoubleOperation.transformString(badStateFeatures, ",");
				double goodActionScore = DoubleOperation.time(weight, goodNumericFeatures);
				double badActionScore = DoubleOperation.time(weight, badNumericFeatures);
				if (goodActionScore <= badActionScore) {
					double[] difference = DoubleOperation.minus(goodNumericFeatures, badNumericFeatures);
					double[] weightDifference = DoubleOperation.time(difference, learningRate);
					weight = DoubleOperation.add(weight, weightDifference);
					totalWeight = DoubleOperation.add(totalWeight, weight);
				}
			}
			br.close();
		} catch (Exception e) {
			
		}
		return currentViolations;
	}
	
	@Override
	protected void performExperiment() {
		// set parameters
		int epoch = Integer.parseInt(property.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP));
		String searchMethod = property.getProperty(EecbConstants.SEARCH_PROP);
		boolean trainTesting = Boolean.parseBoolean(property.getProperty(EecbConstants.TRAINING_VALIDATION_PROP));
		
		if (onlyTesting) {
			//TODO
			String weightPath = property.getProperty(EecbConstants.TESTING_WEIGHTPATH_PROP);
			List<double[]> weights = readWeights(weightPath);
			double[] stoppingRates = DoubleOperation.createDescendingArray(1.0, 3.0, 10);
			
			for (int i = 0; i < epoch; i++) {
				double[] learnedWeight = weights.get(i);
				for (double stoppingrate : stoppingRates) {
					ResultOutput.writeTextFile(logFile, "Now stoppingrate : " + stoppingrate);
					stoppingRate = stoppingrate;
					// test on training set
					if (trainTesting) {
						postProcess = trainPostProcess;
						doTesting(i, learnedWeight, searchMethod, "trainTesting", trainingTopics);
					}
					
					// test on testing set
					postProcess = testPostProcess;
					doTesting(i, learnedWeight, searchMethod, "testing", testingTopics);
				}
				
			}
		} else if (outputFeature) {
			// just with one training epoch
			int numberOfFeatures = features.length;
			weight = new double[numberOfFeatures];
			totalWeight = new double[numberOfFeatures];
			totalViolations = 0;
			int currentEpoch = 1;
			String classifierLearningModel = property.getProperty(EecbConstants.CLASSIFIER_PROP);   // classification model
			boolean experimentWeight = Boolean.parseBoolean(property.getProperty(EecbConstants.WEIGHT_PROP));    // true: average weight, false: latest weight
			if (experimentWeight) {
				ResultOutput.writeTextFile(logFile, "the learned weight used for testing is average weight");
			} else {
				ResultOutput.writeTextFile(logFile, "the learned weight used for testing is the latest weight");
			}
			double[] learningRates = DoubleOperation.createDescendingArray(1.0, 0.0, epoch);
			learningRate = learningRates[0];
			int currentViolations = trainingBySearch(currentEpoch, searchMethod);
			// print weight information
			ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
			ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
			ResultOutput.writeTextFile(logFile, "the number of violated constraints for the " + currentEpoch + "th iteration : " + currentViolations);
			ResultOutput.writeTextFile(violatedFile, currentViolations + "");
			totalViolations += currentViolations;
			ResultOutput.writeTextFile(logFile, "total violation : " + totalViolations + " until " + currentEpoch + "th iteration");
			
			/* pairwise: average weight is over the total number of violated constraints, while for 
			 * pairwise & singleton : average weight is over the number of iterations
			 * */
			double[] averageWeight;
			if (classifierLearningModel.startsWith("Structured")) {
				averageWeight = DoubleOperation.divide(totalWeight, totalViolations);
			} else {
				averageWeight = DoubleOperation.divide(totalWeight, currentEpoch);
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
			
			learnedWeights.add(learnedWeight);
			
			if (currentEpoch % mInterval == 0) {
				
				postProcess = trainPostProcess;
				
				// do tuning parameter, focus on stopping rate
				if (tuningParameter) {
					stoppingRate = tuneParameter(currentEpoch, learnedWeight);
					ResultOutput.writeTextFile(logFile, "After tuning (1.0 - 3.0), the stopping rate is :" + stoppingRate + " for the " + currentEpoch + " iteration");
				}
				
				// validation part
				if (trainTesting) {
					doTesting(currentEpoch, learnedWeight, searchMethod, "trainTesting", trainingTopics);
				}
				
				postProcess = testPostProcess;
				doTesting(currentEpoch, learnedWeight, searchMethod, "testing", testingTopics);
			}
			
			// learn the weight from the file
			for (int i = 1; i < epoch; i++) {
				currentEpoch = i + 1;
				currentViolations = trainingByFile(currentEpoch, featurePath);
				
				ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
				ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
				ResultOutput.writeTextFile(logFile, "the number of violated constraints for the " + currentEpoch + "th iteration : " + currentViolations);
				ResultOutput.writeTextFile(violatedFile, currentViolations + "");
				totalViolations += currentViolations;
				ResultOutput.writeTextFile(logFile, "total violation : " + totalViolations + " until " + currentEpoch + "th iteration");
				
				/* pairwise: average weight is over the total number of violated constraints, while for 
				 * pairwise & singleton : average weight is over the number of iterations
				 * */
				if (classifierLearningModel.startsWith("Structured")) {
					averageWeight = DoubleOperation.divide(totalWeight, totalViolations);
				} else {
					averageWeight = DoubleOperation.divide(totalWeight, currentEpoch);
				}
				
				ResultOutput.writeTextFile(logFile, "average weight vector : " + DoubleOperation.printArray(averageWeight));
				ResultOutput.writeTextFile(logFile, "\n");
				
				// average weight or current weight
				if (experimentWeight) {
					learnedWeight = averageWeight;
				} else {
					learnedWeight = weight;
				}
				learnedWeights.add(learnedWeight);
				if (currentEpoch % mInterval == 0) {
					
					postProcess = trainPostProcess;
					
					// do tuning parameter, focus on stopping rate
					if (tuningParameter) {
						stoppingRate = tuneParameter(currentEpoch, learnedWeight);
						ResultOutput.writeTextFile(logFile, "After tuning (1.0 - 3.0), the stopping rate is :" + stoppingRate + " for the " + currentEpoch + " iteration");
					}
					
					// validation part
					if (trainTesting) {
						doTesting(currentEpoch, learnedWeight, searchMethod, "trainTesting", trainingTopics);
					}
					
					postProcess = testPostProcess;
					doTesting(currentEpoch, learnedWeight, searchMethod, "testing", testingTopics);
				}
			}
			
		} else {
			// with training
			int numberOfFeatures = features.length;
			weight = new double[numberOfFeatures];
			totalWeight = new double[numberOfFeatures];
			totalViolations = 0;
			String classifierLearningModel = property.getProperty(EecbConstants.CLASSIFIER_PROP);   // classification model
			boolean experimentWeight = Boolean.parseBoolean(property.getProperty(EecbConstants.WEIGHT_PROP));    // true: average weight, false: latest weight
			if (experimentWeight) {
				ResultOutput.writeTextFile(logFile, "the learned weight used for testing is average weight");
			} else {
				ResultOutput.writeTextFile(logFile, "the learned weight used for testing is the latest weight");
			}
			double[] learningRates = DoubleOperation.createDescendingArray(1.0, 0.0, epoch);
			
			/* each epoch */
			for (int i = 0; i < epoch; i++) {
				int currentEpoch = i + 1;
				ResultOutput.writeTextFile(logFile, "The " + currentEpoch + "th iteration training....");
				learningRate = learningRates[i];
				
				// training part
				int currentViolations = trainingBySearch(currentEpoch, searchMethod);
				// print weight information
				ResultOutput.writeTextFile(logFile, "weight vector : " + DoubleOperation.printArray(weight));
				ResultOutput.writeTextFile(logFile, "total weight vector : " + DoubleOperation.printArray(totalWeight));
				ResultOutput.writeTextFile(logFile, "the number of violated constraints for the " + currentEpoch + "th iteration : " + currentViolations);
				ResultOutput.writeTextFile(violatedFile, currentViolations + "");
				totalViolations += currentViolations;
				ResultOutput.writeTextFile(logFile, "total violation : " + totalViolations + " until " + currentEpoch + "th iteration");
				
				/* pairwise: average weight is over the total number of violated constraints, while for 
				 * pairwise & singleton : average weight is over the number of iterations
				 * */
				double[] averageWeight;
				if (classifierLearningModel.startsWith("Structured")) {
					averageWeight = DoubleOperation.divide(totalWeight, totalViolations);
				} else {
					averageWeight = DoubleOperation.divide(totalWeight, currentEpoch);
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
				
				// print test result every five epochs
				if (currentEpoch % mInterval == 0) {
				
					postProcess = trainPostProcess;
					
					// do tuning parameter, focus on stopping rate
					if (tuningParameter) {
						stoppingRate = tuneParameter(currentEpoch, learnedWeight);
						ResultOutput.writeTextFile(logFile, "After tuning (1.0 - 3.0), the stopping rate is :" + stoppingRate + " for the " + currentEpoch + " iteration");
					}
					
					// validation part
					if (trainTesting) {
						doTesting(currentEpoch, learnedWeight, searchMethod, "trainTesting", trainingTopics);
					}
					
					postProcess = testPostProcess;
					doTesting(currentEpoch, learnedWeight, searchMethod, "testing", testingTopics);
				}
			}
		}

		// delete serialized objects and mention result
		ResultOutput.deleteResult(serializedOutput);
		if (trainGoldOnly || testGoldOnly) {
			ResultOutput.deleteResult(mentionRepositoryPath);
		}

		ResultOutput.printTime();
	}
	
	
	
	/* tune parameter based on the development topics */
	private double tuneParameter(int currentEpoch, double[] learnedWeight) {
		double rate = 0.0;
		//TODO
		Development development = new Development(developmentTopics, currentEpoch, learnedWeight, 2.0, 3.0, 5);
		rate = development.tuning();
		return rate;
	}
	
	/* do testing */
	private void doTesting(int currentEpoch, double[] learnedWeight, String searchMethod, String phase, String[] topics) {

		String finalResultPath = "";
		if (phase.equals("testing")) {
			finalResultPath = testingPath;
		} else {
			finalResultPath = trainingPath;
		}
		
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + phase + "-" + currentEpoch;
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + phase + "-" + currentEpoch;
		String lossPredictedCorefCluster = conllResultPath + "/losspredictedCorefCluster-" + phase + "-" + currentEpoch;
		
		PrintWriter writerPredicted = null;
		PrintWriter writerGold = null;
		PrintWriter lossWriterPredicted = null;
		try {
			writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
			writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
			lossWriterPredicted = new PrintWriter(new FileOutputStream(lossPredictedCorefCluster));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		for (int j = 0; j < topics.length; j++) {
			ResultOutput.writeTextFile(logFile, "\n");
			String topic = topics[j];
			ResultOutput.writeTextFile(logFile, phase + " : Starting to do testing on " + topic);
			Document document = ResultOutput.deserialize(topic, serializedOutput, false);

			// before search parameters
			ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail before search");
			ResultOutput.printParameters(document, topic, logFile);

			// configure dynamic file and folder path
			currentExperimentFolder = experimentResultFolder + "/" + topic;
			Command.createDirectory(currentExperimentFolder);
			mscorePath = currentExperimentFolder + "/" + phase + "-iteration" + (currentEpoch + 1) + "-" + topic;

			// use search to update weight
			ISearch mSearchMethod = EecbConstructor.createSearchMethod(searchMethod);
			mSearchMethod.setWeight(learnedWeight);
			mSearchMethod.setDocument(document);
			mSearchMethod.testingSearch();
			Document afterDocument = mSearchMethod.getDocument();
			ResultOutput.printDocumentScoreInformation(afterDocument, "document after search: ", logFile, logger);
			
			DocumentAlignment.updateOrderedPredictedMentions(afterDocument);
			ResultOutput.printDocumentScoreInformation(afterDocument, "document after search before pronoun : ", logFile, logger);
			ResultOutput.writeTextFile(logFile, "topic " + topic + "'s detail after search");
			ResultOutput.printParameters(afterDocument, topic, logFile);
			
			// do pronoun sieve on the document
			try {
				DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
				CorefSystem cs = new CorefSystem();
				cs.corefSystem.coreference(afterDocument, pronounSieve);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			ResultOutput.printDocumentScoreInformation(afterDocument, "document after search after pronoun : ", logFile, logger);
			
			if (postProcess) {
		    	SieveCoreferenceSystem.postProcessing(afterDocument);
		    }

		    SieveCoreferenceSystem.printConllOutput(afterDocument, writerPredicted, false, postProcess);
		    SieveCoreferenceSystem.printConllOutput(afterDocument, writerGold, true);
		    
		    //
		    // for bestLossState case
		    //
		    Document documentState = ResultOutput.deserialize(topic, serializedOutput, false);
		    State<CorefCluster> copyBestLossState = mSearchMethod.getBestLostState();
		    List<Integer> removeSets = new ArrayList<Integer>();
		    for (Integer key : copyBestLossState.getState().keySet()) {
		    	CorefCluster cluster = copyBestLossState.getState().get(key);
		    	if (cluster.corefMentions.size() > 1) {
		    		for (Mention mention : cluster.corefMentions) {
		    			int mentionID = mention.mentionID;
		    			if (copyBestLossState.getState().get(mentionID) != null && copyBestLossState.getState().get(mentionID).corefMentions.size() == 1) {
		    				removeSets.add(mentionID);
		    			}
		    		}
		    	}
		    }
		    for (Integer key : removeSets) {
		    	copyBestLossState.remove(key);
		    }
		    
		    documentState.corefClusters = copyBestLossState.getState();
		    DocumentAlignment.updateOrderedPredictedMentions(documentState);
		    ResultOutput.printParameters(documentState, topic, logFile);
		    ResultOutput.printDocumentScoreInformation(documentState, "best loss state : ", logFile, logger);

			if (postProcess) {
		    	SieveCoreferenceSystem.postProcessing(documentState);
		    }

		    SieveCoreferenceSystem.printConllOutput(documentState, lossWriterPredicted, false, postProcess);
		}
		
		printFinalCoNLLScoreResult(goldCorefCluster, predictedCorefCluster, lossPredictedCorefCluster, phase, finalResultPath, currentEpoch);
		
		writerPredicted.close();
		writerGold.close();
		lossWriterPredicted.close();
	}
	
	/**
	 * print the final CoNLL score result
	 * 
	 * @param goldCorefCluster
	 * @param predictedCorefCluster
	 * @param lossPredictedCorefCluster
	 * @param phase
	 * @param finalResultPath
	 * @param currentEpoch
	 */
	private void printFinalCoNLLScoreResult(String goldCorefCluster, String predictedCorefCluster, String lossPredictedCorefCluster, 
			String phase, String finalResultPath, int currentEpoch) {
		CoNLLScorerHelper conllScorerHelper = new CoNLLScorerHelper(currentEpoch, logFile);
		conllScorerHelper.printFinalCoNLLScore(goldCorefCluster, predictedCorefCluster, phase);
		double predictedCoNLL = conllScorerHelper.getFinalCoNllF1Result();
		double predictedScore = conllScorerHelper.getLossScoreF1Result();
		conllScorerHelper.printFinalCoNLLScore(goldCorefCluster, lossPredictedCorefCluster, "loss" + phase);
		double lossCoNLL = conllScorerHelper.getFinalCoNllF1Result();
		double lossScore = conllScorerHelper.getLossScoreF1Result();
		ResultOutput.writeTextFile(finalResultPath, currentEpoch + "-" + stoppingRate + "," + predictedCoNLL + "," + lossCoNLL + "," + predictedScore + ","  + lossScore);
	}
	
	/**
	 * Read the weights
	 * 
	 * @param path
	 * @return
	 */
	private List<double[]> readWeights(String path) {
		List<double[]> weights = new ArrayList<double[]>();

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String currentLine;
			while((currentLine = br.readLine()) != null) {
				String[] segments = currentLine.split(",");
				double[] weight = new double[segments.length];
				for (int i = 0; i < segments.length; i++) {
					weight[i] = Double.parseDouble(segments[i]);
				}
				weights.add(weight);
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return weights;
	}
	
	/**
	 * Need the following properties:
	 *  -props 'Location of coref.properties'
	 * @throws Exception
	 * 
	 */
	public static void main(String[] args) {
		//TODO
		boolean debug = true;
		String configurationPath = "";
		if (debug) {
			configurationPath = "/scratch/JavaFile/stanford-corenlp-2012-05-22/src/edu/oregonstate/experimentconfigs/pairwise-flat/" +
								"debug-flat-oregonstate-pairwise-none-pairwise.properties";
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