package edu.oregonstate.domains.eecb;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

import edu.oregonstate.EgenericDataSetReader;
import edu.oregonstate.domains.eecb.reader.EecbCharSeq;
import edu.oregonstate.domains.eecb.reader.EecbDocument;
import edu.oregonstate.domains.eecb.reader.EecbEntity;
import edu.oregonstate.domains.eecb.reader.EecbEntityMention;
import edu.oregonstate.domains.eecb.reader.EecbEventMention;
import edu.oregonstate.domains.eecb.reader.EecbToken;
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
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.oregonstate.example.SemanticOutputInterface;

/**
 * Simple Wrapper of EECB code to strcture objects
 * 
 * @author xie
 *
 */
public class EecbReader extends EgenericDataSetReader {
	private Counter<String> entityCounts;
	private Counter<String> adjacentEntityMentions;
	private Counter<String> eventCounts;
	
	/**
	 * Make an EecbReader
	 */
	public EecbReader() {
		this(null, true);
	}
	
	public EecbReader(StanfordCoreNLP processor, boolean preprocess) {
		super(processor, preprocess, false, true);
		entityCounts = new ClassicCounter<String>();
		adjacentEntityMentions = new ClassicCounter<String>();
		eventCounts = new ClassicCounter<String>();
		logger = Logger.getLogger(EecbReader.class.getName());
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
	public String getExtentString(CoreMap sentence, Span extentTokenSpan) {
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
	 * match two sentence and its id
	 * <p>
	 * here we just use the simple match, iterate the two sentence find
	 * 
	 * @param lRawText
	 * @param sentenceidToSentence
	 * @return Map<Integer, Integer> the former is the sentence id in the raw text, and the later is the sentence id in the 
	 *								 transformed result
	 */
	private Map<Integer, Integer> matchSentence(List<String> lRawText, Map<Integer, List<String>> sentenceidToSentence) {
		// we need to make sure there exists one-one correspondence between the original text and output sentence
		assert lRawText.size() == sentenceidToSentence.size();
		Map<Integer, Integer> matchResult = new HashMap<Integer, Integer>();
		for (int i = 0; i < lRawText.size(); i++) {
			String sentence = lRawText.get(i);
			
			char[] chars = sentence.toCharArray();
			int textLength = chars.length;
			StringBuilder originalSentencewithoutSpace = new StringBuilder();
			for (int j = 0; j < textLength; j++) {
				char character = chars[j];
				String charac = Character.toString(character);
				if (!charac.equals(" "))	originalSentencewithoutSpace.append(charac);
			}
			String originalsent = originalSentencewithoutSpace.toString().trim();
			for (Integer key : sentenceidToSentence.keySet()) {
				List<String> tokens = sentenceidToSentence.get(key);
				// concatenate the tokens together
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < tokens.size(); j++) {
					sb.append(tokens.get(j));
				}
				String sent = sb.toString().trim();
				if (originalsent.equals(sent)) {
					matchResult.put(i, key);
					break;
				}
			}
		}
		
		/**ensure that every sentence has its corresponding SRL annotations*/
		assert matchResult.size() == lRawText.size();
		return matchResult;
	}
	
	
	
	/**
	 * READ the document
	 * 
	 * @param files list of documents included in specific topic
	 * @param corpus the annotation got 
	 * @return
	 */
	private List<CoreMap> readDocument(List<String> files, String topic, Annotation corpus) {
		List<CoreMap> results = new ArrayList<CoreMap>();
		EecbDocument eecbDocument = EecbDocument.parseDocument(files, topic);
		
		
		
		String docID = eecbDocument.getId();
		
		Map<String, EntityMention> entityMentionMap = new HashMap<String, EntityMention>();
		int tokenOffset = 0;
		
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
		    
		    	Span extent = new Span(eecbEntityMention.getExtent().getTokenStart(), eecbEntityMention.getExtent().getTokenEnd());
		    	Span head = new Span(eecbEntityMention.getHead().getTokenStart(), eecbEntityMention.getHead().getTokenEnd());
		    	EntityMention convertedMention = new EntityMention(eecbEntityMention.getId(), sentence, extent, head, "", "", "");
		    	convertedMention.setCorefID(corefID);
		    	entityCounts.incrementCount(convertedMention.getType());
		        logger.info("CONVERTED ENTITY MENTION: " + convertedMention);
		        AnnotationUtils.addEntityMention(sentence, convertedMention);
		        entityMentionMap.put(eecbEntityMention.getId(), convertedMention);
		    }
		    
		    // convert EventMentions
		    for (EecbEventMention eecbEventMention : eventMentions) {
		    	EecbCharSeq anchor = eecbEventMention.getAnchor();
		    	ExtractionObject anchorObject = new ExtractionObject(
		    			eecbEventMention.getId() + "-anchor",
		    	        sentence,
		    	        new Span(anchor.getTokenStart() - tokenOffset, anchor.getTokenEnd() + 1 - tokenOffset),
		    	        "ANCHOR",
		    	        null);
		    	
		    	Set<String> roleSet = eecbEventMention.getRoles();
		        List<String> roles = new ArrayList<String>();
		        for(String role: roleSet) roles.add(role);
		        List<ExtractionObject> convertedArgs = new ArrayList<ExtractionObject>();

		        int left = Integer.MAX_VALUE;
		        int right = Integer.MIN_VALUE;
		        for(String role: roles){
		          EecbEntityMention arg = eecbEventMention.getArg(role);
		          ExtractionObject o = entityMentionMap.get(arg.getId());
		          if(o == null){
		            logger.severe("READER ERROR: Failed to find event argument with id " + arg.getId());
		            logger.severe("This happens because a few event mentions illegally span multiple sentences. Will ignore this mention.");
		            return null;
		          }
		          convertedArgs.add(o);
		          if(o.getExtentTokenStart() < left) left = o.getExtentTokenStart();
		          if(o.getExtentTokenEnd() > right) right = o.getExtentTokenEnd();
		        }
		    	
		        Span extent = new Span(eecbEventMention.getExtent().getTokenStart(), eecbEventMention.getExtent().getTokenEnd());
		    	EventMention convertedMention = new EventMention(eecbEventMention.getId(), sentence, extent, "", "", anchorObject, convertedArgs, null); // 
		    	if(convertedMention != null){
		            eventCounts.incrementCount(convertedMention.getType());
		            logger.info("CONVERTED EVENT MENTION: " + convertedMention);
		            AnnotationUtils.addEventMention(sentence, convertedMention);
		        }
		    }
		    
		    
		    results.add(sentence);
		    tokenOffset += tokens.size();
		}
		
		return results;
	}	
}
