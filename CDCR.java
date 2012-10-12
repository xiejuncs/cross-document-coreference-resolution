package edu.oregonstate;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import Jama.Matrix;
import net.didion.jwnl.JWNL;

import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.BestBeamSearch;
import edu.oregonstate.search.IterativeResolution;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.oregonstate.training.TrainHeuristicFunction;
import edu.oregonstate.training.TrainSingleDocumentHeuristicFunction;
import edu.oregonstate.util.Constants;
import edu.oregonstate.util.SystemOptions;
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
public class CDCR {

	public static final Logger logger = Logger.getLogger(CDCR.class.getName());
	
	/** Path for the corpus */
	public static String corpusPath;
	
	/** File name for output */
	public static String outputFileName;
	
	/** Path for the annotation file*/
	public static String annotationPath;
	
	/** Sieve Configuration */
	public static String sieve;
	
	/** flag for replicating Stanford experiment */
	public static boolean replicateStanford;
	
	/** flag for doing within coreference resolution */
	public static boolean doWithinCoreference;
	
	/** flag for doing beam search */
	public static boolean beamSearch;
	
	/** just use gold mentions */
	public static boolean goldOnly;
	
	/** whether ALIGN PREDICTED and GOLD */
	public static boolean alignPredictedGold;
	
	/** Align the predicted mentions and gold mentions by making the gold mentions and gold mentions having the equal mentions */
	public static boolean alignPredictedGoldEqual;
	
	/** Aligh the predicted mentions and gold mentions by putting spurious predicted mentions as singleton clusters in gold clusters */
	public static boolean alignPredictedGoldPartial;
	
	/** the configuration file for WORDNET */
	public String wordnetConfigurationPath;
	
	/** the intermediate result, the results are temporal, when the system runs, the folder will be cleaned */
	public static String resultPath;
	
	/** the path for SRL result */
	public static String srlPath;
	
	/** for scoring */
	public Document corpus;
	
	/** for mention words feature */
	public static Map<String, List<String>> datas;
	
	/** the path for word similarity dictionary */
	public String wordSimilarityPath;
	
	/** restricted or unrestricted case */
	public static boolean restricted;
	
	/** whether need to post-process the corpus */
	public static boolean postProcess;
	
	/** whether to incorporate the SRL result */
	public static boolean incorporateSRLResult;
	
	/** EecbTopic documentSentence Null case */
	public static boolean enableNull;
	
