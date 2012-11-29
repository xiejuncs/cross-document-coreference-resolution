package edu.oregonstate;

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

	/** Coreference Resolution Properties */
	protected Properties props;
	
	public SieveCoreferenceSystem corefSystem;
	protected LexicalizedParser parser;
	
	// use the default sieve configuration
	public CorefSystem() {
		this(EecbConstants.PARTIAL_SIEVE_STRING);
	}
	
	
	public CorefSystem(String sieve) {
		setProperties(sieve);
		setCorefSystem();
		parser = makeParser(props);
	}
	
	protected void setProperties(String sieve) {
		//The configuration for EECB corpus, 
		props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.setProperty("dcoref.eecb", (String) ExperimentConstructor.getParameter(EecbConstants.DATASET, "corpusPath"));
		//TODO
		props.setProperty("dcoref.score", "false");
		props.setProperty("dcoref.sievePasses", (String) ExperimentConstructor.getParameter(EecbConstants.DATASET, "sieve"));
	}
	
	protected void setCorefSystem() {
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
	public Document getDocument(String topic) throws Exception {
		EmentionExtractor mentionExtractor = null;
	    mentionExtractor = new EECBMentionExtractor(topic, parser, corefSystem.dictionaries(), props, corefSystem.semantics());
	    
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
	
}
