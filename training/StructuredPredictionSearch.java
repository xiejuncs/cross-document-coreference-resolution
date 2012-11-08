package edu.oregonstate.training;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import edu.oregonstate.CDCR;
import edu.oregonstate.CorefSystemSingleDocument;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.BeamSearch;
import edu.oregonstate.util.DocumentMerge;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import Jama.Matrix;

/**
 * This is high level class for search functions used across the whole platform
 * We can define common functions here, and then use the abstract method to define the 
 * different components, such as train or test, because the beam search part is the same
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public abstract class StructuredPredictionSearch {

	// No of maximum search within one iteration
	protected int maximumSearchWithinOneIteration;
	
	// train topics in the corpus
	protected String[] mTrainTopics;
	
	// test topics in the corpus
	protected String[] mTestTopics;
	
	// initial weight model, set all elements to zero
	protected Matrix mInitialModel;
	
	// average weight model
	protected Matrix averageModel;
	
	// whole search step
	protected int wholeSearchStep;
	
	// beam width
	protected int mBeamWidth;
	
	// output file path
	protected String scoreOutputPath;
	
	protected DocumentMerge dm;
	
	protected ScoreType type;
	
	// No of iteration
	protected int mIteration;
	
	public Matrix getModel() {
		return mInitialModel;
	}
	
	/** abstract class constructor, the sub-class just needs to use super to construct its own constructor */
	public StructuredPredictionSearch( int maximumSearch, int iteration, String[] trainTopics, String[] testTopics, int beamWidth, ScoreType scoreType) {
		maximumSearchWithinOneIteration = maximumSearch;
		mIteration = iteration;
		mTrainTopics = trainTopics;
		mTestTopics = testTopics;
		mBeamWidth = beamWidth;
		type = scoreType;
		ResultOutput.writeTextFile(CDCR.outputFileName, "use " + type.toString() + " Loss function to guide the search in order to learn the weight");
		
		// initialize weight as all 0 value
     	int dimension = Feature.featuresName.length;
     	mInitialModel = new Matrix(dimension, 1);
     	averageModel = new Matrix(dimension, 1);
     	wholeSearchStep = 0;
	}
	
	/** use search guided by the loss function to learn the weight */
	abstract public void train();
	
	/** use the learned weight to guide the search to reach a conference output and then print the score */
	abstract public void test();
	
	/** get document object from one file */
	protected Document getDocumentFromSingleDocument(ScoreType type, String topic, String file) {
		String topicPath = CDCR.corpusPath + topic + "/";
		CorefSystemSingleDocument cs = new CorefSystemSingleDocument();
		String singleDocument = topicPath + file;
		Document document = new Document();
		try {
			document = cs.getDocument(singleDocument);
			ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
			ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
			ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + ResultOutput.printCluster(document.goldCorefClusters));
			
			cs.corefSystem.coref(document);
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Clusters After Stanford's System Preprocess of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.corefClusters.size());
			ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + ResultOutput.printCluster(document.corefClusters));
			ResultOutput.writeTextFile(CDCR.outputFileName, "Stanford System merges " + (document.allGoldMentions.size() - document.corefClusters.size()) + " mentions");
			buildScoreOutputPath(type, topic, file);
			System.out.println("Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
			System.out.println("Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return document;
	}
	
	/** build the scoreOutputString using topic and file*/
	protected void buildScoreOutputPath(ScoreType type, String topic, String file) {
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		String prefix = maximumSearchWithinOneIteration + "-" + mBeamWidth + "-" + type.toString() + "-" + topic + "-" + file.substring(0, file.length() - 5) + "-";
		prefix = prefix.concat(timeStamp);
		scoreOutputPath = EecbConstants.TEMPORY_RESULT_PATH + prefix;
	}
	
	/** build the scoreOutputString using topic*/
	protected void buildScoreOutputPath(String flag, ScoreType type, String topic, int iteration) {
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		String prefix = flag + "-" + maximumSearchWithinOneIteration + "-" + mBeamWidth + "-" + iteration + "-" + type.toString() + "-" + topic + "-";
		prefix = prefix.concat(timeStamp);
		scoreOutputPath = EecbConstants.TEMPORY_RESULT_PATH + prefix;
	}
	
	/** get files orded by the file name */
	protected List<String> getSortedFileNames(String topic) {
		List<String> files  = new ArrayList<String>();
		String topicPath = CDCR.corpusPath + topic + "/";
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
	
	/** 
	 * train the model and output a weight 
	 * Integrate the train and test together, and see the improvement
	 */
	protected void trainModel(Document corpus, String topic) {
		for (int i = 0; i < mIteration; i++) {
			ResultOutput.writeTextFile(CDCR.outputFileName, "training phase : The " + i + "th iteration");
			Document documentState = new Document();
			DocumentMerge dm = new DocumentMerge(corpus, documentState);
			
			dm.mergeDocument();
			buildScoreOutputPath( "train", type, topic, i);
			BeamSearch beamSearch = new BeamSearch(mBeamWidth, scoreOutputPath, type, documentState, maximumSearchWithinOneIteration);
			//normalization(mInitialModel);
			ResultOutput.writeTextFile(CDCR.outputFileName, ResultOutput.printStructredModel(mInitialModel, Feature.featuresName));
			beamSearch.setWeight(mInitialModel);
			beamSearch.setAverageWeight(averageModel);
			beamSearch.bestFirstBeamSearch();
			
			mInitialModel = beamSearch.getWeight();
			wholeSearchStep += beamSearch.getSearchStep();
			//averageModel = beamSearch.getAverageWeight();
			
			//Matrix temporayAverageModel = setAverageMatrix(averageModel);
			
			//normalization(mInitialModel);
			ResultOutput.writeTextFile(CDCR.outputFileName, ResultOutput.printStructredModel(mInitialModel, Feature.featuresName));
			
			Document documentStateForTest = new Document();
			DocumentMerge dmtest = new DocumentMerge(corpus, documentStateForTest);
			dmtest.mergeDocument();
			
			buildScoreOutputPath( "test", type, topic, i);
			BeamSearch beamSearchTest = new BeamSearch(mBeamWidth, scoreOutputPath, type, documentStateForTest, maximumSearchWithinOneIteration);
			beamSearchTest.setWeight(mInitialModel);
			//beamSearchTest.setWeight(temporayAverageModel);
			beamSearchTest.bestFirstBeamSearchForTest();
		}
	}
	
	private Matrix setAverageMatrix (Matrix averageWeight) {
		Matrix matrix = new Matrix(averageWeight.getRowDimension(), 1);
		for (int i = 0; i < averageWeight.getRowDimension(); i++) {
			matrix.set(i, 0, averageWeight.get(i, 0) / wholeSearchStep);
		}
		return matrix;
	}
	
	
	private void normalization(Matrix weight) {
		double sum = weight.norm2();
		
		if (sum == 0.0) return;
		
		for (int i = 0; i < weight.getRowDimension(); i++){
			double value = weight.get(i, 0);
			weight.set(i, 0, value / sum);
		}
	}
	
	
	/** use learned weight to do testings */
	protected void testModel(Document corpus) {
		BeamSearch beamSearch = new BeamSearch(mBeamWidth, scoreOutputPath, type, corpus, maximumSearchWithinOneIteration);
		beamSearch.setWeight(mInitialModel);
		beamSearch.bestFirstBeamSearchForTest();
	}
	
	/** print the debug information */
	protected void printDebugInformation(Document corpus, String topic) {
		ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + " : " + corpus.allGoldMentions.size());
		ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + " : " + corpus.goldCorefClusters.size());
		ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + " : " + corpus.corefClusters.size() + " formed after cross");
		ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + ResultOutput.printCluster(corpus.goldCorefClusters));
		ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + ResultOutput.printCluster(corpus.corefClusters));
	}
	
}
