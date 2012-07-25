package edu.oregonstate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	 * 
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
		// Deterministic sieves in step 6 of Algorithm 1, apply Pronoun Match after cross document coreference resolution
		props.put("dcoref.sievePasses", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2," +
				"StrictHeadMatch3, StrictHeadMatch4, AliasMatch, RelaxedHeadMatch, LexicalChainMatch");
				
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
	    
	    // Extract the mention and gold mentions from the EECB 1.0 corpus
	    // In our case, the props contains the 
	    EmentionExtractor mentionExtractor = null;
	    mentionExtractor = new EECBMentionExtractor(parser, corefSystem.dictionaries(), props, corefSystem.semantics());
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
	    
	    //
	    // Parse one document at a time, and do single-doc coreference resolution in each
	    //
	    Document document;
	    
	    //
	    // In one iteration, orderedMentionsBySentence contains a list of all
	    // mentions in one document. Each mention has properties (annotations):
	    // its surface form (Word), NER Tag, POS Tag, Index, etc.
	    //
	    while (true) {
	    	document = mentionExtractor.nextDoc();
	    	if (document == null) break;
	    	//printDiscourseStructure(document);
	    	if(corefSystem.doScore()){
	            document.extractGoldCorefClusters();
	        }
	    	// run mention detection only
	        if(Constants.SKIP_COREF) {
	          continue;
	        }
	    }
	    
	    corefSystem.coref(document);  // Do Coreference Resolution using the self defined coreference method

	    if(corefSystem.doScore()){
	        //Identifying possible coreferring mentions in the corpus along with any recall/precision errors with gold corpus
	    	corefSystem.printTopK(logger, document, corefSystem.semantics());

	        logger.fine("pairwise score for this doc: ");
	        corefSystem.getScoreSingleDoc().get(corefSystem.getSieves().length-1).printF1(logger);
	        logger.fine("accumulated score: ");
	        corefSystem.printF1(true);
	        logger.fine("\n");
	    }
	    
	    logger.info("Done: ===================================================");
	}
	
}
