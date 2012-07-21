package edu.oregonstate.domains.eecb.reader;

import java.util.ArrayList;
import java.util.List;

/**
 * Store only Eecb Event
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEvent extends EecbElement {
	
	/** The list of mentions for this event */
	private List<EecbEventMention> mMentions;
	
	public EecbEvent(String id) {
		super(id);
		mMentions = new ArrayList<EecbEventMention>();
	}
	
	public void addMention(EecbEventMention m) {
		mMentions.add(m);
	}
	
	public EecbEventMention getMention(int index) {
		return mMentions.get(index);
	}
	
	/** Get the size of Event Mentions */
	public int getMentionCount() {
		return mMentions.size();
	}
}
