package edu.oregonstate.data;

/**
 * Superclass of all Eecb mentions (entities, events, etc)
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbMention extends EecbElement {

	protected EecbCharSeq mExtent;
	protected int mSentenceID;
	
	protected EecbMention(String id, EecbCharSeq mExtent, int sentenceID) {
		super(id);
		this.mExtent = mExtent;
		this.mSentenceID = sentenceID;
	}

	public EecbCharSeq getExtent() {return mExtent;}
	
	public int sentenceID() {
		return this.mSentenceID;
	}
	
	public String toXml(int offset) { return ""; }
}
