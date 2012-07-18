package edu.oregonstate.ie.dcoref;

import java.io.Serializable;

import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;

public class Emention implements CoreAnnotation<Mention>, Serializable {
	
	private static final long serialVersionUID = -8635596914056828168L;
	
	public int mentionID = -1;
	public int startIndex;
	public int endIndex;
	public SemanticGraph dependency;
	
	public Emention() {
	}
	
	public Emention(int mentionID, int startIndex, int endIndex, SemanticGraph dependency) {
		this.mentionID = mentionID;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.dependency = dependency;
	}
	
	public Class<Mention> getType() { return Mention.class; }

}
