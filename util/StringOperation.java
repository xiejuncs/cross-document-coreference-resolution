package edu.oregonstate.util;

import java.util.List;

import edu.stanford.nlp.ling.CoreLabel;

public class StringOperation {

	/** a string for CoreLabel */
	public static String sentenceToString(List<CoreLabel> tokens) {
		StringBuilder os = new StringBuilder();
		
		if (tokens != null) {
			boolean first = true;
			for (CoreLabel token : tokens) {
				if (! first) os.append(" ");
				os.append(token.word());
				first = false;
			}
		}
		
		return os.toString();
	}
}
