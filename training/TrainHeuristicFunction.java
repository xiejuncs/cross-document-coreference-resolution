package edu.oregonstate.training;


import java.util.Calendar;

import Jama.Matrix;
import edu.oregonstate.CDCR;
import edu.oregonstate.CorefSystem;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.BeamSearch;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;


/**
 * train heuristic function for beam search
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainHeuristicFunction {
	
	private int mExpasion;
	// topic in the corpus
	private String[] mTopic;
	private Matrix mInitialModel;
	private int mBeamWidth;
	private static String scoreOutputPath;

	public TrainHeuristicFunction(int expansion, String[] topic, int beamWidth) {
		mExpasion = expansion;
		mTopic = topic;
		mBeamWidth = beamWidth;
		Train.currentOutputFileName = "TrainHeuristicFunction";
	}
	
	// train the model
	// at first, collect the data first, and then use Perceptron to train the model
	public Matrix train() {
		Matrix model = mInitialModel;
		for (String topic : mTopic) {
			ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic" + topic + "................");
			try {
				//for (ScoreType type : ScoreType.values()) {
					ScoreType type = ScoreType.Pairwise;
					CorefSystem cs = new CorefSystem();
					Document document = cs.getDocument(topic);
					ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + " : " + document.allGoldMentions.size());
					ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + " : " + document.goldCorefClusters.size());
					ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + ResultOutput.printCluster(document.goldCorefClusters));
					cs.corefSystem.coref(document);
					ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Clusters After Stanford's System Preprocess of " + topic + " : " + document.corefClusters.size());
					ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + ResultOutput.printCluster(document.corefClusters));
					ResultOutput.writeTextFile(CDCR.outputFileName, "Stanford System merges " + (document.allGoldMentions.size() - document.corefClusters.size()) + " mentions");
					System.out.println(type.toString());
					ResultOutput.writeTextFile(CDCR.outputFileName, type.toString());
					String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
					String prefix = "CROSS-" + mExpasion + "-" + mBeamWidth + "-" + type.toString() + "-" + topic + "-";
					prefix = prefix.concat(timeStamp);
					scoreOutputPath = edu.oregonstate.util.EecbConstants.TEMPORY_RESULT_PATH + prefix;
					ResultOutput.deleteResult(edu.oregonstate.util.EecbConstants.ADJACENT_INTERMEDIATE_RESULT_PATH);
					BeamSearch beamSearch = new BeamSearch(mBeamWidth, scoreOutputPath, type, document, mExpasion);
					beamSearch.bestFirstBeamSearch();
				//}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			ResultOutput.writeTextFile(CDCR.outputFileName, "end to process topic" + topic + "................");
		}

		return model;
	}
	
}
