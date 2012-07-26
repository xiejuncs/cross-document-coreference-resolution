package edu.oregonstate.ie.dcoref;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
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
	protected String[] files;
	
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
		// Output 2.eecb and 1.eecb
		files = new File(topicPath).list();
	}
	
	/**
	 * inistantiate the EecbTopic object
	 * 
	 * extract all mentions in one doc cluster, represented by Mention 
	 */
	public EecbTopic inistantiate(EmentionExtractor mentionExtractor) throws Exception {
		EecbTopic eecbTopic = new EecbTopic();
		Document document;
		while (true) {
	    	document = mentionExtractor.nextDoc();
	    	if (document == null) break;
	    	document.extractGoldCorefClusters();
	    	addDocument(eecbTopic, document);
	    }

		return eecbTopic;
	}
	
	/**
	 * add the according fields of document into eecbTopic, which will be used to conduct
	 * high-precision deterministic sieves in the sixth step in the Algorithm 1 
	 * 
	 * @param eecbTopic
	 * @param document
	 */
	public void addDocument(EecbTopic eecbTopic, Document document) {
		
	}
	
	
	/**
	 * The method is designed for within document coreference resolution
	 */
	public Document nextDoc() throws Exception {
		List<List<CoreLabel>> allWords = new ArrayList<List<CoreLabel>>();
		List<List<Mention>> allGoldMentions = new ArrayList<List<Mention>>();
		List<List<Mention>> allPredictedMentions;
		List<Tree> allTrees = new ArrayList<Tree>();
		Annotation anno;
		try {
			String filename="";
		    while(files.length > fileIndex){
		        if(files[fileIndex].contains("eecb")) {
		        	filename = files[fileIndex];
		            fileIndex++;
		            break;
		        }
		        else {
		            fileIndex++;
		            filename="";
		        }
		    }
		    if(files.length <= fileIndex && filename.equals("")) return null;
		    
		    anno = eecbReader.parse(topicPath + filename);
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
		    printRawDoc(sentences, allPredictedMentions, filename, false);
			
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
		MentionExtractor mentionExtractor = new MentionExtractor(dictionaries, semantics);		
		return mentionExtractor.arrange(anno, allWords, allTrees, allPredictedMentions, allGoldMentions, true);
	}
	
	private void printRawDoc(List<CoreMap> sentences, List<List<Mention>> allMentions, String filename, boolean gold) throws FileNotFoundException {
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
	
	// Define the two variables for obtaining the list of file names
	public static int spc_count = 1;
	private static ArrayList<String> file;
	
	/**
	 * Given a corpus path, get the list of file names resides in the folder and its sub-folder
	 * 	 * 
	 * @param corpusPath
	 * @return
	 */
	private String[] read(String corpusPath) {
		file = new ArrayList<String>();
		File aFile = new File(corpusPath);
		Process(aFile);
		String[] fileArray = file.toArray(new String[file.size()]);
		return fileArray;
	}
	
	/**
	 * Iterate the folder and its sub-folder.
	 * If the File object is a directory, recursive
	 * If the File object is a file, then get its name
	 * 
	 * @param aFile
	 */
	public void Process(File aFile) {
		spc_count++;
		String spcs = "";
		for (int i = 0; i < spc_count; i++)
		      spcs += " ";
		if(aFile.isFile())
		    file.add(aFile.getParent() + "/" + aFile.getName());
			//System.out.println(spcs + "[FILE] " + aFile.getParent() + "/" + aFile.getName());
		else if (aFile.isDirectory()) {
			//System.out.println(spcs + "[DIR] " + aFile.getName());
		      File[] listOfFiles = aFile.listFiles();
		      if(listOfFiles!=null) {
		        for (int i = 0; i < listOfFiles.length; i++)
		          Process(listOfFiles[i]);
		      } else {
		    	  System.out.println(spcs + " [ACCESS DENIED]");
		      }
		}
		spc_count--;
	}
	
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
