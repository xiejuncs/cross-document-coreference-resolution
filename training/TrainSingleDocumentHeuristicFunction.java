package edu.oregonstate.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import edu.oregonstate.cluster.HAC;
import edu.oregonstate.cluster.agglomeration.AgglomerationMethod;
import edu.oregonstate.cluster.agglomeration.AverageLinkage;
import edu.oregonstate.cluster.experiment.DissimilarityMeasure;
import edu.oregonstate.cluster.experiment.EecbDissimilarityMeasure;
import edu.oregonstate.data.EecbClusterDocument;

import edu.oregonstate.CDCR;
import edu.oregonstate.experiment.dataset.CorefSystemSingleDocument;
import edu.oregonstate.general.CounterMap;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.BeamSearch;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;

import Jama.Matrix;

/**
 * train a single document heuristic function
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainSingleDocumentHeuristicFunction {

	// No of Iteration
	private int mExpasion;
	// topic in the corpus
	private String[] mTopic;
	private Matrix mInitialModel;
	private int mBeamWidth;
	public static int offset;
	private static String scoreOutputPath;
	private static List<String> englishstop;
	
	public TrainSingleDocumentHeuristicFunction(int expansion, String[] topic, int beamWidth) {
		englishstop = readFile(edu.oregonstate.util.EecbConstants.STOPWORD_PATH);
		mExpasion = expansion;
		mTopic = topic;
		mBeamWidth = beamWidth;
		Train.currentOutputFileName = "TrainHeuristicFunction";
	}
	
	/**
	 * read the stop words in order to remove the stop words
	 * 
	 * @param fileName
	 * @return
	 */
	private List<String> readFile(String fileName) {
		List<String> al = new ArrayList<String>();
	    try {
	      BufferedReader input = new BufferedReader(new FileReader(fileName));
	      for(String line = input.readLine(); line != null; line = input.readLine()) {
	    	  al.add(line);
	      }
	      input.close();
	      return al;
	    } catch(IOException e) {
	      e.printStackTrace();
	      System.exit(1);
	      return null;
	    } 
	 }
	
	/**
	 * build the vector space model for each document according to tfidf value
	 * 
	 * @param tfidf
	 * @param documentCount how many documents exist in the corpus collection
	 * @return each document is column vector
	 */
	private List<Matrix> vectorSpace(CounterMap<String, Integer> tfidf, int documentCount,
			Set<String> dictionary) {
		int row = tfidf.size();
		List<Matrix> vectors = new ArrayList<Matrix>();
		for (int i = 0; i < documentCount; i++) {
			Matrix vector = new Matrix(row, 1);
			int offset = 0;
			for (String token : dictionary) {
				double count = tfidf.getCount(token, i);
				vector.set(offset, 0, count); // matrix starts with zero
				offset++;
			}
			normalizae(vector);
			vectors.add(vector);
		}
		
		return vectors;
	}
	
	/**
	 * normalize the n * 1 matrix
	 * @param vector
	 */
	private static void normalizae(Matrix vector) {
		double sum = 0.0;
		for (int i = 0; i < vector.getRowDimension(); i++) {
			sum += vector.get(i, 0) * vector.get(i, 0);
		}
		for (int i = 0; i < vector.getRowDimension(); i++) {
			vector.set(i, 0, (vector.get(i, 0) / Math.sqrt(sum)));
		}
	}
	
	private double[] calculatePrecision(Document document, ScoreType type) {
		double sum = 0.0;
		CorefScorer score;
		switch(type) {
			case MUC:
				score = new ScorerMUC();
				break;
			case BCubed:
				score = new ScorerBCubed(BCubedType.Bconll);
				break;
			case CEAF:
				score = new ScorerCEAF();
				break;
			case Pairwise:
				score = new ScorerPairwise();
				break;
			default:
				score = new ScorerMUC();
				break;
		}
    	score.calculateScore(document);
    	sum = score.getF1();
    	double precision = score.getPrecision();
    	double recall = score.getRecall();
    	double[] result = {sum, precision, recall};
		return result;
	}
	
	public void addCorefCluster(Document document, Document corpus) {
		  for(Integer id : document.corefClusters.keySet()) {
			  corpus.addCorefCluster(id, document.corefClusters.get(id));
		  }
	}
	
	
	public void addGoldCorefCluster(Document document, Document corpus) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefClusterWithDuplicateKeys(id, document.goldCorefClusters.get(id));
		}
	}
	
	public void addPredictedMention(Document document, Document corpus) {
		for (Integer id : document.allPredictedMentions.keySet()) {
			corpus.addPredictedMention(id, document.allPredictedMentions.get(id));
		}
	}

	public void addGoldMention(Document document, Document corpus) {
		for (Integer id : document.allGoldMentions.keySet()) {
			corpus.addGoldMention(id, document.allGoldMentions.get(id));
		}
	}
	
	public void merge(Document document, Document corpus) {
		addGoldCorefCluster(document, corpus);
		addPredictedMention(document, corpus);
		addGoldMention(document, corpus);
		addCorefCluster(document, corpus);
		
		for (Integer id : corpus.goldCorefClusters.keySet()) {
			CorefCluster cluster = corpus.goldCorefClusters.get(id);
			for (Mention m : cluster.corefMentions) {
				int mentionID = m.mentionID;
				Mention correspondingMention = corpus.allGoldMentions.get(mentionID);
				int clusterid = id;
				correspondingMention.corefClusterID = clusterid;
			}
		}
	}
	
	// train the model
	// at first, collect the data first, and then use Perceptron to train the model
	public Matrix train() {
		Matrix model = mInitialModel;
		for (String topic : mTopic) {
			List<Document> documents = new ArrayList<Document>();
			ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic " + topic + "................");
			List<String> files  = new ArrayList<String>();
			String topicPath = CDCR.corpusPath + topic + "/";
			files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
			sort(files);
			for (String file : files) {
				ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process document " + file + "...............");
				try {
				    // define the loss function type, and run several experiments
					//for (ScoreType type : ScoreType.values()) {
						ScoreType type = ScoreType.Pairwise;
						CorefSystemSingleDocument cs = new CorefSystemSingleDocument();
						String singleDocument = topicPath + file;
						Document document = cs.getDocument(singleDocument);
						ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
						ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
						ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + printCluster(document.goldCorefClusters));
						cs.corefSystem.coref(document);
						ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Clusters After Stanford's System Preprocess of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.corefClusters.size());
						ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + printCluster(document.corefClusters));
						ResultOutput.writeTextFile(CDCR.outputFileName, "Stanford System merges " + (document.allGoldMentions.size() - document.corefClusters.size()) + " mentions");
						
						//System.out.println(type.toString());
						ResultOutput.writeTextFile(CDCR.outputFileName, type.toString());
						String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
						String prefix = mExpasion + "-" + mBeamWidth + "-" + type.toString() + "-" + topic + "-" + file.substring(0, file.length() - 5) + "-";
						prefix = prefix.concat(timeStamp);
						scoreOutputPath = edu.oregonstate.util.EecbConstants.TEMPORY_RESULT_PATH + prefix;
						System.out.println("Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
						System.out.println("Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
						
						BeamSearch beamSearch = new BeamSearch(mBeamWidth, scoreOutputPath, type, document, mExpasion);
						beamSearch.bestFirstBeamSearch();
						
						ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
						ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
						ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Clusters After Merge: " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.corefClusters.size());
						ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + printCluster(document.goldCorefClusters));
						ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + printCluster(document.corefClusters));
						List<String> rawText = readRawText1(singleDocument);
						document.setRawText(rawText);
						document.setID(topic + "-" + file.substring(0, file.length() - 5));
						documents.add(document);
					//}
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			// use Hierarchical Clustering to reduce the search space
			List<List<String>> allRawText = new ArrayList<List<String>>();
			List<String> allPrefix = new ArrayList<String>();
			for (Document document : documents) {
				allRawText.add(document.getRawText());
				allPrefix.add(document.getID());
			}
			
			System.out.println("build the tf-idf...");
			// get tf-idf value
			edu.oregonstate.cluster.TFIDF tfidf = new edu.oregonstate.cluster.TFIDF(allRawText);
			edu.oregonstate.general.CounterMap<String, Integer> tfIdf = tfidf.buildTFIDF();
			Set<String> dictionary = tfidf.dictionary;
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "feature size :" + dictionary.size());
			for (String term : dictionary) {
				ResultOutput.writeTextFile(CDCR.outputFileName, "feature term :       " + term);
			}
			System.out.println("feature size :" + dictionary.size());
			
			List<Matrix> vectors = vectorSpace(tfIdf, allPrefix.size(), tfidf.dictionary);
			List<edu.oregonstate.data.EecbClusterDocument> anotherdocuments = new ArrayList<edu.oregonstate.data.EecbClusterDocument>();
			assert vectors.size() == allPrefix.size();
			for (int i = 0; i < vectors.size(); i++) {
				EecbClusterDocument document = new EecbClusterDocument(i, vectors.get(i));
				document.setPrefix(allPrefix.get(i));
				anotherdocuments.add(document);
			}
			
			System.out.println("HAC algorithm...");
			// HAC algorithm to select the best initial cluster number for EM
			
			DissimilarityMeasure meausre = new EecbDissimilarityMeasure();
			AgglomerationMethod method = new AverageLinkage();
			HAC clusterer = new HAC(anotherdocuments, meausre, method);
			clusterer.cluster();
			List<String> mergeSequence = clusterer.getSequence();
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "Merge Sequences :");
			for (String sequence : mergeSequence) {
				ResultOutput.writeTextFile(CDCR.outputFileName, "sequence:     " + sequence);
			}
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "Start to merge according to the hierarchical clustering structure");
			for (String sequence : mergeSequence) {
				String[] paras = sequence.split("-");
				int to = Integer.parseInt(paras[0]);
				int from = Integer.parseInt(paras[1]);
				
				Document todocument = documents.get(to);
				
				System.out.println("Number of Gold Mentions of " + topic + "-"  + to + " : " + todocument.allGoldMentions.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-"  + to  + " : " + todocument.allGoldMentions.size());
				System.out.println("Number of gold clusters of " + topic + "-" + to + " : " + todocument.goldCorefClusters.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + to + " : " + todocument.goldCorefClusters.size());
				System.out.println("Number of coref clusters of " + topic + "-" + to + " : " + todocument.corefClusters.size() + " formed after Stanford and within");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + "-" + to + " : " + todocument.corefClusters.size() + " formed after Stanford and within");
				
				Document fromdocument = documents.get(from);
				
				System.out.println("Number of Gold Mentions of " + topic +  "-" + from  + " : " + fromdocument.allGoldMentions.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-" + from  + " : " + fromdocument.allGoldMentions.size());
				System.out.println("Number of gold clusters of " + topic + "-" + from + " : " + fromdocument.goldCorefClusters.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + from + " : " + fromdocument.goldCorefClusters.size());
				System.out.println("Number of coref clusters of " + topic + "-" + from + " : " + fromdocument.corefClusters.size() + " formed after Stanford and within");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + "-" + from + " : " + fromdocument.corefClusters.size() + " formed after Stanford and within");
				
				
				merge(fromdocument, todocument);
				ScoreType type = ScoreType.Pairwise;
				String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
				String prefix = mExpasion + "-" + mBeamWidth + "-" + type.toString() + "-" + topic + "-" + to + "-" + from + "-";
				prefix = prefix.concat(timeStamp);
				scoreOutputPath = edu.oregonstate.util.EecbConstants.TEMPORY_RESULT_PATH + prefix;
				System.out.println("Number of Gold Mentions of " + topic + "-"  + to + "-" + from  + " : " + todocument.allGoldMentions.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-"  + to + "-" + from  + " : " + todocument.allGoldMentions.size());
				System.out.println("Number of gold clusters of " + topic + "-" + to + "-" + from + " : " + todocument.goldCorefClusters.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + to + "-" + from + " : " + todocument.goldCorefClusters.size());
				System.out.println("Number of coref clusters of " + topic + "-" + to + "-" + from + " : " + todocument.corefClusters.size() + " formed after Stanford and within");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + "-" + to + "-" + from + " : " + todocument.corefClusters.size() + " formed after Stanford and within");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + printCluster(todocument.goldCorefClusters));
				ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + printCluster(todocument.corefClusters));
				
				
				BeamSearch beamSearch = new BeamSearch(mBeamWidth, scoreOutputPath, type, todocument, mExpasion);
				beamSearch.bestFirstBeamSearch();
				
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-"  + to + "-" + from  + " : " + todocument.allGoldMentions.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + to + "-" + from + " : " + todocument.goldCorefClusters.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + "-" + to + "-" + from + " : " + todocument.corefClusters.size() + " formed after cross");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + printCluster(todocument.goldCorefClusters));
				ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + printCluster(todocument.corefClusters));
				
				ResultOutput.writeTextFile(CDCR.outputFileName, "MUC: F1: " + calculatePrecision(todocument, ScoreType.MUC)[0] + " P: " + calculatePrecision(todocument, ScoreType.MUC)[1] + " R:" + calculatePrecision(todocument, ScoreType.MUC)[2]);
				ResultOutput.writeTextFile(CDCR.outputFileName, "Bcubed F1: " + calculatePrecision(todocument, ScoreType.BCubed)[0] + " P: " + calculatePrecision(todocument, ScoreType.BCubed)[1] + " R: " + calculatePrecision(todocument, ScoreType.BCubed)[2]);
				ResultOutput.writeTextFile(CDCR.outputFileName, "CEAF F1:" + calculatePrecision(todocument, ScoreType.CEAF)[0] + " P: " + calculatePrecision(todocument, ScoreType.CEAF)[1] + " R: " + calculatePrecision(todocument, ScoreType.CEAF)[2]);
			}

			ResultOutput.writeTextFile(CDCR.outputFileName, "end to process topic" + topic + "................");
		}

		return model;
	}
	
	/**
	 * print the cluster information
	 * 
	 * @param clusters
	 * @return
	 */
	public static String printCluster(Map<Integer, CorefCluster> clusters) {
		StringBuilder sb = new StringBuilder();
		for (Integer key : clusters.keySet()) {
			CorefCluster cluster = clusters.get(key);
			sb.append(Integer.toString(key) + "[ ");
			for (Mention mention : cluster.getCorefMentions()) {
				sb.append(mention.mentionID + " ");
			}
			sb.append(" ]");
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	private static List<String> readRawText1(String filename) {
		List<String> rawText = new ArrayList<String>();
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				String strline = line.replaceAll("\\<[^\\>]*\\>", "");
				if (strline.length() < line.length()) {
					strline = strline.toLowerCase();
					for (String s : strline.split("\\s+")) {
						s = s.replaceAll("[^a-zA-Z0-9]", "");
						if (s.equals("")) continue;
					/**
					 * According to the fourth section of the paper
					 * <p>
					 * The collection documents were preprocessed as follows:
					 * (1) all words were converted to lower case;
					 * (2) stop words and numbers were discarded;
					 * (3) terms that appear in a single document were removed <b>NOTE</b> processing in later part 
					 */
						if (englishstop.contains(s)) continue;
						if (s.matches("\\d+")) continue;
						rawText.add(s);
					}
				}
			}
			entitiesBufferedReader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return rawText;
	}
	
	
	/** sort the files name according to the sequence*/
	public static void sort(List<String> files) {
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