package edu.oregonstate.util;

import java.util.List;
import java.util.Map;

import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;

public class DocumentAlignment {
	
	/**
	 * update the corefcluster ID of each mention in the orderedPredictionMentions
	 * 
	 * @param document
	 */
	public static void updateOrderedPredictedMentions(Document document) {
		List<List<Mention>> predictedOrderedMentionsBySentence = document.getOrderedMentions();
		Map<Integer, CorefCluster> corefClusters = document.corefClusters;
		for (Integer clusterID : corefClusters.keySet()) {
			CorefCluster cluster = corefClusters.get(clusterID);
			for (Mention m : cluster.getCorefMentions()) {
				int sentenceID = m.sentNum;
				List<Mention> mentions = predictedOrderedMentionsBySentence.get(sentenceID);
				int mStartIndex = m.startIndex;
				int mEndIndex = m.endIndex;
				for (Mention mention : mentions) {
					int mentionStartIndex = mention.startIndex;
					int mentionEndIndex = mention.endIndex;
					if (mentionStartIndex == mStartIndex && mentionEndIndex == mEndIndex) {
						mention.corefClusterID = m.corefClusterID;
						break;
					}
				}
				
				int mentionID = m.mentionID;
				Mention correspondingMention = document.allPredictedMentions.get(mentionID);
				correspondingMention.corefClusterID = clusterID;
			}
		}
	}
	
	/**
	 * update the corefcluster ID of each mention in the goldOrderedMentionsBySentence
	 * 
	 * @param document
	 */
	public static void updateOrderedGoldMentions(Document document) {
		List<List<Mention>> goldOrderedMentionsBySentence = document.goldOrderedMentionsBySentence;
		Map<Integer, CorefCluster> goldClusters = document.goldCorefClusters;
		for (Integer clusterID : goldClusters.keySet()) {
			CorefCluster cluster = goldClusters.get(clusterID);
			for (Mention m : cluster.getCorefMentions()) {
				int sentenceID = m.sentNum;
				List<Mention> mentions = goldOrderedMentionsBySentence.get(sentenceID);
				int mStartIndex = m.startIndex;
				int mEndIndex = m.endIndex;
				for (Mention mention : mentions) {
					int mentionStartIndex = mention.startIndex;
					int mentionEndIndex = mention.endIndex;
					if (mentionStartIndex == mStartIndex && mentionEndIndex == mEndIndex) {
						mention.corefClusterID = m.corefClusterID;
						break;
					}
				}
				
				int mentionID = m.mentionID;
				Mention correspondingMention = document.allGoldMentions.get(mentionID);
				correspondingMention.corefClusterID = clusterID;
			}
		}
	}
}
