package edu.oregonstate.domains.eecb.reader;

import edu.stanford.nlp.ie.machinereading.domains.ace.reader.MatchException;
import edu.stanford.nlp.trees.Span;
import java.util.Vector;

/**
 * The textual form of the mention occured in the document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbCharSeq {

	/** The exact text matched by this sequence */
	private String mText;
	/** in order to bookkeep the the start and end of the tokens */
	private Span mTokenOffset;
	
	/**
	 * The reason for this is that we extract the annotation according to character index of the tokens
	 */
	private Span mByteOffset;
	
	public EecbCharSeq(String text, int start, int end) {
		mText = text;
		mByteOffset = new Span(start, end);
		mTokenOffset = null;
	}
	
	public int getByteStart() {
	    return mByteOffset.start();
	}

	public int getByteEnd() {
	    return mByteOffset.end();
	}

	public Span getByteOffset() {
	    return mByteOffset;
	}
	
	public String getText() {
		return mText;
	}
	
	public Span getTokenOffset() {
		return mTokenOffset;
	}
	
	public int getTokenStart() {
	    if (mTokenOffset == null)
	      return -1;
	    return mTokenOffset.start();
	}

	public int getTokenEnd() {
	    if (mTokenOffset == null)
	      return -1;
	    return mTokenOffset.end();
	}
	
	/**
	 * Matches this char seq against the full token stream As a result of this
	 * method mTokenOffset is initialized
	 */
	public void match(Vector<EecbToken> tokens) throws MatchException {
		int start = -1;
	    int end = -1;
	    
	    for (int i = 0; i < tokens.size(); i++) {
	    	if (tokens.get(i).getByteOffset().start() == mByteOffset.start()) {
	            start = i;
	        } else if (mByteOffset.start() > tokens.get(i).getByteOffset().start()
	                && mByteOffset.start() < tokens.get(i).getByteOffset().end()) {
	            start = i;
	        }
	    	
	    	if (tokens.get(i).getByteOffset().end() == mByteOffset.end() + 1) {
	            end = i;
	            break;
	        } else if (mByteOffset.end() >= tokens.get(i).getByteOffset().start()
	                && mByteOffset.end() < tokens.get(i).getByteOffset().end() - 1) {
	            end = i;
	            break;
	        }
	    }
	    
	    if (start >= 0 && end >= 0) {
	        mTokenOffset = new Span(start, end);
	        // mPhrase = makePhrase(tokens, mTokenOffset);
	    } else {
	        throw new MatchException("Match failed!");
	    }
	}
	
	/**
	 * for debug convience 
	 * 
	 * @param label
	 * @param offset
	 * @return
	 */
	public String toXml(String label, int offset) {
	    StringBuffer buffer = new StringBuffer();
	    EecbElement.appendOffset(buffer, offset);
	    buffer.append("<" + label + ">\n");
	    EecbElement.appendOffset(buffer, offset + 2);
	    buffer.append("<charseq START=\"" + mTokenOffset.start() + "\" END=\"" + mTokenOffset.end() + " CHARSTART=\"" + mByteOffset.start() + "\" CHAREND=\"" + 
	    		mByteOffset.end() + "\">" + mText + "</charseq>");
	    buffer.append("\n");
	    EecbElement.appendOffset(buffer, offset);
	    buffer.append("</" + label + ">");
	    return buffer.toString();
	}
	
	@Override
	public String toString() {
		return "EecbCharSeq [mText = " + mText + ", mByteOffset=" + mByteOffset +", mTokenOffset=" + mTokenOffset + "]";
	}

}
