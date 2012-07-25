package edu.oregonstate.domains.eecb.reader;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * TO represent the entity and event object in the class
 */
public class EecbTopic implements Serializable {

	private static final long serialVersionUID = -5240977918505714064L;
	/** clusters for corefernece mentions */
	public Map<Integer, CorefCluster> corefClusters;
	public Map<Integer, CorefCluster> goldCorefClusters;

	public EecbTopic() {
		corefClusters = new HashMap<Intege, CorefCluster>();
		goldCorefClusters = new HashMap<Integer, CorefCluster>(); 
	}

}
