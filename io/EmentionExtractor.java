package edu.oregonstate.io;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.util.Command;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EntityTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;

/**
 * generic mention extractor from a corpus
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EmentionExtractor {
	
	protected String currentDocumentID;
	protected int goldBaseID;
	protected int baseID;
	
	protected Dictionaries dictionaries;
	protected Semantics semantics;
	
	public CorefMentionFinder mentionFinder;
	protected StanfordCoreNLP stanfordProcessor;
	
	/** The maximum mention ID: for preventing duplicated mention ID assignment */
	protected int maxID = -1;
	protected Logger logger;
	protected EecbReader eecbReader;
	
	public static final boolean VERBOSE = false;
	protected String mentionRepositoryPath;
	
	protected boolean goldOnly;
	
	public EmentionExtractor(Dictionaries dict, Semantics semantics, boolean goldonly) {
		this.dictionaries = dict;
		this.semantics = semantics;
		logger = Logger.getLogger(EmentionExtractor.class.getName());
		this.mentionFinder = new RuleBasedCorefMentionFinder();
		goldOnly = goldonly;
		
		if (goldOnly) {
			mentionRepositoryPath = ExperimentConstructor.experimentResultFolder + "/mentionResult";
			Command.createDirectory(mentionRepositoryPath);
		}
	}
	
	protected class EntityComparator implements Comparator<EntityMention> {
	    public int compare(EntityMention m1, EntityMention m2){
	      if(m1.getExtentTokenStart() > m2.getExtentTokenStart()) return 1;
	      else if(m1.getExtentTokenStart() < m2.getExtentTokenStart()) return -1;
	      else if(m1.getExtentTokenEnd() > m2.getExtentTokenEnd()) return -1;
	      else if(m1.getExtentTokenEnd() < m2.getExtentTokenEnd()) return 1;
	      else return 0;
	    }
	}
	
	protected class EventComparator implements Comparator<EventMention> {
		public int compare(EventMention m1, EventMention m2){
		    if(m1.getAnchor().getExtentTokenEnd() > m2.getAnchor().getExtentTokenEnd()) return 1;
		    else if(m1.getAnchor().getExtentTokenEnd() < m2.getAnchor().getExtentTokenEnd()) return -1;
		    else if(m1.getAnchor().getExtentTokenEnd() > m2.getAnchor().getExtentTokenEnd()) return -1;
		    else if(m1.getAnchor().getExtentTokenEnd() < m2.getAnchor().getExtentTokenEnd()) return 1;
		    else return 0;
		}
	}
	
	/**
	 * Extracts the info relevant for coref from the next document in the corpus
	 * @return List of mentions found in each sentence ordered according to the tree traversal.
	 * @throws Exception 
	 */
	public Document inistantiate(String topic) throws Exception {
		return null;
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
		return null;
	}
	
	public void setMentionFinder(CorefMentionFinder mentionFinder)
	{
		this.mentionFinder = mentionFinder;
	}
	
	/** Load Stanford Processor: skip unnecessary annotator */
	protected StanfordCoreNLP loadStanfordProcessor(Properties props) {
	    boolean replicateCoNLL = Boolean.parseBoolean(props.getProperty(Constants.REPLICATECONLL_PROP, "false"));

	    Properties pipelineProps = new Properties(props);
	    StringBuilder annoSb = new StringBuilder("");
	    if (!Constants.USE_GOLD_POS && !replicateCoNLL)  {
	      annoSb.append("pos, lemma");
	    } else {
	      annoSb.append("lemma");
	    }
	    if(Constants.USE_TRUECASE) {
	      annoSb.append(", truecase");
	    }
	    if (!Constants.USE_GOLD_NE && !replicateCoNLL)  {
	      annoSb.append(", ner");
	    }
	    if (!Constants.USE_GOLD_PARSES && !replicateCoNLL)  {
	      annoSb.append(", parse");
	    }
	    String annoStr = annoSb.toString();
	    SieveCoreferenceSystem.logger.info("Ignoring specified annotators, using annotators=" + annoStr);
	    pipelineProps.put("annotators", annoStr);
	    return new StanfordCoreNLP(pipelineProps, false);
	}
	
	protected void printRawDoc(List<CoreMap> sentences, List<List<Mention>> allMentions, boolean gold) throws FileNotFoundException {
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
	    //System.out.println(doc.toString());
	  }
	
	/**
	 * Extract the gold mentions : here use treeset to remove so
	 * 
	 * if(!treeForSortGoldMentions.isEmpty()){
	      for(EntityMention e : treeForSortGoldMentions){
	      
	    if(!treeForSortEventGoldMentions.isEmpty()){
		      for(EventMention e : treeForSortEventGoldMentions){
	 * 
	 * @param s
	 * @param allGoldMentions
	 * @param comparator
	 */
	protected void extractGoldMentions(CoreMap s, List<List<Mention>> allGoldMentions, EntityComparator comparator, EventComparator eventComparator) {
		List<Mention> goldMentions = new ArrayList<Mention>();
	    allGoldMentions.add(goldMentions);
	    List<EntityMention> goldMentionList = s.get(MachineReadingAnnotations.EntityMentionsAnnotation.class);
	    List<EventMention> goldEventMentionList = s.get(MachineReadingAnnotations.EventMentionsAnnotation.class);
	    
	    List<CoreLabel> words = s.get(TokensAnnotation.class);
	    
	    if (goldMentionList != null) {
	    	if(!goldMentionList.isEmpty()){
	    		for(EntityMention e : goldMentionList){
	    			Mention men = new Mention();
	    			men.dependency = s.get(CollapsedDependenciesAnnotation.class);
	    			men.startIndex = e.getExtentTokenStart();
	    			men.endIndex = e.getExtentTokenEnd();

	    			String[] parseID = e.getObjectId().split("-");
	    			//men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	    			men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	    			String[] parseCorefID = e.getCorefID().split("-");
	    			men.goldCorefClusterID = Integer.parseInt(parseCorefID[parseCorefID.length-1].substring(1)) + goldBaseID;
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
	    }
	    
	    if (goldEventMentionList != null) {
	    	if(!goldEventMentionList.isEmpty()){
	    		for(EventMention e : goldEventMentionList){
	    			Mention men = new Mention();
	    			men.dependency = s.get(CollapsedDependenciesAnnotation.class);
	    			men.startIndex = e.getExtentTokenStart();
	    			men.endIndex = e.getExtentTokenEnd();
	    			men.isVerb = true;

	    			String[] parseID = e.getObjectId().split("-");
	    			//men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	    			men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	    			String[] parseCorefID = e.getObjectId().split("-");
	    			String corefID = parseCorefID[1];
	    			int length = corefID.length();
	    			String CorefClusterID = corefID.substring(1, length);
	    			if (CorefClusterID.endsWith("*")) {
	    				CorefClusterID = CorefClusterID.substring(0, CorefClusterID.length()-1);
	    				CorefClusterID += "000";
	    			}

	    			men.goldCorefClusterID = Integer.parseInt(CorefClusterID) + goldBaseID;
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
}
