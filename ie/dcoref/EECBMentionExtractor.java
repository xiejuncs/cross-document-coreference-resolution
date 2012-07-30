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
import edu.oregonstate.domains.eecb.reader.EecbTopic;
import edu.oregonstate.util.EECB_Constants;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
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
		// Output [1.eecb, 2.eecb]
		files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
		sort(files);
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
	public Document inistantiate(EmentionExtractor mentionExtractor, String topic) throws Exception {
		List<List<CoreLabel>> allWords = new ArrayList<List<CoreLabel>>();
		List<List<Mention>> allGoldMentions = new ArrayList<List<Mention>>();
		List<List<Mention>> allPredictedMentions = null;
		List<Tree> allTrees = new ArrayList<Tree>();
		Annotation anno = null;
		try {
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
		    	extractGoldMentions(sentence, allGoldMentions, comparator);
		    }
		    
		    allPredictedMentions = mentionFinder.extractPredictedMentions(anno, -1, dictionaries);
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
	
	/**
	 * Extract the gold mentions
	 * 
	 * @param s
	 * @param allGoldMentions
	 * @param comparator
	 */
	private void extractGoldMentions(CoreMap s, List<List<Mention>> allGoldMentions, EntityComparator comparator) {
		List<Mention> goldMentions = new ArrayList<Mention>();
	    allGoldMentions.add(goldMentions);
	    List<EntityMention> goldMentionList = s.get(MachineReadingAnnotations.EntityMentionsAnnotation.class);
	        
	    List<CoreLabel> words = s.get(TokensAnnotation.class);

	    TreeSet<EntityMention> treeForSortGoldMentions = new TreeSet<EntityMention>(comparator);
	    if(goldMentionList!=null) treeForSortGoldMentions.addAll(goldMentionList);
	    if(!treeForSortGoldMentions.isEmpty()){
	      for(EntityMention e : treeForSortGoldMentions){
	        Mention men = new Mention();
	        men.dependency = s.get(CollapsedDependenciesAnnotation.class);
	        men.startIndex = e.getExtentTokenStart();
	        men.endIndex = e.getExtentTokenEnd();
	        
	        String[] parseID = e.getObjectId().split(":");
	        //men.mentionID = Integer.parseInt(parseID[parseID.length-1]);
	        int topic = Integer.parseInt(parseID[0]);
	        int doc = Integer.parseInt(parseID[1]);
	        int start = Integer.parseInt(parseID[4]);
	        int end = Integer.parseInt(parseID[5]);
	        int id = topic * 100000000 + doc * 1000000 + start * 1000 + end;
	        men.mentionID = id;
	        men.corefClusterID = Integer.parseInt(parseID[3]);
	        //String[] parseCorefID = e.getCorefID().split("-E");
	        //men.goldCorefClusterID = Integer.parseInt(parseCorefID[parseCorefID.length-1]);
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
	
}
