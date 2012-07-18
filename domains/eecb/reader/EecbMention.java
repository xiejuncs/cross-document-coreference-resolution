package edu.oregonstate.domains.eecb.reader;

/**
 * Superclass of all Eecb mentions (entities, events, etc)
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbMention extends EecbElement {

	protected EecbCharSeq mExtent;
	
	protected EecbMention(String id, EecbCharSeq mExtent) {
		super(id);
		this.mExtent = mExtent;
	}
	
	public EecbCharSeq getExtent() {return mExtent;}
	
	@Override
	public String toString() {
		return "EecbMention: [ mExtent = " + mExtent + "]";
	}
}
