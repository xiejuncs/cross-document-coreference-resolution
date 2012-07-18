package edu.oregonstate.domains.eecb.reader;

public class EecbMentionArgument {
	
	final protected String mRole;
	final protected EecbEntityMention mContent;
	final private String mentionType; // in our case, there is just event
	
	public EecbMentionArgument(String Role,
			EecbEntityMention Content, String Type) {
		mRole = Role;
		mContent = Content;
		mentionType = Type;
	}
	
	public EecbEntityMention getContent() {return mContent;}
	
	public String getRole() {return mRole;}
	
	

}
