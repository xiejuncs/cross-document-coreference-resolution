package edu.oregonstate.domains.eecb.reader;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import javax.management.RuntimeErrorException;

import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.util.IntTuple;

/**
 * TO represent the entity and event object in the class
 */
public class EecbTopic implements Serializable {

	private static final long serialVersionUID = -5240977918505714064L;
	
	/**The list of gold mentions*/
	public List<List<List<Mention>>> goldOrderedMentionsBySentence;
	
	/** The list of predicted mentions */
	public List<List<List<Mention>>> predictedOrderedMentionsBySentence;
	
	/** Clusters for coreferent mentions */
	public Map<Integer, CorefCluster> corefClusters;

	/** Gold Clusters for coreferent mentions */
	public Map<Integer, CorefCluster> goldCorefClusters;

	/** All mentions in a document mentionID -> mention*/
	public Map<Integer, Mention> allPredictedMentions;
	public Map<Integer, Mention> allGoldMentions;
	
	/** Set of roles (in role apposition) in a topic  */
	public List<Set<Mention>> roleSet;
	
	private final HashMap<IntTuple, Mention> mentionheadPositions;
	
	/**
	 * Position of each mention in the input matrix
	 * Each mention occurrence with sentence # and position within sentence
	 * (Nth mention, not Nth token)
	 */
	public HashMap<Mention, IntTuple> positions;
	
	/** return the list of predicted mentions */
	public List<List<List<Mention>>> getOrderedMentions() {
		return predictedOrderedMentionsBySentence;
	}

	public EecbTopic() {
		corefClusters = new HashMap<Integer, CorefCluster>();
	    goldCorefClusters = null;
	    allPredictedMentions = new HashMap<Integer, Mention>();
	    allGoldMentions = new HashMap<Integer, Mention>();
	    roleSet = new ArrayList<Set<Mention>>();
	    positions = new HashMap<Mention, IntTuple>();
	    mentionheadPositions = new HashMap<IntTuple, Mention>();
	}
	
	public EecbTopic(List<List<List<Mention>>> predictedMentions, List<List<List<Mention>>> goldMentions, Dictionaries dict) {
		this();
		predictedOrderedMentionsBySentence = predictedMentions;
	    goldOrderedMentionsBySentence = goldMentions;
	    initialize();
	    printMentionDetection();
	}
	
	/** Document initialize */
	private void initialize() {
		initializeCorefCluster();
	}

	/**
	 * initialize positions and corefClusters (put each mention in each CorefCluster)
	 * build singleton Cluster for each mention
	 *  
	 */
	private void initializeCorefCluster() {
		// There are three layers for the predictedOrderedMentionsBySentence
		for (int i = 0; i < predictedOrderedMentionsBySentence.size(); i++) {
			for (int j = 0; j < predictedOrderedMentionsBySentence.get(i).size(); j++) {
				for (int k = 0; k > predictedOrderedMentionsBySentence.get(i).get(j).size(); k++) {
					Mention m = predictedOrderedMentionsBySentence.get(i).get(j).get(k);
					allPredictedMentions.put(m.mentionID, m);
					IntTuple pos = new IntTuple(3); // 0: document, 1: sentence, 2: specific location
					pos.set(0, i);
					pos.set(1, j);
					pos.set(2, k);
					positions.put(m, pos);
					m.sentNum = j;
					corefClusters.put(m.mentionID, new CorefCluster(m.mentionID, new HashSet<Mention>(Arrays.asList(m))));
					m.corefClusterID = m.mentionID;
					
					IntTuple headPosition = new IntTuple(3);
					headPosition.set(0, i);
					headPosition.set(1, j);
					headPosition.set(2, m.headIndex);
					mentionheadPositions.put(headPosition, m);
				}
			}
		}
	}
	
	
	
	/** Extract gold coref cluster information */
	public void extractGoldCorefClusters(){
		goldCorefClusters = new HashMap<Integer, CorefCluster>();
		for (List<List<Mention>> mentions : goldOrderedMentionsBySentence) {
			for (List<Mention> mention : mentions) {
				for (Mention m : mention) {
					int id = m.goldCorefClusterID;
					if (id == -1) throw new RuntimeException("No gold info");
					CorefCluster c = goldCorefClusters.get(id);
					if (c == null) goldCorefClusters.put(id, new CorefCluster());
					c = goldCorefClusters.get(id);
					c.setClusterID(id);
			        c.addCorefMention(m);
				}
			}
		}
	}	
	
	private void printMentionDetection() {
	    int foundGoldCount = 0;
	    for(Mention g : allGoldMentions.values()) {
	    	if(!g.twinless) foundGoldCount++;
	    }
	    SieveCoreferenceSystem.logger.fine("# of found gold mentions: "+foundGoldCount + " / # of gold mentions: "+allGoldMentions.size());
	    SieveCoreferenceSystem.logger.fine("gold mentions == ");
	}
}
