package edu.oregonstate.domains.eecb.reader;

/**
 * Eecb Entity Mention, for example, an noun phrase
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEntityMention {

	private EecbCharSeq mHead;
	/** Position of the head word of this mention */
	private int mHeadTokenPosition;
	
	@Override
	public String toString() {
		return "EecbEntitiMention [ mHead = " + mHead + "]";
	}
	
	
	
}
