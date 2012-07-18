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
	
	/** Offset in the document stream */
	private Span mByteOffset;
	
	/** Span of tokens that match this char sequence */
	private Span mTokenOffset;
	
	public EecbCharSeq(String text, int start, int end) {
		mText = text;
		mByteOffset = new Span(start, end);
		mTokenOffset = null;
	}
	
	public String getText() {
		return mText;
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
	
	public Span getTokenOffset() {
		return mTokenOffset;
	}
	
	@Override
	public String toString() {
		return "EecbCharSeq [mByteOffset = " + mByteOffset + ", mText = " + mText + ", mTokenOffset = " + mTokenOffset + "]";
	}
}
