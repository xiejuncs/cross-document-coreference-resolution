package edu.oregonstate.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Store only EECB event mention
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEventMention extends EecbMention {

	/** Maps argument role to argument mentions */
	private Map<String, EecbEntityMention> mRolesToArguments;
	
	/** the parent event */
	private EecbEvent mParent;
	
	/** anchor text for this event, just the phrase annotated by the mentions.txt */
	private EecbCharSeq mAnchor;

	/** scope is the whole sentence, while the extent is the sentence segment the mention is in*/
	public EecbEventMention(String id, EecbCharSeq extent, EecbCharSeq anchor, int sentence) {
		super(id, extent, sentence);
		this.mAnchor = anchor;
		mRolesToArguments = new HashMap<String, EecbEntityMention>();
	}
	
	@Override
	public String toString() {
		return "EecbEventMention [mAnchor = " + mAnchor + ", mParent=" + mParent + 
		", mRolesToArguments = " + mRolesToArguments + ", mExtent = " + mExtent +
		", mId = " + mID + ", mSentence = " + mSentenceID + "]";
	}
	
	public Collection<EecbEntityMention> getArgs() {
		return mRolesToArguments.values();
	}
	
	public Set<String> getRoles() {
		return mRolesToArguments.keySet();
	}
	
	public EecbEntityMention getArg(String role) {
		return mRolesToArguments.get(role);
	}
	
	public void addArg(EecbEntityMention em, String role){
		mRolesToArguments.put(role, em);
	}
	
	public void setAnchor(EecbCharSeq anchor) {
		mAnchor = anchor;
	}
	
	public EecbCharSeq getAnchor() {
		return mAnchor;
	}
	
	public void setParent(EecbEvent e) {
	    mParent = e;
	}

	public EecbEvent getParent() {
	    return mParent;
	}

}
