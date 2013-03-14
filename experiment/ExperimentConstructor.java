package edu.oregonstate.experiment;

import java.util.Map;

import java.util.Properties;

import edu.oregonstate.io.ResultOutput;
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
	public static String logFile;
	
	// experiment result folder
	public static String resultPath;
	
	// property file
	public static Properties experimentProps;

	// corpus path
	public static String corpusPath;
	
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
		corpusPath = props.getProperty(EecbConstants.CORPUS_PROP);
		 
		StringBuilder sb = new StringBuilder();
		//String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-").replaceAll(":", "-");
        sb.append(corpusPath + "/TEMPORYRESUT/");
        
        ExperimentConfigurationFactory factory = new ExperimentConfigurationFactory(props);
        String name = factory.defineExperimentName();
        sb.append(name);
        
        // create the result folder
        resultPath = sb.toString().trim();
        Command.mkdir(resultPath);
        
        // create folder to store the CONLL results
        Command.mkdir(resultPath + "/conll");
        
        // specify the log file path
        logFile = sb.toString().trim() + "/experimentlog";
        ResultOutput.printProperties(experimentProps, logFile);    // Print experiment configuration
		
		// configure the WORDNET
        factory.configureWordNet();
       
        // Dekang Lin's Similarity thesaurus respecitvely for noun, adjective and verb
        nounSimilarityThesaurus = factory.loadSimilarityDictionary(corpusPath + "/simN.lsp");
        verbSimilarityThesaurus = factory.loadSimilarityDictionary(corpusPath + "/simV.lsp");
        adjectiveSimilarityThesaurus = factory.loadSimilarityDictionary(corpusPath + "/simA.lsp");
        
        // whether need to do post-process on predicted mentions
        boolean goldMentions = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATAGENERATION_GOLDMENTION_PROP));
        postProcess = !goldMentions;
	}
	
	// perform the experiments
	public abstract void performExperiment();
	
}
