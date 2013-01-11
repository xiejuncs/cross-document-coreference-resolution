package edu.oregonstate.experiment;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.didion.jwnl.JWNL;

import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;

/**
 * the abstract class of experiment
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public abstract class ExperimentConstructor {

	/* used for recording the information of the experiment */
	public static String logFile;
	
	/* experiment result folder */
	public static String experimentResultFolder;
	
	/* props file */
	public static Properties experimentProps;

	/* for mention words feature */
	public static Map<String, List<String>> datas;

	/* corpus path */
	public static String corpusPath;
	
	/* training, testing, development */
	public static String[] trainingTopics;
	public static String[] testingTopics;
	public static String[] developmentTopics;

	/**
	 * 
	 * @param props
	 */
	public ExperimentConstructor(Properties props) {
		experimentProps = props;
		String[] experimentTopics;
		int index = 0;
		boolean debug = Boolean.parseBoolean(props.getProperty(EecbConstants.DEBUG_PROP, "false"));
		if (debug) {
			corpusPath = EecbConstants.LOCAL_CORPUS_PATH;
			experimentTopics = EecbConstants.debugTopics;
			developmentTopics = EecbConstants.debugDevelopmentTopics;
			index = 2;
		} else {
			corpusPath = EecbConstants.CLUSTER_CPRPUS_PATH;
			experimentTopics = EecbConstants.stanfordTotalTopics;
			developmentTopics = EecbConstants.stanfordDevelopmentTopics;
			index = 12;
		}
		
		// create a directory to store the output of the specific experiment
		StringBuilder sb = new StringBuilder();
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-").replaceAll(":", "-");
        sb.append(corpusPath + "corpus/TEMPORYRESUT/" + timeStamp);
        
        //
        // gold mentions or predicted mentions of training and testing dataset
        //
        boolean trainGoldOnly = Boolean.parseBoolean(props.getProperty(EecbConstants.TRAIN_GOLD_PROP, "true"));
        if (trainGoldOnly) {
                sb.append("-traingold");
        } else {
                sb.append("-trainpredicted");
        }

        boolean testGoldOnly = Boolean.parseBoolean(props.getProperty(EecbConstants.TEST_GOLD_PROP, "true"));
        if (testGoldOnly) {
                sb.append("-testgold");
        } else {
                sb.append("-testpredicted");
        }
        
        //
        // dataset generation mode
        //
        boolean dataSetMode = Boolean.parseBoolean(props.getProperty(EecbConstants.DATASET_PROP, "true"));
        if (dataSetMode) {
                sb.append("-flat");
        } else {
                sb.append("-hierarchy");
        }
        
        //
        // Stanford experiment or Oregon State statement
        //
        if (props.containsKey(EecbConstants.SEARCH_PROP)) {
                String searchModel = props.getProperty(EecbConstants.SEARCH_PROP);
                String searchWidth = props.getProperty(EecbConstants.SEARCH_BEAMWIDTH_PROP);
                String searchStep = props.getProperty(EecbConstants.SEARCH_MAXIMUMSTEP_PROP);
                sb.append("-oregonstate-" + searchModel + "-" + searchWidth + "-" + searchStep);
        } else {
                sb.append("-stanford");
        }
		
        //
        // classifier method
        //
        String classifierLearningModel = props.getProperty(EecbConstants.CLASSIFIER_PROP);   // classification model
        String classifierNoOfIteration = props.getProperty(EecbConstants.CLASSIFIER_EPOCH_PROP);   // epoch of classification model
        sb.append("-" + classifierLearningModel + "-" + classifierNoOfIteration);
        
        //
        // stopping criterion
        //
        String stopping = props.getProperty(EecbConstants.STOPPING_PROP, "none");
        sb.append("-" + stopping);
        
        //
        // whether process or not process
        //
        boolean trainPostProcess = Boolean.parseBoolean(props.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
        if (trainPostProcess) {
                sb.append("-trainPostProcess");
        } else {
                sb.append("-trainNotPostProcess");
        }
        boolean testPostProcess = Boolean.parseBoolean(props.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
        if (testPostProcess) {
                sb.append("-testPostProcess");
        } else {
                sb.append("-testNotPostProcess");
        }
        
        //
        // score type
        //
        String lossScoreType = props.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP);
        sb.append("-" + lossScoreType);
        
        //
        // create experiment folder for storing the results
        //
        experimentResultFolder = sb.toString();
        Command.createDirectory(experimentResultFolder);
        logFile = experimentResultFolder + "/experimentlog";
        
        //
        // create conll result for storing the output of coreference resolution result
        //
        String conllResultPath = experimentResultFolder + "/conllResult";
        Command.createDirectory(conllResultPath);
        
        // configure training, testing sets
        splitTopics(index, experimentTopics);
       
        //
        // configure WORDNET and mention similarity dictionary
        //
        configureJWordNet();  
        configureWordSimilarity();
	}
	
	protected abstract void performExperiment();
	
	/** 
	 * configure word similarity matrix 
	 */
	private void configureWordSimilarity() {
		String WORD_SIMILARITY_PATH = corpusPath + "corpus/sims.lsp";
		WordSimilarity wordSimilarity = new WordSimilarity(WORD_SIMILARITY_PATH);
		wordSimilarity.initialize();
		datas = wordSimilarity.getDatas();
	}
	
	/**
	 * configure the WORDNET
	 */
	private void configureJWordNet() {
		String WORD_NET_CONFIGURATION_PATH = corpusPath + "corpus/file_properties.xml";
		try {
			JWNL.initialize(new FileInputStream(WORD_NET_CONFIGURATION_PATH));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * split the experiment topics into training and testing according to 
	 * split point
	 * 
	 * @param index
	 * @param experimentTopics
	 * @return
	 */
	private void splitTopics(int index, String[] experimentTopics) {
		trainingTopics = new String[index];
		testingTopics = new String[experimentTopics.length - index];
		for (int i = 0; i < experimentTopics.length; i++) {
			if (i < index) {
				trainingTopics[i] = experimentTopics[i];
			} else {
				testingTopics[i - index] = experimentTopics[i];
			}
		}
	}
}
