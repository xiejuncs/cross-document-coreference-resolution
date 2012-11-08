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
import edu.oregonstate.score.ScorerHelper;
import edu.oregonstate.search.IterativeResolution;
import edu.oregonstate.search.JointCoreferenceResolution;
import edu.oregonstate.training.Train;
import edu.oregonstate.training.TrainHeuristicFunction;
import edu.oregonstate.training.TrainHybridCase;
import edu.oregonstate.training.TrainSingleDocumentHeuristicFunction;
import edu.oregonstate.util.DocumentMerge;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.SystemOptions;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
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

	private static final Logger logger = Logger.getLogger(CDCR.class.getName());
	
	/** Path for the corpus */
	public static String corpusPath;
	
	/** File name for output: CROSS-RESULT */
	public static String outputFileName;
	
	/** Path for the annotation file */
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
	
	/** Align the predicted mentions and gold mentions by putting spurious predicted mentions as singleton clusters in gold clusters */
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
	public static boolean incorporateTopicSRLResult;
	
	/** whether to incorporate the document srl result */
	public static boolean incorporateDocumentSRLResult;
	
	/** EecbTopic documentSentence Null case */
	public static boolean enableNull;
	
	/** which experiment */
	private int experiment;
	
	/** output text to SRL software */
	public static boolean outputText;
	
	public static List<ScoreType> scoreTypes;
	
	public CDCR() {
		SystemOptions option = new SystemOptions();
		corpus = new Document();
		
		//
		// set the path for different knowledge sources
		//
		wordnetConfigurationPath = EecbConstants.WORD_NET_CONFIGURATION_PATH;
		resultPath = EecbConstants.RESULT_PATH;
		srlPath = EecbConstants.TOKENS_OUTPUT_PATH;
		wordSimilarityPath = EecbConstants.WORD_SIMILARITY_PATH;
		enableNull = option.ENABLE_NULL;
		outputText = option.OUTPUTTEXT;
		experiment = option.EXPERIMENTN;
		scoreTypes = option.scoreTypes;
		
		//
		// set outputFileName
		//
		outputFileName = EecbConstants.TEMPORY_RESULT_PATH + Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		ResultOutput.writeTextFile(outputFileName, Calendar.getInstance().getTime().toString().replaceAll("\\s", "-"));
	    
		//
		// set corpusPath and annotationPath
		//
		if (option.DEBUG) {
			corpusPath = EecbConstants.DEBUG_CORPUS_PATH;
			annotationPath = EecbConstants.DEBUG_MENTION_ANNOTATION_PATH;
		} else {
			corpusPath = EecbConstants.WHOLE_CORPUS_PATH;
			annotationPath = EecbConstants.WHOLE_MENTION_ANNOTATION_PATH;
		}
		
		//
		// set sieve
		//
		if (option.FULL_SIEVE) {
			sieve = EecbConstants.FULL_SIEVE_STRING;
		} else {
			sieve = EecbConstants.PARTIAL_SIEVE_STRING;
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
		//whether to incorporate the Topic SRL result
		//
		incorporateTopicSRLResult = option.TOPIC_SRL_RESULT;
		
		//
		// whether to incorporate the document SRL result
		//JWNL.initialize(new FileInputStream(wordnetConfigurationPath));
		incorporateDocumentSRLResult = option.DOCUMENT_SRL_RESULT;
		
	}
	
	/** configure the WORDNET */
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
		//ResultOutput.deleteResult(resultPath);
		CDCR cdcr = new CDCR();
		ResultOutput.writeTextFile(CDCR.outputFileName, "Start..........................");
		ResultOutput.writeTextFile(CDCR.outputFileName, "\n\n");
		ResultOutput.printTime();
		
		String[] parameters = {"100-2"};
		String[] topics = ResultOutput.getTopics(corpusPath);
		switch (cdcr.experiment) {
			case 1: 
				executeStanfordExperiment(cdcr, topics);
				break;
			case 2: 
				executeCrossCoreferenceResolution(topics, parameters);
				break;
			case 3:
				executeWithinCoreferenceResolution(topics, parameters);
				break;
			case 4:
				executeWithinCrossCoreferenceResolution(topics);
				break;
			default:
				executeStanfordExperiment(cdcr, topics);
				break;
		}

		ResultOutput.printTime();
		System.out.println("Done.........");
	}
	
	/**
	 * do within coreference first, do not use search to guide the within coreference resolution, 
	 * combine the within coreference resolution result produced by the Stanford System together,
	 * and then do cross corefernce resolution on the combined document, produce the final result
	 * 
	 * @param topics
	 */
	private static void executeWithinCrossCoreferenceResolution(String[] topics) {
		ResultOutput.writeTextFile(CDCR.outputFileName, "do within coreference first, do not use search to guide the within coreference resolution, " +
				"combine the within coreference resolution result produced by the Stanford System together, " +
				"and then do cross corefernce resolution on the combined document, produce the final result ");
		ResultOutput.writeTextFile(CDCR.outputFileName, "training Phase................");
		ResultOutput.deleteResult(EecbConstants.TEMPORY_RESULT_PATH);
		String[] parameters = {"300-1-10"};
		for (String parameter : parameters) {
			int[] paras = getParameters(parameter);
			for(ScoreType type : scoreTypes) {
				TrainHybridCase thc = new TrainHybridCase(paras[0], paras[2], topics, topics, paras[1], type);
				thc.train();

				Matrix weight = thc.getModel();
				System.out.println(ResultOutput.printStructredModel(weight, Feature.featuresName));
				ResultOutput.writeTextFile(outputFileName, ResultOutput.printStructredModel(weight, Feature.featuresName));
				//thc.test();
			}
		}
	}
	
	/** transform the string to int array */
	private static int[] getParameters(String parameter) {
		ResultOutput.writeTextFile(outputFileName, "Configuration parameters :" + parameter);
		String[] paras = parameter.split("-");
		int maximumSearch = Integer.parseInt(paras[0]);
		int width = Integer.parseInt(paras[1]);
		int noOfIteration = Integer.parseInt(paras[2]);
		int[] parameters = new int[] {maximumSearch, width, noOfIteration};
		return parameters;
	}

	/** combine the documents together, and then do cross corefernce resolution as a whole */
	private static void executeCrossCoreferenceResolution(String[] topics, String[] parameters) {
		ResultOutput.writeTextFile(CDCR.outputFileName, "cross coreference directly");	
		for (String parameter : parameters) {
			ResultOutput.writeTextFile(outputFileName, "Configuration parameters :" + parameter);
	    	String[] paras = parameter.split("-");
	    	String noOfIteration = paras[0];
	    	String width = paras[1];
	    	ResultOutput.writeTextFile(CDCR.outputFileName, "training Phase................");
			TrainHeuristicFunction thf = new TrainHeuristicFunction(Integer.parseInt(noOfIteration), topics, Integer.parseInt(width));
			Matrix model = thf.train();
		}
	}

	/** execute within search first and then combine them together to perform the cross coreference resolution as a whole */
	private static void executeWithinCoreferenceResolution(String[] topics, String[] parameters) {
		ResultOutput.writeTextFile(CDCR.outputFileName, "do within coreference first, and then cross coreference.....");
		/** train the model, the model is too big, so I do not print them out */
		ResultOutput.writeTextFile(CDCR.outputFileName, "training Phase................");
		for (String parameter : parameters) {
			ResultOutput.writeTextFile(outputFileName, "Configuration parameters :" + parameter);
	    	String[] paras = parameter.split("-");
	    	String noOfIteration = paras[0];
	    	String width = paras[1];
	    	TrainSingleDocumentHeuristicFunction tsdhf = new TrainSingleDocumentHeuristicFunction(Integer.parseInt(noOfIteration), topics, Integer.parseInt(width));
	    	Matrix model = tsdhf.train();
		}
	}

	/**
	 * Execute Stanford's Experiment
	 * 
	 * @param cdcr
	 * @param topics
	 */
	private static void executeStanfordExperiment(CDCR cdcr, String[] topics) {
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
			    DocumentMerge dm = new DocumentMerge(topicDocument, cdcr.corpus);
			    dm.addDocument();
			    
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "end to process topic" + topic + "................");
		}
		
		// print the score
		ScorerHelper sh = new ScorerHelper(cdcr.corpus, logger, outputFileName, postProcess);
		sh.printScore();
		
		ResultOutput.writeTextFile(CDCR.outputFileName, "Finish the processing, next state is to evaluation");
	}
	
}
