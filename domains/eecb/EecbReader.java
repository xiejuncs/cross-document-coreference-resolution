package edu.oregonstate.domains.eecb;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.oregonstate.EgenericDataSetReader;
import edu.oregonstate.domains.eecb.reader.EecbCharSeq;
import edu.oregonstate.domains.eecb.reader.EecbDocument;
import edu.oregonstate.domains.eecb.reader.EecbEntity;
import edu.oregonstate.domains.eecb.reader.EecbEntityMention;
import edu.oregonstate.domains.eecb.reader.EecbEventMention;
import edu.oregonstate.domains.eecb.reader.EecbToken;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceCharSeq;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntityMention;
import edu.stanford.nlp.ie.machinereading.structure.AnnotationUtils;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.ExtractionObject;
import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.CoreMap;

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
	 * Read in the detail file and mentions.txt in order to count how many entity and event mention exist
	 * in one single document
	 * 
	 */
	public Annotation read(String path) throws Exception {
		List<CoreMap> allSentences = new ArrayList<CoreMap>();
		File basePath = new File(path);	
		assert basePath.exists();  // Assert whether the file name exists 
		Annotation corpus = new Annotation("");
		assert basePath.isFile(); // Each file name is not a directory
		allSentences.addAll(readDocument(basePath, corpus));
		AnnotationUtils.addSentences(corpus, allSentences);
		return corpus;
	}
	
	/**
	 * Reads in .eecb file
	 * 
	 * @param file
	 * 			a file object of EECB file
	 * @param corpus
	 * @return
	 * @throws IOException
	 */
	private List<CoreMap> readDocument(File file, Annotation corpus) throws Exception {
		//String prefix = file.getName().replaceAll(".eecb", "");
		String prefix = file.getAbsolutePath().replaceAll(".eecb", "");
		// preifx /scratch/JavaFile/stanford-corenlp-2012-05-22/corpus/EECB2.0/data/1/1
		List<CoreMap> sentencesFromFile = readDocument(prefix, corpus);
		return sentencesFromFile;
	}
	
	/**
	 * deal with .eecb file 
	 * @param prefix
	 * @param corpus
	 * @return
	 * @throws Exception
	 */
	private List<CoreMap> readDocument(String prefix, Annotation corpus) throws Exception {
		logger.info("Reading Document: " + prefix);
		List<CoreMap> results = new ArrayList<CoreMap>();
		// NOTE: this method remains to be finish
		EecbDocument eecbDocument = EecbDocument.parseDocument(prefix, GlobalConstantVariables.MENTION_ANNOTATION_PATH);
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
		        l.set(CharacterOffsetBeginAnnotation.class, tokens.get(i).getTokenStart());
		        l.set(CharacterOffsetEndAnnotation.class, tokens.get(i).getTokenEnd());
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

		    	// 
		    	EntityMention convertedMention = new EntityMention(eecbEntityMention.getId(), sentence, eecbEntityMention.getExtent().getTokenOffset(), 
		    			eecbEntityMention.getExtent().getTokenOffset(), "", "", "");
		    	entityCounts.incrementCount(convertedMention.getType());
		    	logger.info("CONVERTED MENTION HEAD SPAN: " + convertedMention.getHead());
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
		    	

		    	EventMention convertedMention = new EventMention(eecbEventMention.getId(), sentence, eecbEventMention.getExtent().getTokenOffset(), "", "", anchorObject, convertedArgs, null); // 
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
