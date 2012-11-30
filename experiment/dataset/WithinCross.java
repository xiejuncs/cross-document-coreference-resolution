package edu.oregonstate.experiment.dataset;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.IDataSet;
import edu.oregonstate.featureExtractor.SrlResultIncorporation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.DocumentMerge;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.ScorerPairwise;

/**
 * use the within result produced by Stanford Multi-Sieve System, and then incorporate them together
 * do coreference resolution to train a heuristic weight
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class WithinCross implements IDataSet {
		
	/** corpus path */
	private String corpusPath;
	
	private static final Logger logger = Logger.getLogger(WithinCross.class.getName());
	
	/** srl path */
	private String srlPath;
	
	public WithinCross() {
	}
	
	@Override
	public Document getData(String[] topics) {
		corpusPath = (String) ExperimentConstructor.mParameters.get(EecbConstants.DATASET).get("corpusPath");
		srlPath = (String) ExperimentConstructor.mParameters.get(EecbConstants.DATASET).get("srlpath");
		
		Document corpus = new Document();
		IDocument documentExtraction = new SingleDocument();
		DocumentMerge dm;
	
		for (String topic : topics) {
			String statisPath = ExperimentConstructor.currentExperimentFolder + "/" + "statistics";
			
			List<String> files  = getSortedFileNames(topic);
			for (String file : files) {
				try {
					String path = corpusPath + topic + "/" + file;
					ResultOutput.writeTextFile(ExperimentConstructor.logFile, file + " : " + path);
					Document document = documentExtraction.getDocument(path);
					// combine the documents together
					dm = new DocumentMerge(document, corpus);
					ResultOutput.writeTextFile(statisPath, file + " " + document.allGoldMentions.size() + " " + document.allPredictedMentions.size() + " " +
					document.corefClusters.size() + " " + document.goldCorefClusters.size());
					dm.mergeDocument();
					ResultOutput.writeTextFile(ExperimentConstructor.logFile, "\n");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			srlIncorporation(topic, corpus);              // incorporate the srl result
			corpus.fill();                                // incorporate SYNONYM
			
			// generate feature for each mention
		    for (Integer id : corpus.corefClusters.keySet()) {
		    	CorefCluster cluster = corpus.corefClusters.get(id);
		    	cluster.regenerateFeature();
		    }
			
			CorefScorer pairscore = new ScorerPairwise();
    		pairscore.calculateScore(corpus);
    		pairscore.printF1(logger, true);
		}

		return corpus;
	}
	
	/** incorporate the srl result into the Corpus mentions */
	private void srlIncorporation(String topic, Document corpus) {
		SrlResultIncorporation srlResult = new SrlResultIncorporation(srlPath + topic + ".output");
		srlResult.alignSRL(corpus.predictedOrderedMentionsBySentence);
	}

	/** get files orded by the file name */
	protected List<String> getSortedFileNames(String topic) {
		List<String> files  = new ArrayList<String>();
		String topicPath = corpusPath + topic + "/";
		files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
		sort(files);
		return files;
	}
	
	/** sort the names of the files */
	private void sort(List<String> files) {
		Integer[] numbers = new Integer[files.size()];
		for (int i = 0; i < files.size(); i++) {
			numbers[i] = Integer.parseInt(files.get(i).substring(0, files.get(i).length() - 5));
		}
		Arrays.sort(numbers);
		for (int i = 0; i < numbers.length; i++) {
			files.set(i, Integer.toString(numbers[i]) + ".eecb");
		}
	}
	
}
