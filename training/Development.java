package edu.oregonstate.training;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.oregonstate.dataset.CorefSystem;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.method.Dagger;
import edu.oregonstate.score.CoNLLScorerHelper;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.search.State;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
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
	
	/* experiment properties */
	private final Properties mProps;
	
	/** development topics */
	private final String[] mDevelopmentTopics;

	/** current epoch */
	private final int mCurrentEpoch;
	
	/** search method */
	private final String searchMethod;
	
	/** the weight used for validation */
	private final double[] mLearnedWeight;
	
	/** starting digit */
	private final double mStartNumber;
	
	/** ending digit */
	private final double mEndNumber;
	
	/** iterations */
	private final int mIterations;
	
	/* log file */
	private final String logFile;
	
	/* model index */
	private final int mModelIndex;
	
	/* conll result folder */
	private final String conllResultPath;
	
	/* serialized output */
	private final String serializeOutput;
	
	/* experiment result folder */
	private final String experimentResultFolder;
	
	/* whether post-process the development set */
	private final boolean postProcess;
	
	/**
	 * 
	 * @param developmentTopics 
	 * @param currentEpoch
	 * @param learnedWeight
	 * @param startNumber : the number used for indicating the beginning number for validation
	 * @param endNumber : the number used for indicating the end number for validation
	 */
	public Development(int modelIndex, int currentEpoch, double[] learnedWeight, double startNumber, double endNumber, int iterations) {
		mProps = ExperimentConstructor.experimentProps;
		mDevelopmentTopics = ExperimentConstructor.developmentTopics;
		experimentResultFolder = ExperimentConstructor.experimentResultFolder;
		conllResultPath = experimentResultFolder + "/conllResult";
		serializeOutput = experimentResultFolder + "/documentobject";
		
		mCurrentEpoch = currentEpoch;
		searchMethod = mProps.getProperty(EecbConstants.SEARCH_PROP);
		mLearnedWeight = learnedWeight;
		mStartNumber = startNumber;
		mEndNumber = endNumber;
		mIterations = iterations;
		logFile = ExperimentConstructor.logFile;
		mModelIndex = modelIndex;
		
		postProcess = Boolean.parseBoolean(mProps.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
	}
	
	/**
	 * tuning the parameter
	 */
	public double tuning() {
		double[] stoppingRates = DoubleOperation.createDescendingArray(mStartNumber, mEndNumber, mIterations);
		boolean bestStateScore = Boolean.parseBoolean(mProps.getProperty(EecbConstants.ENABLE_BEST_SEARCH_SCORE, "false"));

		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "Begin Tuning parameter for the " + mModelIndex + "'s model in the " + mCurrentEpoch + "th iteration");

		double maximumScore = 0.0;
		double optimizedStoppingRate = 0.0;
		double score = 0.0;
		// do tuning
		for (double stoppingRate : stoppingRates) {
			Document corpus = new Document();
			corpus.goldCorefClusters = new HashMap<Integer, CorefCluster>();
			
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "stopping rate number : " + stoppingRate + " for the "  + mCurrentEpoch + "th iteration");
			
			String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-tuning-" + mModelIndex + "-" + mCurrentEpoch + "-" + stoppingRate;
			String goldCorefCluster = conllResultPath + "/goldCorefCluster-tuning-" + mModelIndex + "-" + mCurrentEpoch + "-" + stoppingRate;

			PrintWriter writerPredicted = null;
			PrintWriter writerGold = null;
			try {
				writerPredicted = new PrintWriter(new FileOutputStream(predictedCorefCluster));
				writerGold = new PrintWriter(new FileOutputStream(goldCorefCluster));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			String phaseID = mModelIndex + "-" + mCurrentEpoch + "-" + stoppingRate;

			for (String topic : mDevelopmentTopics) {
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "Starting to tuning on " + topic + " with stpping rate " + stoppingRate + " for the " + mCurrentEpoch + "th iteration");
				Document document = ResultOutput.deserialize(topic, serializeOutput, false);

				// before search : document parameters
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, "topic " + topic + "'s detail before search during tuning-" + mModelIndex + "-" + mCurrentEpoch + "-" + stoppingRate);
				ResultOutput.printParameters(document, topic, logFile);

				// configure dynamic file and folder path
				String currentExperimentFolder = experimentResultFolder + "/" + topic;
				Command.createDirectory(currentExperimentFolder);
				
				ISearch search = EecbConstructor.createSearchMethod(searchMethod);
				State<CorefCluster> bestLossState = search.testingBySearch(document, mLearnedWeight, phaseID, false, stoppingRate);
				
				if (bestStateScore) {
					document.corefClusters = bestLossState.getState();
				}
				
				DocumentAlignment.alignDocument(document);

				ResultOutput.printDocumentScore(document, ScoreType.Pairwise, logFile, "single " + phaseID + " document " + topic);
				ResultOutput.printParameters(document, topic, logFile);

				// do pronoun coreference resolution
				CorefSystem cs = new CorefSystem();
				cs.applyPronounSieve(document);

//				ResultOutput.writeTextFile(logFile, "gold clusters : " + ResultOutput.printCluster(document.goldCorefClusters));
//				ResultOutput.writeTextFile(logFile, "predicted clusters : " + ResultOutput.printCluster(document.corefClusters));
				// whether post-process the document
				if (postProcess) {
					DocumentAlignment.postProcessDocument(document);
				}

				// add single document to the corpus
				DocumentAlignment.mergeDocument(document, corpus);
				
				ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster, postProcess);
			}

			writerPredicted.close();
			writerGold.close();
			
			// do scoring on this iteration
			try {
				// Stanford scoring
				String[] scoreInformation = ResultOutput.printDocumentScore(corpus, ScoreType.Pairwise, logFile, phaseID);

				// CoNLL scoring
				double[] finalScores = ResultOutput.printCorpusResult(mCurrentEpoch, logFile, goldCorefCluster, predictedCorefCluster, "model generation");
				ResultOutput.writeTextFile(experimentResultFolder + "/" + phaseID + ".csv", scoreInformation[0] + "\t" + finalScores[0] + "\t" + 
												finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);
				
				if (score > maximumScore) {
					optimizedStoppingRate = stoppingRate;
					maximumScore = score;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the stopping rate : " + optimizedStoppingRate);
		return optimizedStoppingRate;
	}
	
}