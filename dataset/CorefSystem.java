package edu.oregonstate.dataset;

import java.io.FileInputStream;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.EECBMentionExtractor;
import edu.oregonstate.io.EmentionExtractor;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.DefaultPaths;

/**
 * Implementation of SieveCorefSystem which can be used across the whole code base
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CorefSystem {

	/** Experiment Properties */
	private final Properties mExperimentProps;
	
	// SieveCorefCoreferenceSystem
	private SieveCoreferenceSystem corefSystem;
	
	// parser
	private LexicalizedParser parser;
	
	// coref system propery
	private Properties props;
	
	// use the default sieve configuration
	public CorefSystem() {
		mExperimentProps = ExperimentConstructor.experimentProps;
		String sieve = "";
		if (mExperimentProps.getProperty(EecbConstants.SIEVES_PROP).equals("partial")) {
			sieve = EecbConstants.PARTIAL_SIEVE_STRING;
		} else {
			sieve = EecbConstants.FULL_SIEVE_STRING;
		}
		
		generateCorefSystem(sieve);
	}
	
	private void generateCorefSystem(String sieve) {
		props = setProperties(sieve);
		
		setCorefSystem(props);
		
		// set parser for coref system
		parser = makeParser(props);
	}
	
	private Properties setProperties(String sieve) {
		Properties props = new Properties();
		props.setProperty("annotators", mExperimentProps.getProperty(EecbConstants.ANNOTATORS_PROP));
		
		boolean debug = Boolean.parseBoolean(mExperimentProps.getProperty(EecbConstants.DEBUG_PROP, "false"));
		String corpusPath = "";
		if (debug) {
			corpusPath = EecbConstants.LOCAL_CORPUS_PATH;
		} else {
			corpusPath = EecbConstants.CLUSTER_CPRPUS_PATH;
		}
		String dataPath = corpusPath + "corpus/EECB1.0/data/";
		props.setProperty("dcoref.eecb", dataPath);
		props.setProperty("dcoref.score", mExperimentProps.getProperty(EecbConstants.SCORE_PROP));
		props.setProperty("dcoref.sievePasses", sieve);
		
		return props;
	}
	
	
	private void setCorefSystem(Properties props) {
		try {
			corefSystem = new SieveCoreferenceSystem(props);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected LexicalizedParser makeParser(Properties props) {
	    int maxLen = Integer.parseInt(props.getProperty(Constants.PARSER_MAXLEN_PROP, "100"));
	    String[] options = {"-maxLength", Integer.toString(maxLen)};
	    LexicalizedParser parser = LexicalizedParser.loadModel(props.getProperty(Constants.PARSER_MODEL_PROP, DefaultPaths.DEFAULT_PARSER_MODEL), options);
	    return parser;
	}
	
	/**
	 * return the whole topic which consists of documents
	 * 
	 * @param topic
	 * @return
	 * @throws Exception
	 */
	public Document getDocument(String topic, boolean goldOnly) throws Exception {
		EmentionExtractor mentionExtractor = null;
	    mentionExtractor = new EECBMentionExtractor(topic, parser, corefSystem.dictionaries(), props, corefSystem.semantics(), goldOnly);
	    
	    assert mentionExtractor != null;
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
	        System.out.println("No mention finder specified, but not using gold mentions");
	    }
	    // Parse one document at a time, and do single-doc coreference resolution in each
	    Document document = mentionExtractor.inistantiate(topic);
	    
	    return document;
	}
	
	public SieveCoreferenceSystem getCorefSystem() {
		return corefSystem;
	}
	
}
