package edu.oregonstate.domains.eecb.reader;


public class EecbMentionArgument {
	
	final protected String mRole;
	final protected EecbEntityMention mContent;
	
	public EecbMentionArgument(String Role,
			EecbEntityMention Content) {
		mRole = Role;
		mContent = Content;
	}
	
	public EecbEntityMention getContent() {return mContent;}
	
	public String getRole() {return mRole;}
	
	public String toXml(int offset) {
	    StringBuffer buffer = new StringBuffer();
	    EecbElement.appendOffset(buffer, offset);
	    buffer.append("<event_mention_argument REFID=\"" + mContent.getId() + 
	  	  "\" ROLE=\"" + mRole + "\">\n");
	    //buffer.append(getContent().toXml(offset + 2));
	    EecbCharSeq ext = getContent().getExtent();
	    buffer.append(ext.toXml("extent", offset + 2));
	    buffer.append("\n");
	    EecbElement.appendOffset(buffer, offset);
	    buffer.append("</event_mention_argument>");
	    return buffer.toString();
	}

}
