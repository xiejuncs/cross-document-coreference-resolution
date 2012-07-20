package edu.oregonstate.domains.eecb.reader;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntity;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEventMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceRelationMention;

/**
 * Eecb Entity Mention, for example, an noun phrase
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEntityMention extends EecbMention {
	
	private String mType;
	
	private List<EecbEventMention> mEventMentions;
	
	/** The parent entity */
	private EecbEntity mParent;
	
	public EecbEntityMention(String id, String type, EecbCharSeq extent) {
		super(id, extent);
		mType = type;
	    mExtent = extent;
	    mEventMentions = new ArrayList<EecbEventMention>();
	}
	
	public void setParent(EecbEntity e) { mParent = e; }
	
}
