package edu.oregonstate.io;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import edu.oregonstate.CRC_MAIN;
import edu.oregonstate.EgenericDataSetReader;
import edu.oregonstate.data.EecbCharSeq;
import edu.oregonstate.data.EecbDocument;
import edu.oregonstate.data.EecbEntity;
import edu.oregonstate.data.EecbEntityMention;
import edu.oregonstate.data.EecbEventMention;
import edu.oregonstate.data.EecbToken;
import edu.stanford.nlp.ie.machinereading.structure.AnnotationUtils;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.ExtractionObject;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ie.machinereading.structure.Span;

/**
 * Simple Wrapper of EECB code to strcture objects
 * 
 * @author xie
 *
 */
public class EecbReader extends EgenericDataSetReader {
	
	/**
	 * Make an EecbReader
	 */
	public EecbReader() {
		this(null, true);
	}
	
	public EecbReader(StanfordCoreNLP processor, boolean preprocess) {
		super(processor, preprocess, false, true);
		logger = Logger.getLogger(CRC_MAIN.class.getName());
	    // run quietly by default
	    logger.setLevel(Level.SEVERE);
	}
	
	/**
	 * The important class for Stanford system is Document. The corefernce function 
	 * defined in the SivveCoreferenceSystem also uses Document to conduct the resolution.
	 * However, in EECB corpus, each topic directory contains several documents. Hence, we 
	 * concatenate them together. This is the first step that we need to do.
	 * 
	 * @param files list of documents included in specific topic
	 * @return
	 */
	public Annotation read(List<String> files, String topic) {
		if (files == null) {
			new RuntimeException("There are no files contained in this topic");		
		}
		List<CoreMap> allSentences = new ArrayList<CoreMap>();
		Annotation corpus = new Annotation("");
		allSentences.addAll(readDocument(files, topic, corpus));
		AnnotationUtils.addSentences(corpus, allSentences);
		return corpus;
	}
	
	/**
	 * Get the extent of the mention according to the extentTokenSpan
	 * 
	 * @param sentence
	 * @param extentTokenSpan
	 * @return
	 */
	public static String getExtentString(CoreMap sentence, Span extentTokenSpan) {
	    List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
	    StringBuilder sb = new StringBuilder();
	    for (int i = extentTokenSpan.start(); i < extentTokenSpan.end(); i ++){
	      CoreLabel token = tokens.get(i);
	      if(i > extentTokenSpan.start()) sb.append(" ");
	      sb.append(token.word());
	    }
	    return sb.toString();
	}	
	
	/**
	 * READ the document and transform to the CoreLabel
	 * 
	 * @param files list of documents included in specific topic
	 * @param corpus the annotation got 
	 * @return
	 */
	private List<CoreMap> readDocument(List<String> files, String topic, Annotation corpus) {
		List<CoreMap> results = new ArrayList<CoreMap>();
		EecbDocument eecbDocument = new EecbDocument(topic, files);
		eecbDocument.parse();
		
		String docID = eecbDocument.getId();
		
		// because tokenset is different from the the acutal tokenize 
		int offset = 0;
		for (int sentenceIndex = 0; sentenceIndex < eecbDocument.getSentenceCount(); sentenceIndex++ ) {
			List<EecbToken> tokens = eecbDocument.getSentence(sentenceIndex);
			List<CoreLabel> words = new ArrayList<CoreLabel>();
			StringBuffer textContent = new StringBuffer();
			for (int i = 0; i < tokens.size(); i++) {
				CoreLabel l = new CoreLabel();
				l.setWord(tokens.get(i).getLiteral());
		        l.set(CoreAnnotations.TextAnnotation.class, l.word());
		        l.set(CharacterOffsetBeginAnnotation.class, tokens.get(i).getByteStart());
		        l.set(CharacterOffsetEndAnnotation.class, tokens.get(i).getByteEnd());
		        words.add(l);
		        if(i > 0) textContent.append(" ");
		        textContent.append(tokens.get(i).getLiteral());
			}
			
			CoreMap sentence = new Annotation(textContent.toString());
		    sentence.set(CoreAnnotations.DocIDAnnotation.class, docID);
		    sentence.set(CoreAnnotations.TokensAnnotation.class, words);
		    logger.info("Reading sentence: \"" + textContent + "\"");
		    
		    List<EecbEntityMention> entityMentions = eecbDocument.getEntityMentions(sentenceIndex);
		    List<EecbEventMention> eventMentions = eecbDocument.getEventMentions(sentenceIndex);
		    
		    // convert entity mentions
		    for (EecbEntityMention eecbEntityMention : entityMentions) {
		    	String corefID = "";
		    	for (String entityID : eecbDocument.getKeySetEntities()) {
		    		EecbEntity e = eecbDocument.getEntity(entityID);
		    		if (e.getMentions().contains(eecbEntityMention)) {
		    			corefID = entityID;
		    			break;
		    		}
		    	}
		    
		    	int extEnd = eecbEntityMention.getExtent().getTokenEnd() - offset + 1;
		    	int extStart = eecbEntityMention.getExtent().getTokenStart() - offset;
		    	
		    	Span extent = new Span(extStart, extEnd);
		    	EntityMention convertedMention = new EntityMention(eecbEntityMention.getId(), sentence, extent, null, "", "", "");
		    	convertedMention.setCorefID(corefID);
		        logger.info("CONVERTED ENTITY MENTION: " + convertedMention);
		        AnnotationUtils.addEntityMention(sentence, convertedMention);
		    }
		    
		    // convert EventMentions
		    for (EecbEventMention eecbEventMention : eventMentions) {
		    	EecbCharSeq anchor = eecbEventMention.getAnchor();
		    	ExtractionObject anchorObject = new ExtractionObject(
		    			eecbEventMention.getId() + "-anchor",
		    	        sentence,
		    	        new Span(anchor.getTokenStart(), anchor.getTokenEnd()),
		    	        "ANCHOR",
		    	        null);
		    	
		    	Set<String> roleSet = eecbEventMention.getRoles();
		        List<String> roles = new ArrayList<String>();
		        for(String role: roleSet) roles.add(role);
		        List<ExtractionObject> convertedArgs = new ArrayList<ExtractionObject>();

		        for(String role: roles){
		          EecbEntityMention arg = eecbEventMention.getArg(role);
		          int extEnd = arg.getExtent().getTokenEnd();
			    	if (extEnd > sentence.size()) {
			    		extEnd = sentence.size();
			    	}
			    	
			    	Span extent = new Span(arg.getExtent().getTokenStart(), extEnd);
			    	EntityMention convertedMention = new EntityMention(arg.getId(), sentence, extent, null, "", "", "");
			        logger.info("CONVERTED ENTITY MENTION: " + convertedMention);
		          ExtractionObject o = convertedMention;
		          
		          convertedArgs.add(o);
		        }
		    	
		        int extEnd = eecbEventMention.getExtent().getTokenEnd() - offset + 1;
		    	int extStart = eecbEventMention.getExtent().getTokenStart() - offset;
		        
		        Span extent = new Span(extStart, extEnd);
		    	EventMention convertedMention = new EventMention(eecbEventMention.getId(), sentence, extent, "", "", anchorObject, convertedArgs, null); // 
		    	if(convertedMention != null){
		            logger.info("CONVERTED EVENT MENTION: " + convertedMention);
		            AnnotationUtils.addEventMention(sentence, convertedMention);
		        }
		    }
		    
		    results.add(sentence);
		    offset += tokens.size();
		}
		return results;
	}
	
}
