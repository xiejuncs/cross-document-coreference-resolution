package edu.oregonstate.experiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.oregonstate.featureExtractor.WordSimilarity;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.training.Development;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.util.Triple;

/**
 * find out what is the configuration set for the experiment
 * 
 * @author jun (xiejuncs@gmail.com)
 *
 */
public class ExperimentConfigurationFactory {
	
	// set the property for the experiment
	private final Properties props;
	
	// corpus folder
	private final String corpusPath;
	
	public ExperimentConfigurationFactory(Properties properties) {
		props = properties;
		corpusPath = ExperimentConstructor.corpusPath;
	}
	
	// define the experiment name as the result folder name
	public String defineExperimentName() {
		StringBuilder sb = new StringBuilder();
		
		// get the EXPERIMENT_PROP value and get the elements
		// for each element, get its value and concatenate them together.
		// such that Pairwise-StructuredPerceptron
		String experimentProp = props.getProperty(EecbConstants.EXPERIMENT_PROP);
		String[] experimentElements = experimentProp.split(",");
		for (String element : experimentElements) {
			String key = "dcoref." + element.trim();
			String value = props.getProperty(key.trim());
			sb.append("-" + value);
		}
		
		return sb.toString().trim();
	}
	
	// configure the WordNet at the beginning of the experiment
	public void configureWordNet() {
		String wordnetPath = props.getProperty(EecbConstants.WORDNET_PROP);
		System.setProperty("wordnet.database.dir", wordnetPath);
	}
	
	// load data from Word Similarity Dictionary
	public Map<String, ClassicCounter<String>> loadSimilarityDictionary(String similarityPath) {
		WordSimilarity similarity = new WordSimilarity(similarityPath);
		similarity.load();
		return similarity.getDatas();
	}
	
	/**
	 * get mention boundary from gold mention file
	 * @return
	 */
	public Map<String, Map<String, List<Triple>>> loadGoldMentionBoundary() {
		String mentionPath = corpusPath + "/mentions.txt";
		List<String> records = IOUtils.linesFromFile(mentionPath);
		Map<String, Map<String, List<Triple>>> goldMentionBoundary = new HashMap<String, Map<String, List<Triple>>>();
		// the format of the gold mention file
		// # N or V? (0)  Topic(1)  Doc(2) Sentence Number(3) CorefID(4) StartIdx(5)  EndIdx(6) StartCharIdx(7)  EndCharIdx(8)
		// # CharIdx doesn't include spaces
		// # sentence number starts from 0
		for (String record : records) {
			String[] elements = record.split("\t");
			
			// index topic
			String topic = elements[1];
			boolean containTopic = goldMentionBoundary.containsKey(topic);
			if (!containTopic) {
				goldMentionBoundary.put(topic, new HashMap<String, List<Triple>>());
			}
			
			// index the combination of document and sentence
			String document = elements[2];
			String sentenceNumber = elements[3];
			String DocSen = document + "-" + sentenceNumber;
			boolean containDocSen = goldMentionBoundary.get(topic).containsKey(DocSen);
			if (!containDocSen) {
				goldMentionBoundary.get(topic).put(DocSen, new ArrayList<Triple>());
			}
			
			// add record as triple : corefID, startCharIdx, endCharIdx
			String corefID = elements[0] + "-" + elements[4];
			int startCharIdx = Integer.parseInt(elements[7]);
			int endCharIdx = Integer.parseInt(elements[8]);
			Triple<String, Integer, Integer> triple = new Triple<String, Integer, Integer>(corefID, startCharIdx, endCharIdx);
			goldMentionBoundary.get(topic).get(DocSen).add(triple);
		}
		
		return goldMentionBoundary;
	}
	
	// tune the stopping rate
	public static double tuneStoppingRate(double[] weight, int j) {
		double stoppingrate = 0.0;
		
		String stopping = ExperimentConstructor.experimentProps.getProperty(EecbConstants.STOPPING_CRITERION);
		if (stopping.equals("tuning")) {
			Development development = new Development(j, weight, 1.0, 3.0, 10);
			stoppingrate = development.tuning();
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "\nthe stopping rate is : " + stoppingrate + " for " + j + "\n");
		}

		return stoppingrate;
	}
}
