package edu.oregonstate.domains.eecb.reader;

public class EecbEventMentionArgument extends EecbMentionArgument {
	public EecbEventMentionArgument(String Role,
			EecbEntityMention content) {
		super(Role, content, "event");
	}
}
