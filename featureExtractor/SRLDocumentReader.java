package edu.oregonstate.featureExtractor;

import java.util.*;

import edu.stanford.nlp.io.IOUtils;


/**
 * read SRL result Document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SRLDocumentReader {

	/** document path */
	private final String mDocumentPath;
	
	public SRLDocumentReader(String documentPath) {
		mDocumentPath = documentPath;
	}
	
	/**
	 * read raw input and format as SRLDocument
	 * Seperating the sentences 
	 * 
	 * @return
	 */
	public SRLDocument readDocument() {
		// read srl result from the output of Semantic role labeling software
		List<String> srlResults = IOUtils.linesFromFile(mDocumentPath);
		
		// define a SRLDocument
		String[] elements = mDocumentPath.split("/");
		String topic = elements[elements.length - 1].split("\\.")[0];
		SRLDocument document = new SRLDocument(topic);
		
		// format the srl result as the SRLDocument
		List<String[]> sentence = new ArrayList<String[]>();
		for (int index = 0; index <= srlResults.size(); index++) {
			if (index == srlResults.size()) {
				document.addSentence(sentence);
				break;
			}
			
			String line = srlResults.get(index);
			if ((line.equals(""))) {
				document.addSentence(sentence);
				sentence = new ArrayList<String[]> ();
				continue;
			}
			
			String[] token = line.split("\t");
			sentence.add(token);
		}
		
		return document;
	}
	
	/**
	 * the main entry of the program 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		args = new String[]{"data/srl/16.output"};
		
		String documentPath = args[0];
		
		SRLDocumentReader reader = new SRLDocumentReader(documentPath);
		SRLDocument document = reader.readDocument();
	}
}
