package edu.oregonstate.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import edu.oregonstate.CDCR;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.EecbReader;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EntityTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.UtteranceAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.dcoref.MentionExtractor;

/**
 * Extract single topic <COREF> mentions from eecb corpus  
 */
public class EECBMentionExtractor extends EmentionExtractor {
	private String topicPath;
	protected ArrayList<String> files;

	public EECBMentionExtractor(String topic, LexicalizedParser p, Dictionaries dict, Properties props, Semantics semantics) throws Exception {
		super(dict, semantics);
		stanfordProcessor = loadStanfordProcessor(props);
		baseID = 1000000 * Integer.parseInt(topic);
		topicPath = props.getProperty(edu.oregonstate.util.EecbConstants.EECB_PROP, CDCR.corpusPath) + "/" + topic + "/";
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
			anno = eecbReader.read(files, topic);
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
		    
		    if (ExperimentConstructor.goldOnly) {
		    	// just use the gold mentions
		        allPredictedMentions = new ArrayList<List<Mention>>();
		        for (int i = 0; i < allGoldMentions.size(); i++) {
		        	List<Mention> sentence = new ArrayList<Mention>();
		        	for (int j = 0; j < allGoldMentions.get(i).size(); j++) {
		        		Mention mention = allGoldMentions.get(i).get(j);
		        		ResultOutput.serialize(mention, mention.mentionID, ExperimentConstructor.mentionResultPath);
		        		Mention copyMention = ResultOutput.deserialize(Integer.toString(mention.mentionID) + ".ser", ExperimentConstructor.mentionResultPath, true);
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
		dcorfMentionExtractor.setCurrentDocumentID(topic);
		Document document = dcorfMentionExtractor.arrange(anno, allWords, allTrees, allPredictedMentions, allGoldMentions, true);
		document.extractGoldCorefClusters();
		return document;
	}
	
	@Override
	public String toString() {
		return "EECBMentionExtractor: [ topicPath : " + topicPath + ", Length of file pool : " + files.size() +"]"; 
	}
	
}
