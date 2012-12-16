package edu.oregonstate.training;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.dataset.CorefSystem;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;
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
	
	/** the final stopping rate used for testing */
	private double optimizedStoppingRate;
	
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
	
	/**
	 * 
	 * @param developmentTopics 
	 * @param currentEpoch
	 * @param learnedWeight
	 * @param startNumber : the number used for indicating the beginning number for validation
	 * @param endNumber : the number used for indicating the end number for validation
	 */
	public Development(String[] developmentTopics, int currentEpoch, double[] learnedWeight, double startNumber, double endNumber) {
		mDevelopmentTopics = developmentTopics;
		optimizedStoppingRate = 0.0;
		mCurrentEpoch = currentEpoch + 1;
		searchMethod = ExperimentConstructor.property.getProperty(EecbConstants.SEARCH_PROP);
		mLearnedWeight = learnedWeight;
		mStartNumber = startNumber;
		mEndNumber = endNumber;
	}
	
	public double getStoppingRate() {
		return optimizedStoppingRate;
	}
	
	/**
	 * tuning the parameter
	 */
	public void tuning() {
		double[] stoppingRates = DoubleOperation.createDescendingArray(mStartNumber, mEndNumber, 10);

		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "Begin Tuning parameter for the " + mCurrentEpoch + "th iteration");

		double maximumScore = 0.0;
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
				ExperimentConstructor.printParameters(document, topic);

				// configure dynamic file and folder path
				String currentExperimentFolder = ExperimentConstructor.experimentResultFolder + "/" + topic;
				Command.createDirectory(currentExperimentFolder);

				// use the learned weight to do testing
				ISearch mSearchMethod = ExperimentConstructor.createSearchMethod(searchMethod);
				mSearchMethod.setWeight(mLearnedWeight);
				mSearchMethod.setDocument(document);
				mSearchMethod.testingSearch();
				
				ExperimentConstructor.updateOrderedPredictedMentions(document);
				
				// after search parameters
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "topic " + topic + "'s detail after search");
				ExperimentConstructor.printParameters(document, topic);
				
				// do pronoun sieve on the document
				try {
					DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
					CorefSystem cs = new CorefSystem();
					cs.corefSystem.coreference(document, pronounSieve);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				if (ExperimentConstructor.postProcess) {
					SieveCoreferenceSystem.postProcessing(document);
				}
				
				SieveCoreferenceSystem.printConllOutput(document, writerPredicted, false, ExperimentConstructor.postProcess);
				SieveCoreferenceSystem.printConllOutput(document, writerGold, true);
			}

			// do scoring on this iteration
			try {
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "\n\n");
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the score summary of tuning for the " + mCurrentEpoch + "th iteration with stopping rate " + stoppingRate);
				String summary = SieveCoreferenceSystem.getConllEvalSummary(ExperimentConstructor.conllScorerPath, goldCorefCluster, predictedCorefCluster);
				ExperimentConstructor.printScoreSummary(summary, true);
				double score = printFinalScore(summary);
				
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

	}
	
	/** return the final score */
	protected double printFinalScore(String summary) {
		Pattern f1 = Pattern.compile("Coreference:.*F1: (.*)%");
		Matcher f1Matcher = f1.matcher(summary);
		double[] F1s = new double[5];
		int i = 0;
		while (f1Matcher.find()) {
			F1s[i++] = Double.parseDouble(f1Matcher.group(1));
		}
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "Final score ((muc+bcub+ceafe)/3) = "+(F1s[0]+F1s[1]+F1s[3])/3);
		
		return ((F1s[0]+F1s[1]+F1s[3])/3);
	}
	
	
}
