package edu.oregonstate.domains.eecb.reader;

import java.util.ArrayList;
import java.util.List;

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
	
	public EecbEntity(String id) {
		super(id);
		mMentions = new ArrayList<EecbEntityMention>();
	}
	
	public void addMention(EecbEntityMention m) {
		mMentions.add(m);
	}
	
	public List<EecbEntityMention> getMentions() {return mMentions;}
	
}
