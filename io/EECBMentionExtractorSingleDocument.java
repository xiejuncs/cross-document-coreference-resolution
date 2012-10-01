package edu.oregonstate.io;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.oregonstate.CDCR;
import edu.oregonstate.io.EmentionExtractor.EntityComparator;
import edu.oregonstate.io.EmentionExtractor.EventComparator;
import edu.oregonstate.util.Constants;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.MentionExtractor;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.UtteranceAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * Extract single document <COREF> mentions from eecb corpus 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EECBMentionExtractorSingleDocument extends EmentionExtractor {
	
	private String documentPath;
	private String documentIdentifier;
	
	public EECBMentionExtractorSingleDocument(String singleDocument, LexicalizedParser p, Dictionaries dict, Properties props, Semantics semantics) throws Exception {
		super(dict, semantics);
		stanfordProcessor = loadStanfordProcessor(props);
		String[] paras = singleDocument.split("/");
		int length = paras.length;
		String topic = paras[length - 2];
		String documentID = paras[length - 1];
		documentID = documentID.substring(0, documentID.length() - 5);
		documentIdentifier = topic + "-" + documentID;
		documentPath = singleDocument;
		baseID = 1000000 * Integer.parseInt(topic) + 100000 * Integer.parseInt(documentID);
		eecbReader = new EecbReader(stanfordProcessor, false);
		eecbReader.setLoggerLevel(Level.INFO);
	}
	
	/**
	 * just extract the info for coref from a single document in the corpus
	 * inistantiate extract the info for coref from a single topic in the corpus.
	 * So it is different from the topic case
	 * 
	 * @param singleDocument
	 * @return
	 * @throws Exception
	 */
	public Document inistantiateSingleDocument(String singleDocument) throws Exception {
		List<List<CoreLabel>> allWords = new ArrayList<List<CoreLabel>>();
		List<List<Mention>> allGoldMentions = new ArrayList<List<Mention>>();
		List<List<Mention>> allPredictedMentions = null;
		List<Tree> allTrees = new ArrayList<Tree>();
		Annotation anno = new Annotation("");
		
		try {
			// call the eecbReader
			anno = eecbReader.read(documentIdentifier);
			stanfordProcessor.annotate(anno);
			 
		    List<CoreMap> sentences = anno.get(SentencesAnnotation.class);
		    for (CoreMap sentence : sentences) {
		    	int i = 1;
		    	for (CoreLabel w : sentence.get(TokensAnnotation.class)) {
		    		w.set(IndexAnnotation.class, i++);
		    		if(!w.containsKey(UtteranceAnnotation.class)) {
		    	        w.set(UtteranceAnnotation.class, 0);
		    	    }
		    	}
		   
		    	allTrees.add(sentence.get(TreeAnnotation.class));
		    	allWords.add(sentence.get(TokensAnnotation.class));
		    	EntityComparator comparator = new EntityComparator();
		    	EventComparator eventComparator = new EventComparator();
		    	extractGoldMentions(sentence, allGoldMentions, comparator, eventComparator);
		    }
		    
		    if (CDCR.goldOnly) {
		    	// just use the gold mentions
		        allPredictedMentions = new ArrayList<List<Mention>>();
		        for (int i = 0; i < allGoldMentions.size(); i++) {
		        	List<Mention> sentence = new ArrayList<Mention>();
		        	for (int j = 0; j < allGoldMentions.get(i).size(); j++) {
		        		Mention mention = allGoldMentions.get(i).get(j);
		        		ResultOutput.serialize(mention, mention.mentionID, Constants.RESULT_PATH);
		        		Mention copyMention = ResultOutput.deserialize(Integer.toString(mention.mentionID) + ".ser", Constants.RESULT_PATH, true);
		        		copyMention.goldCorefClusterID = -1;
		        		sentence.add(copyMention);
		        	}
		        	allPredictedMentions.add(sentence);
		        }
		        
		    } else {
		    	// set the mention id here
		    	allPredictedMentions = mentionFinder.extractPredictedMentions(anno, baseID, dictionaries);
		    }
		    
		    /** according to the extraction result, print the original document with different annotation */
		    printRawDoc(sentences, allPredictedMentions, false);
		    printRawDoc(sentences, allGoldMentions, true);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		MentionExtractor dcorfMentionExtractor = new MentionExtractor(dictionaries, semantics);
		dcorfMentionExtractor.setCurrentDocumentID(documentIdentifier);
		Document document = dcorfMentionExtractor.arrange(anno, allWords, allTrees, allPredictedMentions, allGoldMentions, true);
		document.extractGoldCorefClusters();
		return document;
	}
	
	@Override
	public String toString() {
		return "EECBMentionExtractorSingleDocument: [ documentPath : " + documentPath; 
	}
	
}
