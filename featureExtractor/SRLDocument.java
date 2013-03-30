package edu.oregonstate.featureExtractor;

import java.util.*;

/**
 * SRL document, need to specify the document ID
 * A collection of annotated tokens
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SRLDocument {

	/** document ID */
	private final String mDocumentID;
	
	/** sentences */
	private List<List<String[]>> sentences;
	
	public SRLDocument(String documentID) {
		mDocumentID = documentID;
		sentences = new ArrayList<List<String[]>>();
	}
	
	public String getDocumentID() {
		return mDocumentID;
	}
	
	public void addSentence(List<String[]> sentence) {
		sentences.add(sentence);
	}
	
	public List<List<String[]>> getSentences() {
		return sentences;
	}
	
}
