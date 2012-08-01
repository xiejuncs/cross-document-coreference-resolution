package edu.oregonstate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

import edu.oregonstate.domains.eecb.reader.EecbTopic;
import edu.oregonstate.ie.dcoref.EECBMentionExtractor;
import edu.oregonstate.ie.dcoref.EmentionExtractor;
import edu.oregonstate.util.EECB_Constants;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem.LogFormatter;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.DefaultPaths;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.StringUtils;

/**
 * The main entry point for cross document coreference resolution
 * 
 * <p>
 * The dataset is EECB 1.0, which is annotated by the Stanford NLP group on the basis of ECB 
 * corpus created by Bejan and Harabagiu (2010).
 * The idea of the paper, Joint Entity and Event Coreference Resolution across Documents, 
 * is to model entity and event jointly in an iterative way.
 * 
 * <p>
 * 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CRC_MAIN {
	
	public static final Logger logger = Logger.getLogger(CRC_MAIN.class.getName());
	
	private static LexicalizedParser makeParser(Properties props) {
	    int maxLen = Integer.parseInt(props.getProperty(Constants.PARSER_MAXLEN_PROP, "100"));
	    String[] options = {"-maxLength", Integer.toString(maxLen)};
	    LexicalizedParser parser = LexicalizedParser.loadModel(props.getProperty(Constants.PARSER_MODEL_PROP, DefaultPaths.DEFAULT_PARSER_MODEL), options);
	    return parser;
	}
	
	/**
	 * Stick to my own implementation, not think about converting to ACE 2005 style
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		logger.info("Start: ============================================================");
		
		/**
		 * The configuration for EECB corpus
		 */
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("dcoref.eecb", GlobalConstantVariables.CORPUS_PATH);
		props.put("dcoref.score", "true");
		// Deterministic sieves in step 6 of Algorithm 1, apply Pronoun Match after cross document coreference resolution
		// Hence, in this way, in the final part, we need to create a Stanford CoreNLP again.
		//props.put("dcoref.sievePasses", "MarkRole, DiscourseMatch");
		//props.put("dcoref.sievePasses", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch");
		
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		
		/** initialize logger */
	    FileHandler fh;
	    try {
	      String logFileName = props.getProperty(Constants.LOG_PROP, "log.txt");
	      if(logFileName.endsWith(".txt")) {
	        logFileName = logFileName.substring(0, logFileName.length()-4) +"_"+ timeStamp+".txt";
	      } else {
	        logFileName = logFileName + "_"+ timeStamp+".txt";
	      }
	      fh = new FileHandler(logFileName, false);
	      logger.addHandler(fh);
	      logger.setLevel(Level.FINE);
	      fh.setFormatter(new LogFormatter());
	    } catch (SecurityException e) {
	      System.err.println("ERROR: cannot initialize logger!");
	      throw e;
	    } catch (IOException e) {
	      System.err.println("ERROR: cannot initialize logger!");
	      throw e;
	    }
	    
	    logger.fine(timeStamp);
	    logger.fine(props.toString());
	    Constants.printConstants(logger);
	   
	    // initialize coref system
	    SieveCoreferenceSystem corefSystem = new SieveCoreferenceSystem(props);
	    // Load the Stanford Parser
	    LexicalizedParser parser = makeParser(props);
	    
	    /** 
	     * Cluster of Documents, In the current time, we just assume that each topic is a cluster
	     * 
	     * Algorithm 1: Joint Coreference Resolution 
	     * foreach document cluster c in C 
	     */
	    String[] topics = getTopics(GlobalConstantVariables.CORPUS_PATH);
	    for (String topic : topics) {
	    	// Extract the mention and gold mentions from the EECB 1.0 corpus
		    // In our case, the props contains the
		    // Use default mention finder : Rule based (need to be modified for VERB. In the current time,
		   	// <b>NOTED</b> Only nominal, pronominal and verbal mention will be extracted
		    EmentionExtractor mentionExtractor = null;
		    mentionExtractor = new EECBMentionExtractor(topic, parser, corefSystem.dictionaries(), props, corefSystem.semantics());
		    
		    assert mentionExtractor != null;
		    if (!EECB_Constants.USE_GOLD_MENTIONS) {
		    	// Set mention finder
		    	String mentionFinderClass = props.getProperty(Constants.MENTION_FINDER_PROP);
		    	if (mentionFinderClass != null) {
		            String mentionFinderPropFilename = props.getProperty(Constants.MENTION_FINDER_PROPFILE_PROP);
		            CorefMentionFinder mentionFinder;
		            if (mentionFinderPropFilename != null) {
		              Properties mentionFinderProps = new Properties();
		              mentionFinderProps.load(new FileInputStream(mentionFinderPropFilename));
		              mentionFinder = (CorefMentionFinder) Class.forName(mentionFinderClass).getConstructor(Properties.class).newInstance(mentionFinderProps);
		            } else {
		              mentionFinder = (CorefMentionFinder) Class.forName(mentionFinderClass).newInstance();
		            }
		            mentionExtractor.setMentionFinder(mentionFinder);
		    	}
		    	if (mentionExtractor.mentionFinder == null) {
		            logger.warning("No mention finder specified, but not using gold mentions");
		    	}
		    }
		   
		    // Parse one document at a time, and do single-doc coreference resolution in each
		    Document document = mentionExtractor.inistantiate(mentionExtractor, topic);
		    document.extractGoldCorefClusters();
		    corefSystem.coref(document);  // Do Coreference Resolution using the self defined coreference method
		    if(corefSystem.doScore()){
		        //Identifying possible coreferring mentions in the corpus along with any recall/precision errors with gold corpus
		    	corefSystem.printTopK(logger, document, corefSystem.semantics());

		        logger.fine("pairwise score for this doc: ");
		        System.out.println("pairwise score for this doc: ");
		        corefSystem.getScoreSingleDoc().get(corefSystem.getSieves().length-1).printF1(logger);
		        logger.fine("accumulated score: ");
		        System.out.println("accumulated score: ");
		        corefSystem.printF1(true);
		        logger.fine("\n");
		    }
	    }
	    
	    logger.info("Done: ===================================================");
	}
	
	// GET topics from the corpusPath
	private static String[] getTopics(String corpusPath) {
		File corpusDir = new File(corpusPath);
		String[] directories = corpusDir.list();
		// sort the arrays in order to execute in directory sequence
		Arrays.sort(directories);	
		return directories;
	}
	
}
