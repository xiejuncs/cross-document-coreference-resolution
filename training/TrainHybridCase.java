package edu.oregonstate.training;

import java.util.ArrayList;
import java.util.List;

import edu.oregonstate.CDCR;
import edu.oregonstate.featureExtractor.SrlResultIncorporation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.DocumentMerge;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;

/**
 * use the within result produced by Stanford Multi-Sieve System, and then incorporate them together 
 * do coreference resolution to train a heuristic weight
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainHybridCase extends StructuredPredictionSearch {
	
	/** put all within coreference resolution together to form a cross coreference resolution document */
	public Document corpus;

	// set the iteration number and beam width
	public TrainHybridCase(int maximumSearch, int iteration, String[] trainTopics, String[] testTopics, int beamWidth, ScoreType type){
		super(maximumSearch, iteration, trainTopics, testTopics, beamWidth, type);
		initializeCorpus();
	}
	
	/**
	 * initialize the corpus document
	 */
	private void initializeCorpus() {
		corpus = new Document();
		corpus.predictedOrderedMentionsBySentence = new ArrayList<List<Mention>>();
	}
	
	/**
	 * At first, use stanford system to do within coreference resolution, and then integrate them together
	 * Secondly, use search to guide the merge, during training phase, we need to learn a cost function, this will 
	 * be defined in the bestsearch file
	 */
	public void train() {
		ResultOutput.writeTextFile(CDCR.outputFileName, "begin the training");
		for (String topic : mTrainTopics) {
			ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic " + topic + "................");
			List<String> files  = getSortedFileNames(topic);
			for (String file : files) {
				ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process document " + file + "...............");
				try {
					Document document = getDocumentFromSingleDocument(type, topic, file);
					// combine the documents together
					dm = new DocumentMerge(document, corpus);
					dm.mergeDocument();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			srlIncorporation(topic);
			corpus.fill();
			
			// deal with the cross coreference resolution case here
			ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic +  " : " + corpus.allGoldMentions.size());
			ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + " : " + corpus.goldCorefClusters.size());
			ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + ResultOutput.printCluster(corpus.goldCorefClusters));
			ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Clusters After Stanford's System Preprocess of " + topic + " : " + corpus.corefClusters.size());
			ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + ResultOutput.printCluster(corpus.corefClusters));
			ResultOutput.writeTextFile(CDCR.outputFileName, "Stanford System merges " + (corpus.allGoldMentions.size() - corpus.corefClusters.size()) + " mentions");
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "Start to do cross coreference resolution based on the corpus");
			trainModel(corpus, topic);
			
			printDebugInformation(corpus, topic);
		}
		
	}

	/** incorporate the srl result into the Corpus mentions */
	private void srlIncorporation(String topic) {
		SrlResultIncorporation srlResult = new SrlResultIncorporation(CDCR.srlPath+topic + ".output");
		srlResult.alignSRL(corpus.predictedOrderedMentionsBySentence);
	}
	
	/** apply the learned weight to direct the learning */
	public void test() {
		ResultOutput.writeTextFile(CDCR.outputFileName, "begin the test");
		initializeCorpus();
		for (String topic : mTestTopics) {
			ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic " + topic + "................");
			List<String> files  = getSortedFileNames(topic);
			for (String file : files) {
				ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process document " + file + "...............");
				try {
					Document document = getDocumentFromSingleDocument(type, topic, file);
					System.out.println("Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
					System.out.println("Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
					// combine the documents together
					dm = new DocumentMerge(document, corpus);
					dm.mergeDocument();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			srlIncorporation(topic);
			corpus.fill();
			printDebugInformation(corpus, topic);
			// deal with the cross coreference resolution case here
			ResultOutput.writeTextFile(CDCR.outputFileName, "Start to do cross coreference resolution testing based on the corpus");
			buildScoreOutputPath("test", type, topic, mIteration);
			testModel(corpus);
			printDebugInformation(corpus, topic);
		}
	}
	
}
