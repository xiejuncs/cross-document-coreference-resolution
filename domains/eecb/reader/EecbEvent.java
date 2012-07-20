package edu.oregonstate.domains.eecb.reader;

import java.util.ArrayList;
import java.util.List;

/**
 * Store only Eecb Event
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEvent {
	private String Id;
	
	private String mType;
	
	/** The list of mentions for this event */
	private List<EecbEventMention> mMentions;
	
	public EecbEvent(String Id, String type) {
		this.Id = Id;
		this.mType = type;
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
	
	public String getId() {
		return this.Id;
	}
}
