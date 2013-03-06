package edu.oregonstate.method;

import java.util.HashMap;
import java.util.Properties;

import edu.oregonstate.dataset.CorefSystem;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.ISearch;
import edu.oregonstate.search.State;
import edu.oregonstate.util.DocumentAlignment;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;

/**
 * decoding the coreference resolution using 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CoreferenceResolutionDecoding extends Decoding {

	// topics used for decoding, such as training topics
	private final String[] decodingTopics;

	// whether do post-process on predicted mentions
	private final boolean postProcess;

	// whether output feature, which is useful for Dagger if the feature is on
	private final boolean featureOutput;

	// stop criterion for decoding
	private final double stopCriterion;
	
	// conll result path
	private final String conllResultPath;
	
	// log file
	private final String logFile;
	
	// serialized object path
	private final String serializedPath;
	
	// best state score
	private final boolean bestState;
	
	// Loss Type
	private final ScoreType lossType;
	
	// result path
	private final String resultPath;

	public CoreferenceResolutionDecoding(String phase, String[] topics, boolean outputFeature, double stoppingRate) {
		super(phase);
		decodingTopics = topics;
		featureOutput = outputFeature;
		stopCriterion = stoppingRate;		
		postProcess = ExperimentConstructor.postProcess;
		conllResultPath = ExperimentConstructor.resultPath + "/conll";
		logFile = ExperimentConstructor.logFile;
		serializedPath = ExperimentConstructor.resultPath + "/document";
		bestState = Boolean.parseBoolean(ExperimentConstructor.experimentProps.getProperty(EecbConstants.BEST_STATE_PROP, "false"));
		String lossTypeString = ExperimentConstructor.experimentProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise");
		lossType = ScoreType.valueOf(lossTypeString);
		resultPath = ExperimentConstructor.resultPath;
		
	}

	/**
	 * decoding the coreference resolution
	 * 
	 * @param weight
	 */
	public void decode(double[] weight) {
		// store the predicted mentions and gold mentions into corpus
		Document corpus = new Document();
		corpus.goldCorefClusters = new HashMap<Integer, CorefCluster>();

		// conll scoring files
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + decodingPhase;
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + decodingPhase;

		for(String topic : decodingTopics) {
			ResultOutput.writeTextFile(logFile, "\n\n(Dagger) Testing Iteration Epoch : " + decodingPhase + "; Document :" + topic + "\n\n");

			Document document = ResultOutput.deserialize(topic, serializedPath, false);
			ResultOutput.printParameters(document, topic, logFile);

			ISearch search = EecbConstructor.createSearchMethod("BeamSearch");
			State<CorefCluster> bestLossState = search.testingBySearch(document, weight, decodingPhase, featureOutput, stopCriterion);

			// if enable best score
			if (bestState) {
				document.corefClusters = bestLossState.getState();
			}
			DocumentAlignment.alignDocument(document);

			// do pronoun coreference resolution
			CorefSystem cs = new CorefSystem();
			cs.applyPronounSieve(document);

			// print the cluster result
			//ResultOutput.writeTextFile(logFile, "\ngold clusters\n");
			//ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.goldCorefClusters));
			//ResultOutput.writeTextFile(logFile, "\npredicted clusters\n");
			//ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.corefClusters));

			// whether post-process the document
			if (postProcess) {
				DocumentAlignment.postProcessDocument(document);
			}

			//print the cluster result
			//ResultOutput.writeTextFile(logFile, "\n\nafter post-process\n\n");
			//ResultOutput.writeTextFile(logFile, "\ngold clusters\n");
			//ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.goldCorefClusters));
			//ResultOutput.writeTextFile(logFile, "\npredicted clusters\n");
			//ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.corefClusters));

			// add single document to the corpus
			ResultOutput.printDocumentScore(document, lossType, logFile, "single " + decodingPhase + " document " + topic);
			ResultOutput.printParameters(document, topic, logFile);

			DocumentAlignment.mergeDocument(document, corpus);

			ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster);
		}

		// Stanford scoring
		String[] scoreInformation = ResultOutput.printDocumentScore(corpus, lossType, logFile, decodingPhase);

		// CoNLL scoring
		double[] finalScores = ResultOutput.printCorpusResult(logFile, goldCorefCluster, predictedCorefCluster, "model generation");
		ResultOutput.writeTextFile(resultPath + "/" + decodingPhase + ".csv", scoreInformation[0] + "\t" + finalScores[0] + "\t" + 
				finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);
	}

}
