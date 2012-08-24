package edu.oregonstate;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.logging.Logger;

import Jama.Matrix;

import edu.oregonstate.features.Feature;
import edu.oregonstate.io.EECBMentionExtractor;
import edu.oregonstate.io.EmentionExtractor;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.oregonstate.util.EECB_Constants;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
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

public class EventCoreference {
	public static final Logger logger = Logger.getLogger(EventCoreference.class.getName());
	public Document corpus;
	public Properties props;
	public SieveCoreferenceSystem corefSystem;
	public LexicalizedParser parser;
	public static boolean printScore = false;
	
	// set all configuration
	public EventCoreference() {
		corpus = new Document();
		setProperties();
		setCorefSystem();
		parser = makeParser(props);
	}
	
	public void setProperties() {
		//The configuration for EECB corpus, 
		props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		props.put("dcoref.eecb", GlobalConstantVariables.WHOLE_CORPUS_PATH);
		props.put("dcoref.score", "true");
		props.put("dcoref.sievePasses", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch");
	}
	
	public void setCorefSystem() {
		try {
			corefSystem = new SieveCoreferenceSystem(props);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public LexicalizedParser makeParser(Properties props) {
	    int maxLen = Integer.parseInt(props.getProperty(Constants.PARSER_MAXLEN_PROP, "100"));
	    String[] options = {"-maxLength", Integer.toString(maxLen)};
	    LexicalizedParser parser = LexicalizedParser.loadModel(props.getProperty(Constants.PARSER_MODEL_PROP, DefaultPaths.DEFAULT_PARSER_MODEL), options);
	    return parser;
	}
	
	public Document getDocument(String topic) throws Exception {
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
	           System.out.println("No mention finder specified, but not using gold mentions");
	    	}
	    }
	    // Parse one document at a time, and do single-doc coreference resolution in each
	    Document document = mentionExtractor.inistantiate(topic);
	    
	    return document;
	}
	
	public void addCorefCluster(Document document) {
		  for(Integer id : document.corefClusters.keySet()) {
			  corpus.addCorefCluster(id, document.corefClusters.get(id));
		  }
	}
	
	public void addGoldCorefCluster(Document document) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefCluster(id, document.goldCorefClusters.get(id));
		}
	}
	
	public void addPredictedMention(Document document) {
		for (Integer id : document.allPredictedMentions.keySet()) {
			corpus.addPredictedMention(id, document.allPredictedMentions.get(id));
		}
	}

	public void addGoldMention(Document document) {
		for (Integer id : document.allGoldMentions.keySet()) {
			corpus.addGoldMention(id, document.allGoldMentions.get(id));
		}
	}
	
	/**
	 * add four fields to the corpus
	 * 
	 * @param document
	 */
	public void add(Document document) {
		addCorefCluster(document);
		addGoldCorefCluster(document);
		addPredictedMention(document);
		addGoldMention(document);
	}
	
	/*
	 * Algorithm 1 : Joint co-reference resolution
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Start........................");
		EventCoreference ec = new EventCoreference();
		// delete the intermediate results
	    CRC_MAIN.deleteResult(GlobalConstantVariables.RESULT_PATH);
	    String[] topics = CRC_MAIN.getTopics(GlobalConstantVariables.WHOLE_CORPUS_PATH);
		
	    // train the model
	    Train train = new Train( ec, topics, 10, 1.0, 0.7);
	    Matrix initialmodel = train.assignInitialWeights();
	    Matrix model = train.train(initialmodel);
	    
	    for (String topic : topics) {
	    	System.out.println("begin to process topic 1................");
	    	// apply high preicision sieves phase
	    	Document topicDocument = ec.getDocument(topic);
	    	ec.corefSystem.coref(topicDocument);
	    	if(ec.corefSystem.doScore()){
		        System.out.println("accumulated score: \n");
		        ec.corefSystem.printF1(true);
		        System.out.println("\n");
		    }
	    	
	    	// iterative event/entity co-reference
	    	JointCoreferenceResolution ir = new JointCoreferenceResolution(topicDocument, ec.corefSystem.dictionaries(), model);
		    ir.merge(ec.corefSystem.dictionaries());
		    
		    // pronoun sieves
		    DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
		    ec.corefSystem.coreference(topicDocument, pronounSieve);
		    
		    // add the four fields into the corpus data structure
		    ec.add(topicDocument);
		    System.out.println("end to process topic 1................");
	    }
		
	    // evaluate
	    // one question: whether we need to post-process
	    // remove all singleton clusters, and apposition/ copular relations before scoring
	    CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
    	score.calculateScore(ec.corpus);
    	score.printF1(logger, true);
    	
    	//corefSystem.postProcessing(document);
    	CorefScorer mucscore = new ScorerMUC();
    	mucscore.calculateScore(ec.corpus);
    	mucscore.printF1(logger, true);
    	
    	CorefScorer pairscore = new ScorerPairwise();
    	pairscore.calculateScore(ec.corpus);
    	pairscore.printF1(logger, true);
    	
    	CRC_MAIN.printModel(model, Feature.featuresName);
    	
		
		System.out.println("End.........................");
	}
	
}
