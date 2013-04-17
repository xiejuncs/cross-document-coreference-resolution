package edu.oregonstate.experiment;

import java.util.Map;

import java.util.Properties;

import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.stats.ClassicCounter;

/**
 * the abstract class of experiment
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public abstract class ExperimentConstructor {

	// used for recording the information of the whole experiment
	public static String experimentLogFile;
	
	// experiment result folder
	public static String experimentFolder;
	
	// property file
	public static Properties experimentProps;

	// corpus path
	public static String experimentCorpusPath;
	
	// debug Mode
	public static boolean debugMode;
	
	// Dekang Lin's Noun Similarity thesaurus
	public static Map<String, ClassicCounter<String>> nounSimilarityThesaurus;

	// Dekang Lin's Verb Similarity thesaurus, in order to get its top 10, use the Lemma word form
	public static Map<String, ClassicCounter<String>> verbSimilarityThesaurus;

	// Dekang Lin's Adjective Similarity thesaurus
	public static Map<String, ClassicCounter<String>> adjectiveSimilarityThesaurus;
	
	// post-process the corpus for predicted mentions
	public static boolean postProcess;

	/**
	 * configure the experiment
	 * 
	 * @param props
	 */
	public ExperimentConstructor(Properties props) {
		experimentProps = props;
		
		// debug mode
		debugMode = Boolean.parseBoolean(props.getProperty(EecbConstants.DEBUG_PROP, "false"));
		
		// corpus folder, which stores the EECB corpus and TEMPORARY folder which is used for print the log file
		experimentCorpusPath = props.getProperty(EecbConstants.CORPUS_PROP);
		 
		StringBuilder sb = new StringBuilder();
		//String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-").replaceAll(":", "-");
        sb.append(experimentCorpusPath + "/TEMPORYRESUT/");
        
        ExperimentConfigurationFactory factory = new ExperimentConfigurationFactory(props);
        String name = factory.defineExperimentName();
        sb.append(name);
        
        // create the result folder
        experimentFolder = sb.toString().trim();
        Command.mkdir(experimentFolder);
        
        // create folder to store the CONLL results
        Command.mkdir(experimentFolder + "/conll");
        
        // create folder to store the serialized results
		Command.mkdir(experimentFolder + "/document");
		
		// create folder to store the model result
		Command.mkdir(experimentFolder + "/model");
		
		// create folder to store the violation result
		Command.mkdir(experimentFolder + "/violation");
		
		// create folder to store weight difference 
		Command.mkdir(experimentFolder + "/weightdifference");
		
		// create folder to store weight norm
		Command.mkdir(experimentFolder + "/weightnorm");
		
		// create folder to store the constraints, the name of the file is just the topic name
		Command.mkdir(experimentFolder + "/constraints");
			
        // specify the log file path
        experimentLogFile = sb.toString().trim() + "/experimentlog";
		
		// configure the WORDNET
        factory.configureWordNet();
       
        // Dekang Lin's Similarity thesaurus respecitvely for noun, adjective and verb
        nounSimilarityThesaurus = factory.loadSimilarityDictionary(experimentCorpusPath + "/simN.lsp");
        verbSimilarityThesaurus = factory.loadSimilarityDictionary(experimentCorpusPath + "/simV.lsp");
        adjectiveSimilarityThesaurus = factory.loadSimilarityDictionary(experimentCorpusPath + "/simA.lsp");
        
        // whether need to do post-process on predicted mentions
        // because gold mention also includes the singleton cluster,
        // so no matter whether gold mention or predicted mention, 
        // do post-process
        // boolean goldMentions = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP));
        postProcess = true;
	}
	
	// perform the experiments
	public abstract void performExperiment();
	
}
