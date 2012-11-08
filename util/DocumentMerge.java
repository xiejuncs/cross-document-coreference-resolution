package edu.oregonstate.util;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.util.IntPair;

/**
 * Aim to Merge one document to another document,
 * 
 * There are three types of merge: 
 *     (1) For the cross coreference resolution case, we just need to incorporate the document into a document container.
 *     For example, the field document just need to merge into the field corpus
 *     (2) For the within case, besides the pure merge, we also need to update the corefClusterID field in the corefCluster 
 *     field. Because we tend to focus on dynamic document, only change predictedMentions and corefCluster two fields
 *     (3) For the hybrid case, we also need to incorporate predicitedMnetionOrderedBySentence into the document container
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class DocumentMerge {

	/** we incorporate document into corpus */
	private Document mDocument;
	private Document mCorpus;
	
	public DocumentMerge( Document document, Document corpus) {
		mDocument = document;
		mCorpus = corpus;
	}
	
	public Document getCorpus() {
		return mCorpus;
	}
	
	/** just add the corefCluster */
	private void addCorefCluster() {
		  for(Integer id : mDocument.corefClusters.keySet()) {
			  mCorpus.addCorefCluster(id, mDocument.corefClusters.get(id));
		  }
	}
	
	/** add the gold coref cluster */
	private void addGoldCorefCluster() {
		for (Integer id : mDocument.goldCorefClusters.keySet()) {
			mCorpus.addGoldCorefCluster(id, mDocument.goldCorefClusters.get(id));
		}
	}
	
	/** add the gold coreference clustering regarding the conflcting key */
	private void addGoldCorefClusterWithDuplicateKeys() {
		for (Integer id : mDocument.goldCorefClusters.keySet()) {
			mCorpus.addGoldCorefClusterWithDuplicateKeys(id, mDocument.goldCorefClusters.get(id));
		}
	}
	
	/** add the predicted mention */
	private void addPredictedMention() {
		if (mCorpus.predictedOrderedMentionsBySentence == null) {
			mCorpus.predictedOrderedMentionsBySentence = new ArrayList<List<Mention>>();
		}
		
		for (Integer id : mDocument.allPredictedMentions.keySet()) {
			mCorpus.addPredictedMention(id, mDocument.allPredictedMentions.get(id));
		}
	}

	/** add the gold mention */
	private void addGoldMention() {
		for (Integer id : mDocument.allGoldMentions.keySet()) {
			mCorpus.addGoldMention(id, mDocument.allGoldMentions.get(id));
		}
	}
	
	/**
	 * add four fields to the corpus
	 * 
	 * It does not matter to change the fields of the original object, because we do not need to 
	 * use the original object anymore, we just need to add those fields of the original object into
	 * the new object, Then that is OK
	 * 
	 * @param document
	 */
	public void addDocument() {
		addCorefCluster();
		addGoldCorefCluster();
		addPredictedMention();
		addGoldMention();
	}
	
	/** add the predicted mentions according to the sentence */
	private void addPredictedOrderdBySentence() {
		for (int i = 0; i < mDocument.predictedOrderedMentionsBySentence.size(); i++) {
			List<Mention> mentions = mDocument.predictedOrderedMentionsBySentence.get(i);
			mCorpus.predictedOrderedMentionsBySentence.add(mentions);
		}
	}
	
	/** 
	 * merge document to another document, and pay attention to the conflicting gold cluster keys,  
	 * meanwhile, change the corefCluster id of predicted mentions
	 */
	public void mergeDocument() {
		addGoldCorefClusterWithDuplicateKeys();
		addPredictedMention();
		addGoldMention();
		addCorefCluster();
		addPredictedOrderdBySentence();
		
		for (IntPair key : mDocument.mentionSynonymInWN){
			mCorpus.mentionSynonymInWN.add(key);
		}
		
		
		
		for (Integer id : mCorpus.goldCorefClusters.keySet()) {
			CorefCluster cluster = mCorpus.goldCorefClusters.get(id);
			for (Mention m : cluster.corefMentions) {
				int mentionID = m.mentionID;
				Mention correspondingMention = mCorpus.allGoldMentions.get(mentionID);
				int clusterid = id;
				correspondingMention.corefClusterID = clusterid;
			}
		}
		
	}
	
	
}
