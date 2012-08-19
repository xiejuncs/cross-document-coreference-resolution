package edu.oregonstate.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Eecb Entity Mention, for example, an noun phrase
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEntityMention extends EecbMention {
	
	@Override
	public String toString() {
		return "EecbEntityMention [mHead=" + mHead + ", mExtent" + this.mExtent +", mSentence = " + sentenceID() +"]";
	  }
	
	/** The set of event mentions that contain this entity mention */
	private List<EecbEventMention> mEventMentions;
	
	/** The parent entity */
	private EecbEntity mParent;
	
	private EecbCharSeq mHead;

	/** Position of the head word of this mention */
	private int mHeadTokenPosition;
	
	public EecbEntityMention(String id, EecbCharSeq extent, EecbCharSeq head, int sentence) {
		super(id, extent, sentence);
	    mExtent = extent;
	    mHead = head;
	    mParent = null;
	    mHeadTokenPosition = -1;
	    mEventMentions = new ArrayList<EecbEventMention>();
	}
	
	public void setParent(EecbEntity e) { mParent = e; }
	public EecbEntity getParent() { return mParent; }
	public EecbCharSeq getHead() { return mHead; }
	public EecbCharSeq getExtent() { return mExtent; }
	public int getHeadTokenPosition() { return mHeadTokenPosition; }
	
	public void addEventMention(EecbEventMention rm) {
	    mEventMentions.add(rm);
	}
	public List<EecbEventMention> getEventMentions() {
	    return mEventMentions;
	}
	
	public String toXml(int offset) {
	    StringBuffer buffer = new StringBuffer();
	    appendOffset(buffer, offset);
	    buffer.append("<entity_mention ID=\"" + getId() + "\">\n");
	    buffer.append(mExtent.toXml("extent", offset + 2));
	    buffer.append("\n");
	    buffer.append(mHead.toXml("head", offset + 2));
	    buffer.append("\n");
	    appendOffset(buffer, offset);
	    buffer.append("</entity_mention>");
	    return buffer.toString();
	}
	
}
