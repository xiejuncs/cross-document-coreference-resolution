package edu.oregonstate;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import Jama.Matrix;

import edu.oregonstate.features.Feature;
import edu.oregonstate.io.EECBMentionExtractor;
import edu.oregonstate.io.EmentionExtractor;
import edu.oregonstate.search.IterativeResolution;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.oregonstate.util.EECB_Constants;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem.LogFormatter;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.DefaultPaths;

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
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */

public class CRC_MAIN {
	
	public static final Logger logger = Logger.getLogger(CRC_MAIN.class.getName());
	public static Properties props; // in order to use for later methods
	public static SieveCoreferenceSystem corefSystem;
	public static LexicalizedParser parser;
	public static boolean printScore = false;
	
	
	public static LexicalizedParser makeParser(Properties props) {
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
		
		//The configuration for EECB corpus, 
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("dcoref.eecb", GlobalConstantVariables.CORPUS_PATH);
		props.put("dcoref.score", "true");
		props.put("dcoref.sievePasses", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch");
		
		// the initialization of corefSystem
		corefSystem = new SieveCoreferenceSystem(props);
	    parser = makeParser(props);
		
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
	    
	    // delete the intermediate results
	    deleteResult(GlobalConstantVariables.RESULT_PATH);
	    String[] topics = getTopics(GlobalConstantVariables.CORPUS_PATH);
	    EventCoreference ec = new EventCoreference();
	    // train the model
	    Train train = new Train(ec, topics, 10, 1.0, 0.7);
	    Matrix initialmodel = train.assignInitialWeights();
	    Matrix model = train.train(initialmodel);
	    
	    for (String topic : topics) {
		    // Parse one document at a time, and do single-doc coreference resolution in each
		    Document document = getDocument(topic);
		    //document.extractGoldCorefClusters();
		    corefSystem.coref(document); 
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
		    
		    printScore = true;
		    // Get the coref Clusters from the document, and then apply the iterative event/entity coreference
		    JointCoreferenceResolution ir = new JointCoreferenceResolution(document, corefSystem.dictionaries(), model);
		    ir.merge(corefSystem.dictionaries());
		 
		    // TODO Pronoun part
		    DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
		    corefSystem.coreference(document, pronounSieve);
		    
		    if(CRC_MAIN.printScore){
		    	
		    	CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
		    	score.calculateScore(document);
		    	score.printF1(CRC_MAIN.logger, true);
		    	
		    	corefSystem.postProcessing(document);
		    	
		    	CorefScorer mucscore = new ScorerMUC();
		    	mucscore.calculateScore(document);
		    	mucscore.printF1(CRC_MAIN.logger, true);
		    	
		    	CorefScorer pairscore = new ScorerPairwise();
		    	pairscore.calculateScore(document);
		    	pairscore.printF1(CRC_MAIN.logger, true);
		    }
	    }
	    
	    printModel(model, Feature.featuresName);
	    logger.info("Done: ===================================================");
	}
	
	public static void printModel(Matrix model, String[] featureName) {
		System.out.println("bias weight: " + model.get(0, 0));
		for (int i = 0; i < featureName.length; i++) {
			System.out.println(featureName[i] + " weight: " + model.get(i+1, 0));
		}
	}
	
	// delete the intermediate result in case of wrong linear model
	public static void deleteResult(String directoryName) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		for (File file : files) {
			if (!file.delete()) {
				System.out.println("Failed to delete "+file);
			}
		}
	}
	
	/**
	 * according to the topic, create a Document representation
	 * 
	 * @param topic
	 * @return
	 * @throws Exception
	 */
	public static Document getDocument(String topic) throws Exception {
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
	    Document document = mentionExtractor.inistantiate(topic);
	    
	    return document;
	}
	
	// GET topics from the corpusPath
	public static String[] getTopics(String corpusPath) {
		File corpusDir = new File(corpusPath);
		String[] directories = corpusDir.list();
		// sort the arrays in order to execute in directory sequence
		// sort string array and sort int array are different.
		// Hence, I need to convert the string array to int array first, and then transform back
		int[] dirs = new int[directories.length];
		for (int i = 0; i < directories.length; i++) {
			dirs[i] = Integer.parseInt(directories[i]);
		}
		Arrays.sort(dirs);
		for (int i = 0; i < directories.length; i++) {
			directories[i] = Integer.toString(dirs[i]);
		}
		return directories;
	}
	
}
