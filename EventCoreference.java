package edu.oregonstate;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.didion.jwnl.JWNL;
import Jama.Matrix;

import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.features.Feature;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.BestBeamSearch;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.oregonstate.training.TrainHeuristicFunction;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.dcoref.sievepasses.DeterministicCorefSieve;

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
	public static Map<String, List<String>> datas;
	
	public static boolean printScore = false;
	public boolean linearregression = true;
	public boolean beamsearch = false;
	
	// set all configuration
	public EventCoreference() {
		corpus = new Document();
	    configureJWordNet();
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
	
	/*
	 * Algorithm 1 : Joint co-reference resolution
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Start........................");
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		logger.fine(timeStamp);
		
		WordSimilarity wordSimilarity = new WordSimilarity(GlobalConstantVariables.WORD_SIMILARITY_PATH);
		wordSimilarity.initialize();
		datas = wordSimilarity.datas;
		
		EventCoreference ec = new EventCoreference();
		
		//TODO
		// configuration part, set ec.linearregression, ec.beamsearch variable and ec.printScore
		
		assert ec.linearregression != ec.beamsearch;
		int beamWidth = 5;
		// delete the intermediate results
	    CRC_MAIN.deleteResult(GlobalConstantVariables.RESULT_PATH);
	    String[] topics = CRC_MAIN.getTopics(GlobalConstantVariables.WHOLE_CORPUS_PATH);
	    
	    Matrix model = new Matrix(Feature.featuresName.length + 1, 1);
	    // train the model
	    if (ec.linearregression) {
	    	Train train = new Train( topics, 10, 1.0, 0.7);
	    	Matrix initialmodel = train.assignInitialWeights();
	    	model = train.train(initialmodel);
	    }
	    
	    if (ec.beamsearch) {
	    	TrainHeuristicFunction thf = new TrainHeuristicFunction(10, topics, model, beamWidth, true);
	    	model = thf.train();
	    }
	    
	    for (String topic : topics) {
	    	System.out.println("begin to process topic" + topic + "................");
	    	// apply high preicision sieves phase
	    	CorefSystem cs = new CorefSystem();
	    	Document topicDocument = cs.getDocument(topic);
	    	cs.corefSystem.coref(topicDocument);
	    	if(cs.corefSystem.doScore()){
		        System.out.println("accumulated score: \n");
		        cs.corefSystem.printF1(true);
		        System.out.println("\n");
		    }

	    	// iterative event/entity co-reference
	    	// flag variable : linearregression, if true, then do replicate the Stanford's experiment,
	    	// if not, then learn a heuristic function
	    	if (ec.linearregression) {
	    		JointCoreferenceResolution ir = new JointCoreferenceResolution(topicDocument, cs.corefSystem.dictionaries(), model);
	    		ir.merge(cs.corefSystem.dictionaries());
	    	}
	    	
	    	// structured perceptron without bias, just set bias as 0
	    	if (ec.beamsearch) {
	    		BestBeamSearch beamSearch = new BestBeamSearch(topicDocument, cs.corefSystem.dictionaries(), model, beamWidth);
	    		beamSearch.search();
	    	}
		    
		    // pronoun sieves
		    DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
		    cs.corefSystem.coreference(topicDocument, pronounSieve);
		    
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

    	//corefSystem.postProcessing(document);
    	CorefScorer mucscore = new ScorerMUC();
    	mucscore.calculateScore(ec.corpus);
    	mucscore.printF1(logger, true);
    	
    	CorefScorer pairscore = new ScorerPairwise();
    	pairscore.calculateScore(ec.corpus);
    	pairscore.printF1(logger, true);
    	
    	if (ec.linearregression) {
    		CRC_MAIN.printModel(model, Feature.featuresName);
    	}
    	
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
	
	CorefScorer postceafscore = new ScorerCEAF();
	postceafscore.calculateScore(ec.corpus);
	postceafscore.printF1(logger, true);
	
    	logger.info("Done: ===================================================");
		System.out.println("End.........................");
	}
	
}
