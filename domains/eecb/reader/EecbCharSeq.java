package edu.oregonstate.domains.eecb.reader;

import edu.stanford.nlp.trees.Span;


/**
 * The textural form of the mention occured in the document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbCharSeq {

	/** The exact text matched by this sequence */
	private String mText;
	
	/** 
	 * The reason for this is that we extract the annotation according to the span of tokens
	 * Span of tokens that match this char sequence 
	 */
	private Span mTokenOffset;
	
	public EecbCharSeq(String text, int start, int end) {
		mText = text;
		mTokenOffset = new Span(start, end);
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
	
	public String toXml(String label, int offset) {
	    StringBuffer buffer = new StringBuffer();
	    EecbElement.appendOffset(buffer, offset);
	    buffer.append("<" + label + ">\n");
	    EecbElement.appendOffset(buffer, offset + 2);
	    buffer.append("<charseq START=\"" + mTokenOffset.start() + "\" END=\"" + mTokenOffset.end() + "\">" + mText
	            + "</charseq>");
	    buffer.append("\n");
	    EecbElement.appendOffset(buffer, offset);
	    buffer.append("</" + label + ">");
	    return buffer.toString();
	}
	
	@Override
	public String toString() {
		return "EecbCharSeq [mText = " + mText + ", mTokenOffset=" + mTokenOffset + "]";
	}

}
