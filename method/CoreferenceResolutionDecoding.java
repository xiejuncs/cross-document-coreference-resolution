package edu.oregonstate.method;

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

	// topic used for decoding, such as one training topic
	private final String topic;

	// whether do post-process on predicted mentions
	private final boolean postProcess;

	// whether output feature, which is useful for Dagger if the feature is on
	private final boolean featureOutput;

	// stop criterion for decoding
	private final double mStoppingRate;

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
	private final String experimentFolder;

	public CoreferenceResolutionDecoding(String phase, String topic, boolean outputFeature, double stoppingRate, String phaseIndex) {
		super(phase);
		this.topic = topic;
		featureOutput = outputFeature;
		mStoppingRate = stoppingRate;		
		postProcess = ExperimentConstructor.postProcess;
		conllResultPath = ExperimentConstructor.experimentFolder + "/conll/" + phaseIndex;
		logFile = ExperimentConstructor.experimentFolder + "/" + topic + "/logfile";
		serializedPath = ExperimentConstructor.experimentFolder + "/document";
		bestState = Boolean.parseBoolean(ExperimentConstructor.experimentProps.getProperty(EecbConstants.SEARCH_BESTSTATE, "true"));
		String lossTypeString = ExperimentConstructor.experimentProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP, "Pairwise");
		lossType = ScoreType.valueOf(lossTypeString);
		experimentFolder = ExperimentConstructor.experimentFolder;
	}

	/**
	 * decoding the coreference resolution
	 * 
	 * @param weight
	 */
	public void decode(double[] weight) {
		// conll scoring files
		String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + decodingPhase;
		String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + decodingPhase;

		ResultOutput.writeTextFile(logFile, "\n\nTesting Iteration Epoch : " + decodingPhase + "; Document :" + topic + "\n\n");

		Document document = ResultOutput.deserialize(topic, serializedPath, false);
		ResultOutput.printParameters(document, topic, logFile);

		ISearch search = EecbConstructor.createSearchMethod("BeamSearch");
		State<CorefCluster> bestLossState = search.testingBySearch(document, weight, decodingPhase, featureOutput, mStoppingRate);

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

		ResultOutput.printDocumentScore(document, lossType, logFile, "single " + decodingPhase + " document " + topic);
		ResultOutput.printParameters(document, topic, logFile);

		ResultOutput.printDocumentResultToFile(document, goldCorefCluster, predictedCorefCluster);

		// Stanford scoring
		String[] scoreInformation = ResultOutput.printDocumentScore(document, lossType, logFile, decodingPhase);

		// CoNLL scoring
		double[] finalScores = ResultOutput.printCorpusResult(logFile, goldCorefCluster, predictedCorefCluster, decodingPhase);
		ResultOutput.writeTextFile(experimentFolder + "/" + topic + "/" + decodingPhase + ".csv", scoreInformation[0] + "\t" + finalScores[0] + "\t" + 
				finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);
	}

}
