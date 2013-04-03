package edu.oregonstate.util;

import java.util.List;
import java.util.Map;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;

public class DocumentAlignment {
	
	/**
	 * update the corefcluster ID of each mention in the orderedPredictionMentions
	 * 
	 * @param document
	 */
	public static void alignDocument(Document document) {
		updateOrderedPredictedMentions(document);
		//updateOrderedGoldMentions(document);
	}
	
	private static void updateOrderedPredictedMentions(Document document) {
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
						mention.mentionID = m.mentionID;
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
	private static void updateOrderedGoldMentions(Document document) {
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
						mention.mentionID = m.mentionID;
						break;
					}
				}
				
				int mentionID = m.mentionID;
                Mention correspondingMention = document.allGoldMentions.get(mentionID);
                correspondingMention.goldCorefClusterID = clusterID;
			}
		}
	}
	
	/**
	 * whether post-process the document
	 * 
	 * @param document
	 */
	public static void postProcessDocument(Document document) {
		boolean postProcessGold = Boolean.parseBoolean(ExperimentConstructor.experimentProps.getProperty(EecbConstants.DATAGENERATION_POSTPROCESS_GOLD_PROP, "true"));
		SieveCoreferenceSystem.postProcessing(document);
		if (postProcessGold) {
			SieveCoreferenceSystem.postProcessingGoldClusters(document);
		}
	}
	
	/**
	 * merge from document to to document: four fields, which is just used for scoring in the system, not output for CoNLL scoring
	 * 
	 * @param from
	 * @param to
	 */
	public static void mergeDocument(Document from, Document to) {
		// add allGoldMentions
		for (Integer key : from.allGoldMentions.keySet()) {
			to.allGoldMentions.put(key, from.allGoldMentions.get(key));
		}
		
		// add goldCorefClusters
		for (Integer key : from.goldCorefClusters.keySet()) {
			to.goldCorefClusters.put(key, from.goldCorefClusters.get(key));
		}
		
		// add allPredictedMentions
		for (Integer key : from.allPredictedMentions.keySet()) {
			to.allPredictedMentions.put(key, from.allPredictedMentions.get(key));
		}
		
		// add corefClusters
		for (Integer key : from.corefClusters.keySet()) {
			to.corefClusters.put(key, from.corefClusters.get(key));
		}
	}
	
}
