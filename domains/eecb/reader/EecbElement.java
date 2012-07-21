package edu.oregonstate.domains.eecb.reader;

/**
 * Base class for all EECB annotation elements
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbElement {

	/** unique identifier for this element*/
	protected String mID;
	
	public EecbElement(String mID) {
		this.mID = mID;
	}
	
	public String getId() {return mID; }

	// indentation for debug.
	// Entity/Event without indentation
	// EntityMention/EventMention with two indentations
	public static void appendOffset(StringBuffer buffer, int offset) {
		for(int i = 0; i < offset; i ++){
			buffer.append(" ");
		}
	}
	
}
