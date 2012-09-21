package edu.oregonstate;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.didion.jwnl.JWNL;
import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;

public class EventCoreferencePipeline {

	public static final Logger logger = Logger.getLogger(EventCoreferencePipeline.class.getName());
	
	// in order to compare the result with the Stanford system
	public static String sieve = "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, " +
								 "StrictHeadMatch4, RelaxedHeadMatch, PronounMatch";
	
	// for scoring
	public Document corpus;
	
	// for mention words feature
	public static Map<String, List<String>> datas;
	
	// set all configuration
	public EventCoreferencePipeline() {
		corpus = new Document();
		if (GlobalConstantVariables.REPLICATE_STANFORD_EXPERIMENT) {
			configureJWordNet();
		}
	}
	
	public void configureJWordNet() {
		try {
			System.out.println("begin configure WORDNET");
			JWNL.initialize(new FileInputStream(GlobalConstantVariables.WORD_NET_CONFIGURATION_PATH));
			System.out.println("finish configure WORDNET");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
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
	
	public static void configureWordSimilarity() {
		WordSimilarity wordSimilarity = new WordSimilarity(GlobalConstantVariables.WORD_SIMILARITY_PATH);
		wordSimilarity.initialize();
		datas = wordSimilarity.datas;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println("Start........................");
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		logger.fine(timeStamp);
		
		configureWordSimilarity();
		EventCoreferencePipeline ec = new EventCoreferencePipeline();
		
		// beam search width : 100; the maximum number of expansion: 200
		// if the noOfIteration is Integer.MAX_VALUE, then we just need to reach the gold state
		// 200 maybe not enough for reaching the gold state
		GlobalConstantVariables.OWN_EXPERIMENT = true;
		String[] parameters = {"50-1"};
		
	    CRC_MAIN.deleteResult(GlobalConstantVariables.RESULT_PATH);  // delete the intermediate results
	    String[] topics = CRC_MAIN.getTopics(GlobalConstantVariables.WHOLE_CORPUS_PATH);
	    
	    // Execute how many experiments
	    for (String parameter : parameters) {
	    	System.out.println("Configuration parameters :" + parameter);
	    	
		    for (String topic : topics) {
		    	System.out.println("begin to process topic" + topic + "................");
		    	// apply high preicision sieves phase
		    	CorefSystem cs = new CorefSystem(EventCoreferencePipeline.sieve);
		    	Document topicDocument = cs.getDocument(topic);
		    	cs.corefSystem.coref(topicDocument);
			    
			    // add the four fields into the corpus data structure
			    ec.add(topicDocument);
			    System.out.println("end to process topic" + topic + "................");
		    }
		    
		    // evaluate
		    // one question: whether we need to post-process
		    // remove all singleton clusters, and apposition/ copular relations before scoring
		    CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
	    	score.calculateScore(ec.corpus);
	    	score.printF1(logger, true);
	    	
	    	CorefScorer ceafscore = new ScorerCEAF();
	    	ceafscore.calculateScore(ec.corpus);
	    	ceafscore.printF1(logger, true);

	    	CorefScorer mucscore = new ScorerMUC();
	    	mucscore.calculateScore(ec.corpus);
	    	mucscore.printF1(logger, true);
	    	
	    	CorefScorer pairscore = new ScorerPairwise();
	    	pairscore.calculateScore(ec.corpus);
	    	pairscore.printF1(logger, true);
	    	
	    	double conllF11 = (score.getF1() + ceafscore.getF1() + mucscore.getF1()) / 3;    
	    	System.out.println("conllF1:     " + conllF11 );
	    	
	    	System.out.println("do post processing");
	    	CorefSystem cs = new CorefSystem();
	    	cs.corefSystem.postProcessing(ec.corpus);
	    	
	    	CorefScorer postmucscore = new ScorerMUC();
	    	postmucscore.calculateScore(ec.corpus);
	    	postmucscore.printF1(logger, true);
	    	
	    	CorefScorer postpairscore = new ScorerPairwise();
	    	postpairscore.calculateScore(ec.corpus);
	    	postpairscore.printF1(logger, true);

	    	// Average of MUC, B^{3} and CEAF-\phi_{4}.
	    	double conllF1 = (score.getF1() + ceafscore.getF1() + postmucscore.getF1()) / 3;
	    	System.out.println("conllF1:     " + conllF1 );
		
	    	logger.info("Done: ===================================================");
			System.out.println("End.........................");
	    }
	}
	
}
