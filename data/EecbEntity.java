package edu.oregonstate.data;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntityMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceToken;

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
	
	public List<EecbEntityMention> getMentions() {return mMentions;}
	
	public void addMention(EecbEntityMention m) { 
	    mMentions.add(m);
	    m.setParent(this);
	}
	
	public String toXML(int offset) {
		StringBuffer buffer = new StringBuffer();
	    appendOffset(buffer, offset);
	    buffer.append("<entity ID=\"" + getId() + "\">\n");
	    for(EecbEntityMention m: mMentions){
	      buffer.append(m.toXml(offset + 2));
	      buffer.append("\n");
	    }
	    appendOffset(buffer, offset);
	    buffer.append("</entity>");
	    return buffer.toString();
	}
}
