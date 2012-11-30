package edu.oregonstate.experiment.dataset;

import edu.stanford.nlp.dcoref.Document;

public class SingleDocument implements IDocument {

	/**
	 * path: the path file
	 */
	public Document getDocument(String path) {
		CorefSystemSingleDocument cs = new CorefSystemSingleDocument();
		Document document = new Document();
		try {
			document = cs.getDocument(path);
			cs.corefSystem.coref(document);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return document;
	}
	
}
