package edu.oregonstate.training;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.crosscoreferenceresolution.CrossCoreferenceResolution;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.CoNLLScorerHelper;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;

/**
 * Development Set used to tune the hyper-parameters. In our experiment, we need 
 * to tune the stopping rate for the testing phase.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Development {
	
	/** development topics */
	private String[] mDevelopmentTopics;

	/** current epoch */
	private int mCurrentEpoch;
	
	/** search method */
	private String searchMethod;
	
	/** the weight used for validation */
	private double[] mLearnedWeight;
	
	/** starting digit */
	private double mStartNumber;
	
	/** ending digit */
	private double mEndNumber;
	
	/** iterations */
	private int mIterations;
	
	/* log file */
	private String logFile;
	
	/**
	 * 
	 * @param developmentTopics 
	 * @param currentEpoch
	 * @param learnedWeight
	 * @param startNumber : the number used for indicating the beginning number for validation
	 * @param endNumber : the number used for indicating the end number for validation
	 */
	public Development(String[] developmentTopics, int currentEpoch, double[] learnedWeight, double startNumber, double endNumber, int iterations) {
		mDevelopmentTopics = developmentTopics;
		mCurrentEpoch = currentEpoch;
		searchMethod = ExperimentConstructor.property.getProperty(EecbConstants.SEARCH_PROP);
		mLearnedWeight = learnedWeight;
		mStartNumber = startNumber;
		mEndNumber = endNumber;
		mIterations = iterations;
		logFile = ExperimentConstructor.logFile;
	}
	
	/**
	 * tuning the parameter
	 */
	public double tuning() {
		double[] stoppingRates = DoubleOperation.createDescendingArray(mStartNumber, mEndNumber, mIterations);

		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "Begin Tuning parameter for the " + mCurrentEpoch + "th iteration");

		double maximumScore = 0.0;
		double optimizedStoppingRate = 0.0;
		// do tuning
		for (double stoppingRate : stoppingRates) {
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "stopping rate number : " + stoppingRate + " for the "  + mCurrentEpoch + "th iteration");
			ExperimentConstructor.stoppingRate = stoppingRate;
			
			String predictedCorefCluster = ExperimentConstructor.conllResultPath + "/predictedCorefCluster-tuning-" + mCurrentEpoch + "-" + stoppingRate;
			String goldCorefCluster = ExperimentConstructor.conllResultPath + "/goldCorefCluster-tuning-" + mCurrentEpoch + "-" + stoppingRate;

			PrintWriter writerPredicted = null;
			PrintWriter writerGold = null;
			try {
				writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
				writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			for (String topic : mDevelopmentTopics) {
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "Starting to tuning on " + topic + " with stpping rate " + stoppingRate + " for the " + mCurrentEpoch + "th iteration");
				Document document = ResultOutput.deserialize(topic, ExperimentConstructor.serializedOutput, false);

				// before search : document parameters
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "topic " + topic + "'s detail before search");
				ResultOutput.printParameters(document, topic, logFile);

				// configure dynamic file and folder path
				String currentExperimentFolder = ExperimentConstructor.experimentResultFolder + "/" + topic;
				Command.createDirectory(currentExperimentFolder);

				// use the learned weight to do testing
				ISearch mSearchMethod = EecbConstructor.createSearchMethod(searchMethod);
				mSearchMethod.setWeight(mLearnedWeight);
				mSearchMethod.setDocument(document);
				mSearchMethod.testingSearch();
				Document afterDocument = mSearchMethod.getDocument();
				
				DocumentAlignment.updateOrderedPredictedMentions(afterDocument);
				ResultOutput.printDocumentScoreInformation(afterDocument, "document after search before pronoun : ", logFile, CrossCoreferenceResolution.logger);
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "topic " + topic + "'s detail after search");
				ResultOutput.printParameters(afterDocument, topic, logFile);
				
				// do pronoun sieve on the document
				try {
					DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
					CorefSystem cs = new CorefSystem();
					cs.corefSystem.coreference(afterDocument, pronounSieve);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				if (ExperimentConstructor.postProcess) {
					SieveCoreferenceSystem.postProcessing(afterDocument);
				}
				
				DocumentAlignment.updateOrderedPredictedMentions(afterDocument);
				ResultOutput.printDocumentScoreInformation(afterDocument, "document after search after pronoun : ", logFile, CrossCoreferenceResolution.logger);
				
				SieveCoreferenceSystem.printConllOutput(afterDocument, writerPredicted, false, ExperimentConstructor.postProcess);
				SieveCoreferenceSystem.printConllOutput(afterDocument, writerGold, true);
			}

			// do scoring on this iteration
			try {
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "\n\n");
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the score summary of tuning for the " + mCurrentEpoch + "th iteration with stopping rate " + stoppingRate);
				
				CoNLLScorerHelper conllScorerHelper = new CoNLLScorerHelper(mCurrentEpoch, logFile);
				conllScorerHelper.printFinalCoNLLScore(goldCorefCluster, predictedCorefCluster, "development tuning");
				double score = conllScorerHelper.getFinalCoNllF1Result();
				
				if (score > maximumScore) {
					optimizedStoppingRate = stoppingRate;
					maximumScore = score;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
			writerPredicted.close();
			writerGold.close();
		}

		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the stopping rate : " + optimizedStoppingRate);
		return optimizedStoppingRate;
	}
	
}
