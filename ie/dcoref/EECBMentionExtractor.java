package edu.oregonstate.ie.dcoref;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import edu.oregonstate.CRC_MAIN;
import edu.oregonstate.domains.eecb.EecbReader;
import edu.oregonstate.util.EECB_Constants;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.BeginIndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EntityTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.UtteranceAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.ValueAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.TokenizerAnnotator;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.dcoref.MentionExtractor;

/**
 * Extract <COREF> mentions from eecb corpus  
 */
public class EECBMentionExtractor extends EmentionExtractor {
	private EecbReader eecbReader;
	private String topicPath;
	protected int fileIndex = 0;
	protected ArrayList<String> files;
	
	private static Logger logger = CRC_MAIN.logger;
	
	private static class EntityComparator implements Comparator<EntityMention> {
	    public int compare(EntityMention m1, EntityMention m2){
	      if(m1.getExtentTokenStart() > m2.getExtentTokenStart()) return 1;
	      else if(m1.getExtentTokenStart() < m2.getExtentTokenStart()) return -1;
	      else if(m1.getExtentTokenEnd() > m2.getExtentTokenEnd()) return -1;
	      else if(m1.getExtentTokenEnd() < m2.getExtentTokenEnd()) return 1;
	      else return 0;
	    }
	}
	
	private static class EventComparator implements Comparator<EventMention> {
		public int compare(EventMention m1, EventMention m2){
		    if(m1.getExtentTokenStart() > m2.getExtentTokenStart()) return 1;
		    else if(m1.getExtentTokenStart() < m2.getExtentTokenStart()) return -1;
		    else if(m1.getExtentTokenEnd() > m2.getExtentTokenEnd()) return -1;
		    else if(m1.getExtentTokenEnd() < m2.getExtentTokenEnd()) return 1;
		    else return 0;
		}
	}
	
	/**
	 * Constructor of EECBMentionExtractor
	 * 
	 * @param p
	 * @param dict
	 * @param props
	 * @param semantics
	 * @throws Exception
	 */
	public EECBMentionExtractor(String topic, LexicalizedParser p, Dictionaries dict, Properties props, Semantics semantics) throws Exception {
		super(dict, semantics);
		stanfordProcessor = loadStanfordProcessor(props);
		topicPath = props.getProperty(EECB_Constants.EECB_PROP, GlobalConstantVariables.CORPUS_PATH) + "/" + topic + "/";
		eecbReader = new EecbReader(stanfordProcessor, false);
		eecbReader.setLoggerLevel(Level.INFO);
		files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
		sort(files);   // Output [1.eecb, 2.eecb, 3.eecb, 4.eecb, 5.eecb]
	}
	
	/** sort the files name according to the sequence*/
	public void sort(ArrayList<String> files) {
		Integer[] numbers = new Integer[files.size()];
		for (int i = 0; i < files.size(); i++) {
			numbers[i] = Integer.parseInt(files.get(i).substring(0, files.get(i).length() - 5));
		}
		Arrays.sort(numbers);
		for (int i = 0; i < numbers.length; i++) {
			files.set(i, Integer.toString(numbers[i]) + ".eecb");
		}
	}
	
