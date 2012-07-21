package edu.oregonstate.domains.eecb.reader;

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
	private Map<String, EecbEventMentionArgument> mRolesToArguments;
	
	/** the parent event */
	private EecbEvent mParent;
	
	/** anchor text for this event, just the phrase annotated by the mentions.txt */
	private EecbCharSeq mAnchor;

	public EecbEventMention(String id, EecbCharSeq extent, EecbCharSeq anchor) {
		super(id, extent);
		this.mAnchor = anchor;
		mRolesToArguments = new HashMap<String, EecbEventMentionArgument>();
	}
	
	@Override
	public String toString() {
		return "EecbEventMention [mAnchor = " + mAnchor + ", mParent=" + mParent + 
		", mRolesToArguments = " + mRolesToArguments + ", mExtent = " + mExtent +
		", mId = " + mID + "]";
	}
	
	public Collection<EecbEventMentionArgument> getArgs() {
		return mRolesToArguments.values();
	}
	
	public Set<String> getRoles() {
		return mRolesToArguments.keySet();
	}
	
	public EecbEntityMention getArg(String role) {
		return mRolesToArguments.get(role).getContent();
	}
	
	public void addArg(EecbEntityMention em, String role){
		mRolesToArguments.put(role, new EecbEventMentionArgument(role, em));
	}
	
	public void setAnchor(EecbCharSeq anchor) {
		mAnchor = anchor;
	}
	
	public EecbCharSeq getAnchor() {
		return mAnchor;
	}

}