	public CDCR() {
		SystemOptions option = new SystemOptions();
		corpus = new Document();
		
		//
		// set the path for different knowledge sources
		//
		wordnetConfigurationPath = Constants.WORD_NET_CONFIGURATION_PATH;
		resultPath = Constants.RESULT_PATH;
		srlPath = Constants.TOKENS_OUTPUT_PATH;
		wordSimilarityPath = Constants.WORD_SIMILARITY_PATH;
		enableNull = option.ENABLE_NULL;
		
		//
		// set outputFileName
		//
		outputFileName = Constants.TEMPORY_RESULT_PATH + Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		ResultOutput.writeTextFile(outputFileName, Calendar.getInstance().getTime().toString().replaceAll("\\s", "-"));
	    
		//
		// set corpusPath and annotationPath
		//
		if (option.DEBUG) {
			corpusPath = Constants.DEBUG_CORPUS_PATH;
			annotationPath = Constants.DEBUG_MENTION_ANNOTATION_PATH;
		} else {
			corpusPath = Constants.WHOLE_CORPUS_PATH;
			annotationPath = Constants.WHOLE_MENTION_ANNOTATION_PATH;
		}
		
		//
		// set sieve
		//
		if (option.FULL_SIEVE) {
			sieve = Constants.FULL_SIEVE_STRING;
		} else {
			sieve = Constants.PARTIAL_SIEVE_STRING;
		}
		
		//
		// set replicate Stanford experiment
		//
		replicateStanford = option.REPLICATE_STANFORD_EXPERIMENT;
		if (replicateStanford) {
			configureJWordNet();
			configureWordSimilarity();
		}
		
		//
		// set within coreference resolution
		// 
		doWithinCoreference = option.WITHIN_COREFERNECE_RESOLUTION;
		//if (doWithinCoreference && option.DEBUG) {
		//	corpusPath = Constants.DEBUG_WITHIN_CORPUS_PATH;
		//	annotationPath = Constants.DEBUG_WITHIN_MENTION_ANNOTATION_PATH;
		//}
		
		//
		// set beam search
		//
		beamSearch = option.BEAM_SEARCH;
		
		//
		// set goldOnly
		//
		goldOnly = option.GOLD_ONLY;
		
		//
		// set alignPredictedGold
		//
		alignPredictedGold = option.ALIGN_PREDICTED_GOLD;
		
		//
		// set alignPredictedGoldEqual
		//
		alignPredictedGoldEqual = option.ALIGN_PREDICTED_GOLD_EQUAL;
		
		//
		// set alignPredictedGoldPartial
		//
		alignPredictedGoldPartial = option.ALIGN_PREDICTED_GOLD_PARTIAL;
		
		//
		// set restricted
		//
		restricted = option.RESTRICTED;
		
		//
		// set post Process
		//
		postProcess = option.POST_PROCESS;
		
		//
		//whether to incorporate the SRL result
		//
		incorporateSRLResult = option.SRL_RESULT;
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
	
	/** configurate the WORDNET */
	public void configureJWordNet() {
		try {
			System.out.println("begin configure WORDNET");
			JWNL.initialize(new FileInputStream(wordnetConfigurationPath));
			System.out.println("finish configure WORDNET");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
	/** configure word similarity matrix */
	public void configureWordSimilarity() {
		WordSimilarity wordSimilarity = new WordSimilarity(wordSimilarityPath);
		wordSimilarity.initialize();
		datas = wordSimilarity.datas;
	}
	
	/**
	 * The whole experiment, if I need to change the configuration
	 * Just need to update the SystemOptions
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		CDCR cdcr = new CDCR();
		//ResultOutput.deleteResult(resultPath);
		ResultOutput.writeTextFile(CDCR.outputFileName, "Start..........................");
		ResultOutput.writeTextFile(CDCR.outputFileName, "\n\n");
		
		String[] topics = ResultOutput.getTopics(corpusPath);
		/** whether replicate the Stanford experiment or conduct my own experiment*/
		if (replicateStanford) {
			ResultOutput.writeTextFile(CDCR.outputFileName, "Replicate stanford experiment");
			
			/** train the model and print them out */
			Train train = new Train( topics, 10, 1.0, 0.7);  // the L2 regularized linear regression configuration
			Matrix model = new Matrix(Feature.featuresName.length + 1, 1);
			Matrix initialmodel = train.assignInitialWeights();
		    ResultOutput.writeTextFile(CDCR.outputFileName, ResultOutput.printModel(initialmodel, Feature.featuresName));
		    model = train.train(initialmodel);
		    ResultOutput.writeTextFile(CDCR.outputFileName, ResultOutput.printModel(model, Feature.featuresName));
		    
			for (String topic : topics) {
				//TODO
				// replicate the Stanford experiment
				ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic" + topic + "................");
				CorefSystem cs = new CorefSystem();
				try {
					Document topicDocument = cs.getDocument(topic);
					cs.corefSystem.coref(topicDocument);
					
					// iterative event/entity co-reference
			    	// flag variable : linearregression, if true, then do replicate the Stanford's experiment,
			    	// if not, then learn a heuristic function
			    	IterativeResolution ir = new JointCoreferenceResolution(topicDocument, cs.corefSystem.dictionaries(), model);
			    	ir.merge(cs.corefSystem.dictionaries());

				    // pronoun sieves
				    DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
				    cs.corefSystem.coreference(topicDocument, pronounSieve);
				    
				    // add the four fields into the corpus data structure
				    cdcr.add(topicDocument);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				ResultOutput.writeTextFile(CDCR.outputFileName, "end to process topic" + topic + "................");
			}
			cdcr.printScore(cdcr.corpus);
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "Finish the processing, next state is to evaluation");
		} else {
			ResultOutput.writeTextFile(CDCR.outputFileName, "Search guided by the loss function begin.................");
			String[] parameters = {"30-1"};
			for (String parameter : parameters) {
				ResultOutput.writeTextFile(outputFileName, "Configuration parameters :" + parameter);
		    	String[] paras = parameter.split("-");
		    	String noOfIteration = paras[0];
		    	String width = paras[1];
		    	
				if (doWithinCoreference) {
					//TODO
					// within case 
					ResultOutput.writeTextFile(CDCR.outputFileName, "do within coreference first, and then cross coreference.....");
					/** train the model, the model is too big, so I do not print them out */
			    	ResultOutput.writeTextFile(CDCR.outputFileName, "training Phase................");
			    	TrainSingleDocumentHeuristicFunction tsdhf = new TrainSingleDocumentHeuristicFunction(Integer.parseInt(noOfIteration), topics, Integer.parseInt(width));
			    	Matrix model = tsdhf.train();
					
				} else {
					//TODO
					// cross case
					ResultOutput.writeTextFile(CDCR.outputFileName, "cross coreference directly");
				    	
			    	/** train the model, the model is too big, so I do not print them out */
			    	ResultOutput.writeTextFile(CDCR.outputFileName, "training Phase................");
				    TrainHeuristicFunction thf = new TrainHeuristicFunction(Integer.parseInt(noOfIteration), topics, Integer.parseInt(width));
				    Matrix model = thf.train();
				    
				    /*
				    ResultOutput.writeTextFile(CDCR.outputFileName, "Apply the learned cost function to the coreference resolution task................");
				    for (String topic : topics) {
				    	ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic" + topic + "................");
				    	// apply high preicision sieves phase
				    	CorefSystem cs = new CorefSystem(CDCR.sieve);
				    	try {
				    		Document topicDocument = cs.getDocument(topic);
				    		cs.corefSystem.coref(topicDocument);
				    	
				    		// structured perceptron without bias, just set bias as 0
				    		BestBeamSearch beamSearch = new BestBeamSearch(topicDocument, cs.corefSystem.dictionaries(), model, Integer.parseInt(width));
				    		beamSearch.search();
					    
				    		// pronoun sieves
						    DeterministicCorefSieve pronounSieve = (DeterministicCorefSieve) Class.forName("edu.stanford.nlp.dcoref.sievepasses.PronounMatch").getConstructor().newInstance();
						    cs.corefSystem.coreference(topicDocument, pronounSieve);
				    		
				    		cdcr.add(topicDocument);
				    	} catch (Exception e) {
							e.printStackTrace();
							System.exit(1);
						}
				    	ResultOutput.writeTextFile(CDCR.outputFileName, "end to process topic" + topic + "................");
				    }*/
				}
			}
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "Search guided by the loss function end.................");
		}

		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		ResultOutput.writeTextFile(CDCR.outputFileName, "\n\n");
		ResultOutput.writeTextFile(CDCR.outputFileName, timeStamp);
		ResultOutput.writeTextFile(CDCR.outputFileName, "End..............................");
		System.out.println("Done.........");
	}
	
	/** print score of the document, whether post-processing or not */
	public void printScore(Document document) {
		CorefScorer score = new ScorerBCubed(BCubedType.Bconll);
    	score.calculateScore(document);
    	score.printF1(logger, true);
    	
    	CorefScorer ceafscore = new ScorerCEAF();
    	ceafscore.calculateScore(document);
    	ceafscore.printF1(logger, true);

    	if (!postProcess) {
    		CorefScorer mucscore = new ScorerMUC();
    		mucscore.calculateScore(document);
    		mucscore.printF1(logger, true);
    	
    		CorefScorer pairscore = new ScorerPairwise();
    		pairscore.calculateScore(document);
    		pairscore.printF1(logger, true);
    		
    		// Average of MUC, B^{3} and CEAF-\phi_{4}.
    		double conllF1 = (score.getF1() + ceafscore.getF1() + mucscore.getF1()) / 3;
        	ResultOutput.writeTextFile(CDCR.outputFileName, "conllF1:     " + conllF1);
    	} else {
    		ResultOutput.writeTextFile(CDCR.outputFileName, "do post processing");
    		CorefSystem cs = new CorefSystem();
        	cs.corefSystem.postProcessing(document);
        	
        	CorefScorer postmucscore = new ScorerMUC();
        	postmucscore.calculateScore(document);
        	postmucscore.printF1(logger, true);
        	
        	CorefScorer postpairscore = new ScorerPairwise();
        	postpairscore.calculateScore(document);
        	postpairscore.printF1(logger, true);
        	
        	// Average of MUC, B^{3} and CEAF-\phi_{4}.
        	double conllF1 = (score.getF1() + ceafscore.getF1() + postmucscore.getF1()) / 3;
        	ResultOutput.writeTextFile(CDCR.outputFileName, "conllF1:     " + conllF1);
    	}
	}
	
}
