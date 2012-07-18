package edu.oregonstate.structure;

import edu.stanford.nlp.ie.machinereading.structure.Span;

/**
 * Each entity mention is described by a type and a span of text
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EentityMention extends EextractionObject {
	private String corefID = "-1";
	
	public void setCorefID(String id) {
	    this.corefID = id;
	}
	
	/** 
	 * Offsets the head span, e.g., "George Bush" in the extent "the president George Bush"
	 * The offsets are relative to the sentence containing this mention 
	 */
	private Span headTokenSpan;
	
	public Span getHead() { return headTokenSpan; }
}
