package edu.oregonstate.util;

import java.io.File;
import java.util.List;

import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;

/**
 * those commands used for creating file or something else
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Command {

	// create a directory given a path string
	public static void createDirectory(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
	}
	
	/** 
	 * count the total number of mentions
	 * 
	 * @param mentionList
	 * @return
	 */
	public static int countMentions(List<List<Mention>> mentionList) {
		int totalNumber = 0;
		for (List<Mention> mentions : mentionList ) {
			totalNumber += mentions.size();
		}
		
		return totalNumber;
	}
	
	/**
	 * update the allPredictedMentions, which is used for Stanford scoring function
	 * The reason for this is that the corefClusters information has been updated. The mention id should be consistent 
	 * with the allPredictedMentions and corefClusters
	 * 
	 * @param stateDocument
	 * @param state
	 */
	public static void generateStateDocument(Document stateDocument, State<CorefCluster> state) {
		stateDocument.corefClusters = state.getState();
		
		for (Integer id : stateDocument.corefClusters.keySet()) {
			CorefCluster cluster = stateDocument.corefClusters.get(id);
			for (Mention m : cluster.corefMentions) {
				int mentionID = m.mentionID;
				Mention correspondingMention = stateDocument.allPredictedMentions.get(mentionID);
				int clusterid = id;
				correspondingMention.corefClusterID = clusterid;
			}
		}
	}
}
