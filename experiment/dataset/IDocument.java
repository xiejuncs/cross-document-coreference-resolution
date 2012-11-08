package edu.oregonstate.experiment.dataset;

import edu.stanford.nlp.dcoref.Document;

/**
 * how to get document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface IDocument {

	public Document getDocument(String path);
}
