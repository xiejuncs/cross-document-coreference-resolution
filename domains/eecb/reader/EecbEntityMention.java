package edu.oregonstate.domains.eecb.reader;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEventMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceRelationMention;

/**
 * Eecb Entity Mention, for example, an noun phrase
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEntityMention extends EecbMention {

	private EecbCharSeq mHead;
	/** Position of the head word of this mention */
	private int mHeadTokenPosition;
	
	private String mType;

	private String mLdctype;
	
	private List<EecbEventMention> mEventMentions;
	
	public EecbEntityMention(String id, String type, String ldctype, EecbCharSeq extent, EecbCharSeq head) {
		super(id, extent);
		mType = type;
	    mLdctype = ldctype;
	    mHead = head;
	    mExtent = extent;
	    mHeadTokenPosition = -1;
	    mEventMentions = new ArrayList<EecbEventMention>();
	}
	
	@Override
	public String toString() {
		return "EecbEntitiMention [ mHead = " + mHead + "]";
	}
	
	
	
}
