package edu.oregonstate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.oregonstate.ie.dcoref.EECBMentionExtractor;
import edu.oregonstate.ie.dcoref.MentionExtractor;
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
	
	/**
	 * If true, we score the output of the given test document
	 * Assumes gold annotations are available
	 * 
	 */
	private final boolean doScore;
	
	/**
	 * automatically set by looking at sieves
	 */
	private final boolean useSemantics;
	
	/**
	 * Array of sieve passes to be used in the system
	 * Ordered from highest precision to lowest
	 * See paper, they use 7 sieves
	 */
	private final DeterministicCorefSieve [] sieves;
	private final String [] sieveClassNames;
	
	/**
	 * Dictionaries of all the useful goodies (gener, animacy, number etc)
	 */
	private final Dictionaries dictionaries;
	
	/**
	 * Semantic knowledge : wordnet
	 */
	private final Semantics semantics;
	
	/**
	 * Current sieve index
	 */
	public int currentSieve;
	
	/** counter for links in passes (Pair<correct links, total links>) */
	public List<Pair<Integer, Integer>> linksCountInPass;
	
	/** scores for each pass */
	public List<CorefScorer> scorePairwise;
	public List<CorefScorer> scoreBcubed;
	public List<CorefScorer> scoreMUC;
	
	private List<CorefScorer> scoreSingleDoc;
	
	/** Additional scoring stats */
	int additionalCorrectLinksCount;
	int additionalLinksCount;
	
	public CRC_MAIN(Properties props) throws Exception {
		// initialize required fields
		currentSieve = -1;
		
		linksCountInPass = new ArrayList<Pair<Integer,Integer>>();
		scorePairwise = new ArrayList<CorefScorer>();
		scoreBcubed = new ArrayList<CorefScorer>();
		scoreMUC = new ArrayList<CorefScorer>();
		
		/** construct the sieve passes */
		String sievePasses = props.getProperty(Constants.SIEVES_PROP, Constants.SIEVEPASSES);
	    sieveClassNames = sievePasses.trim().split(",\\s*");
	    sieves = new DeterministicCorefSieve[sieveClassNames.length];
	    for(int i = 0; i < sieveClassNames.length; i ++){
	      sieves[i] = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses."+sieveClassNames[i]).getConstructor().newInstance();
	      sieves[i].init(props);
	    }
	    
	    //
	    // create scoring framework
	    //
	    doScore = Boolean.parseBoolean(props.getProperty(Constants.SCORE_PROP, "false"));
	    
	    //
	    // set useWordNet
	    //
	    useSemantics = sievePasses.contains("AliasMatch") || sievePasses.contains("LexicalChainMatch");
	    
	    if(doScore){
	        for(int i = 0 ; i < sieveClassNames.length ; i++){
	          scorePairwise.add(new ScorerPairwise());
	          scoreBcubed.add(new ScorerBCubed(BCubedType.B0));
	          scoreMUC.add(new ScorerMUC());
	          linksCountInPass.add(new Pair<Integer, Integer>(0, 0));
	        }
	      }
	    
	    //
	    // load all dictionaries
	    //
	    dictionaries = new Dictionaries(props);
	    semantics = (useSemantics)? new Semantics(dictionaries) : null;
	    
	    
	}
	
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
		args = new String[1];
		args[0] = GlobalConstantVariables.CONFIG_PATH + "coref.properties";
		Properties props = StringUtils.argsToProperties(args);
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
	    MentionExtractor mentionExtractor = null;
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
	    
	    logger.info("Done: ===================================================");
	}
	
}
