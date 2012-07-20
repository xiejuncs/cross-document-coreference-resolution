package edu.oregonstate.domains.eecb.reader;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntityMention;

/**
 * EECB entity. In the EECB corpus, an corefid is used to represent the entity.
 * For example: <ENTITY COREFID="27">People magazine</ENTITY>
 * "People Magazine" is a entity mention. Its entity identifier is 27.
 * <p>
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbEntity extends EecbElement {	
	private List<EecbEntityMention> mMentions;
	private String mType;
	
	public EecbEntity(String id, String type) {
		super(id);
		mType = type;
		mMentions = new ArrayList<EecbEntityMention>();
	}
	
	public List<EecbEntityMention> getMentions() {return mMentions;}
	
	public void addMention(EecbEntityMention m) { 
	    mMentions.add(m);
	    m.setParent(this);
	  }
}
