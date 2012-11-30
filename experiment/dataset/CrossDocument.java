package edu.oregonstate.experiment.dataset;

import edu.stanford.nlp.dcoref.Document;

/**
 * First, put the documents of the specific topic together without doing coreference resolution on single document,
 * after that, doing coreference resolution on each topic (represented as a Document Object). During coreference 
 * resolution phase, creates training examples from the clusters available after every merge operation.
 * <p>
 * 
 * After each merge, there are two things needed to do:
 * 1. update the ID of the related predicate and nominals, for example, if I merge VM1 and VM2, then the arguments of VM2
 * 's ID should to modified to the ID of VM1. In this part, just need to 
 * 2. regenerate the cluster features
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */

public class CrossDocument implements IDocument {
	
	/** treat the topic as a whole */
	public Document getDocument(String path) {
		CorefSystem cs = new CorefSystem();
		Document document = new Document();
		try {
			document = cs.getDocument(path);
			cs.corefSystem.coref(document);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return document;
	}
	
}