	/**
	 * We represent topic as Document class. For example, if there are two documents included 
	 * in this class. Then we need to concatenate them together. As a whole, we do coreference 
	 * resolution. In this way, there are less works involved in this situation.
	 *  
	 *  @param mentionExtractor extractor for each document
	 *  @return topic represented by each topic
	 * extract all mentions in one doc cluster, represented by Mention 
	 */
	public Document inistantiate(String topic) throws Exception {
		List<List<CoreLabel>> allWords = new ArrayList<List<CoreLabel>>();
		List<List<Mention>> allGoldMentions = new ArrayList<List<Mention>>();
		List<List<Mention>> allPredictedMentions = null;
		List<Tree> allTrees = new ArrayList<Tree>();
		Annotation anno = new Annotation("");
		try {
			// call the eecbReader
			anno = eecbReader.parse(files, topic);
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
		    
		    allPredictedMentions = mentionFinder.extractPredictedMentions(anno, -1, dictionaries);
		    
		    /** according to the extraction result, print the original document with different annotation */
		    printRawDoc(sentences, allPredictedMentions, false);
		    printRawDoc(sentences, allGoldMentions, true);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		MentionExtractor dcorfMentionExtractor = new MentionExtractor(dictionaries, semantics);		
		return dcorfMentionExtractor.arrange(anno, allWords, allTrees, allPredictedMentions, allGoldMentions, true);
	}
	
	// Define the two variables for obtaining the list of file names
	public static int spc_count = 1;
	private static ArrayList<String> file;
	
	@Override
	public String toString() {
		return "EECBMentionExtractor: [ topicPath : " + topicPath + ", Length of file pool : " + file.size() +"]"; 
	}
	
	private void printRawDoc(List<CoreMap> sentences, List<List<Mention>> allMentions, boolean gold) throws FileNotFoundException {
	    StringBuilder doc = new StringBuilder();
	    int previousOffset = 0;
	    Counter<Integer> mentionCount = new ClassicCounter<Integer>();
	    for(List<Mention> l : allMentions) {
	      for(Mention m : l){
	        mentionCount.incrementCount(m.goldCorefClusterID);
	      }
	    }
	    
	    for(int i = 0 ; i<sentences.size(); i++) {
	      CoreMap sentence = sentences.get(i);
	      List<Mention> mentions = allMentions.get(i);
	      
	      String[] tokens = sentence.get(TextAnnotation.class).split(" ");
	      String sent = "";
	      List<CoreLabel> t = sentence.get(TokensAnnotation.class);
	      if(previousOffset+2 < t.get(0).get(CharacterOffsetBeginAnnotation.class)) sent+= "\n";
	      previousOffset = t.get(t.size()-1).get(CharacterOffsetEndAnnotation.class);
	      Counter<Integer> startCounts = new ClassicCounter<Integer>();
	      Counter<Integer> endCounts = new ClassicCounter<Integer>();
	      HashMap<Integer, Set<Integer>> endID = new HashMap<Integer, Set<Integer>>();
	      for (Mention m : mentions) {
	        startCounts.incrementCount(m.startIndex);
	        endCounts.incrementCount(m.endIndex);
	        if(!endID.containsKey(m.endIndex)) endID.put(m.endIndex, new HashSet<Integer>());
	        endID.get(m.endIndex).add(m.goldCorefClusterID);
	      }
	      for (int j = 0 ; j < tokens.length; j++){
	        if(endID.containsKey(j)) {
	          for(Integer id : endID.get(j)){
	            if(mentionCount.getCount(id)!=1 && gold) sent += "]_"+id;
	            else sent += "]";
	          }
	        }
	        for (int k = 0 ; k < startCounts.getCount(j) ; k++) {
	          if(!sent.endsWith("[")) sent += " ";
	          sent += "[";
	        }
	        sent += " ";
	        sent = sent + tokens[j];
	      }
	      for(int k = 0 ; k <endCounts.getCount(tokens.length); k++) {
	        sent += "]";
	      }
	      sent += "\n";
	      doc.append(sent);
	    }
	    if(gold) logger.fine("New DOC: (GOLD MENTIONS) ==================================================");
	    else logger.fine("New DOC: (Predicted Mentions) ==================================================");
	    logger.fine(doc.toString());
	  }
	
	/**
	 * Extract the gold mentions
	 * 
	 * @param s
	 * @param allGoldMentions
	 * @param comparator
	 */
	private void extractGoldMentions(CoreMap s, List<List<Mention>> allGoldMentions, EntityComparator comparator, EventComparator eventComparator) {
		List<Mention> goldMentions = new ArrayList<Mention>();
	    allGoldMentions.add(goldMentions);
	    List<EntityMention> goldMentionList = s.get(MachineReadingAnnotations.EntityMentionsAnnotation.class);
	    List<EventMention> goldEventMentionList = s.get(MachineReadingAnnotations.EventMentionsAnnotation.class);
	    
	    List<CoreLabel> words = s.get(TokensAnnotation.class);

	    TreeSet<EntityMention> treeForSortGoldMentions = new TreeSet<EntityMention>(comparator);
	    if(goldMentionList!=null) treeForSortGoldMentions.addAll(goldMentionList);
	    
	    TreeSet<EventMention> treeForSortEventGoldMentions = new TreeSet<EventMention>(eventComparator);
	    if(goldEventMentionList!=null) treeForSortEventGoldMentions.addAll(goldEventMentionList);
	    
	    if(!treeForSortGoldMentions.isEmpty()){
	      for(EntityMention e : treeForSortGoldMentions){
	        Mention men = new Mention();
	        men.dependency = s.get(CollapsedDependenciesAnnotation.class);
	        men.startIndex = e.getExtentTokenStart();
	        men.endIndex = e.getExtentTokenEnd();

	        String[] parseID = e.getObjectId().split(":");
	        //men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	        men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	        String[] parseCorefID = e.getCorefID().split(":");
	        men.goldCorefClusterID = Integer.parseInt(parseCorefID[parseCorefID.length-1]);
	        men.originalRef = -1;
	        
	        for(int j=allGoldMentions.size()-1 ; j>=0 ; j--){
	          List<Mention> l = allGoldMentions.get(j);
	          for(int k=l.size()-1 ; k>=0 ; k--){
	            Mention m = l.get(k);
	            if(men.goldCorefClusterID == m.goldCorefClusterID){
	              men.originalRef = m.mentionID;
	            }
	          }
	        }
	        goldMentions.add(men);
	        if(men.mentionID > maxID) maxID = men.mentionID;
	        
	        // set ner type
	        for(int j = e.getExtentTokenStart() ; j < e.getExtentTokenEnd() ; j++){
	          CoreLabel word = words.get(j);
	          String ner = e.getType() +"-"+ e.getSubType();              
	          if(Constants.USE_GOLD_NE){
	            word.set(EntityTypeAnnotation.class, e.getMentionType());
	            if(e.getMentionType().equals("NAM")) word.set(NamedEntityTagAnnotation.class, ner);
	          }
	        }
	      }
	    }
	    
	    if(!treeForSortEventGoldMentions.isEmpty()){
		      for(EventMention e : treeForSortEventGoldMentions){
		        Mention men = new Mention();
		        men.dependency = s.get(CollapsedDependenciesAnnotation.class);
		        men.startIndex = e.getExtentTokenStart();
		        men.endIndex = e.getExtentTokenEnd();

		        String[] parseID = e.getObjectId().split(":");
		        //men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
		        men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
		        String[] parseCorefID = e.getObjectId().split(":");
		        String corefID = parseCorefID[1];
		        int length = corefID.length();
		        String CorefClusterID = corefID.substring(1, length);
		        if (CorefClusterID.endsWith("*")) {
		        	CorefClusterID = CorefClusterID.substring(0, CorefClusterID.length()-1);
		        	CorefClusterID += "000";
		        }
		        
		        men.goldCorefClusterID = Integer.parseInt(CorefClusterID);
		        men.originalRef = -1;
		        
		        for(int j=allGoldMentions.size()-1 ; j>=0 ; j--){
		          List<Mention> l = allGoldMentions.get(j);
		          for(int k=l.size()-1 ; k>=0 ; k--){
		            Mention m = l.get(k);
		            if(men.goldCorefClusterID == m.goldCorefClusterID){
		              men.originalRef = m.mentionID;
		            }
		          }
		        }
		        goldMentions.add(men);
		        if(men.mentionID > maxID) maxID = men.mentionID;
		      }
		    }
	  }
	
}
