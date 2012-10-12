package edu.oregonstate.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import com.sun.org.apache.regexp.internal.RESyntaxException;

import edu.oregonstate.cluster.HAC;
import edu.oregonstate.cluster.agglomeration.AgglomerationMethod;
import edu.oregonstate.cluster.agglomeration.AverageLinkage;
import edu.oregonstate.cluster.experiment.DissimilarityMeasure;
import edu.oregonstate.cluster.experiment.EecbDissimilarityMeasure;
import edu.oregonstate.data.PriorityQueue;

import edu.oregonstate.CDCR;
import edu.oregonstate.CorefSystemSingleDocument;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

import Jama.Matrix;

/**
 * train a single document heuristic function
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainSingleDocumentHeuristicFunction {

	private int mExpasion;
	public static final Logger logger = Logger.getLogger(TrainHeuristicFunction.class.getName());
	// topic in the corpus
	private String[] mTopic;
	private Matrix mInitialModel;
	private int mBeamWidth;
	public static int offset;
	private static String scoreOutputPath;
	private static List<String> englishstop;
	
	public TrainSingleDocumentHeuristicFunction(int expansion, String[] topic, int beamWidth) {
		englishstop = readFile(edu.oregonstate.util.Constants.STOPWORD_PATH);
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
	private List<Matrix> vectorSpace(edu.oregonstate.general.CounterMap<String, Integer> tfidf, int documentCount,
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
	
	// in oder to calculate the cost function
	private double calculateScore(Counter<String> features, Matrix mModel) {
		double sum = 0.0;
        for (int i = 0; i < mModel.getRowDimension(); i++) {
        	if (i == 0) {
        		sum += mModel.get(i, 0);
            } else {
                sum += features.getCount(Feature.featuresName[i-1]) * mModel.get(i, 0);
            }
        }
        return sum;
    }
	
	// use ceaf score to perform the loss function
	private Double calculateLossFunction(Document document, ScoreType type) {
		Double sum = 0.0;
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
		return (1 - sum);
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
	
	// create features for each state
	private Counter<String> getFeatures(State<CorefCluster> state) {
		Counter<String> features = new ClassicCounter<String>();
		return features;
	}
		
	// get the largest value
	public Integer compare_hashMap_min(Map<Integer, Double> scores) {
        Collection<Double> c = scores.values();
        Double minvalue = Collections.min(c);
        Set<Integer> scores_set = scores.keySet();
        Iterator<Integer> scores_it = scores_set.iterator();
        while(scores_it.hasNext()) {
        		Integer id = scores_it.next();
                Double value = scores.get(id);
                if (value == minvalue) {
                        return id;
                }
        }
        return null;
	}
	
	// for each state, get its successor states
	// there are two actions, one is Merge, one is Split
	/*
	private Map<Integer, State<CorefCluster>> adj(State<CorefCluster> index) {
		Map<Integer, State<CorefCluster>> neighbors = new HashMap<Integer, State<CorefCluster>>();
		Map<Integer, CorefCluster> clusters = index.getState();
		List<Integer> ids = new ArrayList<Integer>();
		for (Integer key : clusters.keySet()) {
			ids.add(key);
		}
		int size = ids.size();
		ResultOutput.writeTextFile(CDCR.outputFileName, "before create children: total of clusters : " + size);
		System.out.println("total of clusters : " + size);
		// merge
		for (int i = 0; i < (size - 1); i++) {
			for (int j = 0; j < i; j++) {
				CorefCluster icluster = clusters.get(ids.get(i));
				CorefCluster jcluster = clusters.get(ids.get(j));
				ResultOutput.serialize(icluster, icluster.clusterID, edu.oregonstate.util.Constants.RESULT_PATH);
            	CorefCluster cicluster = ResultOutput.deserialize(Integer.toString(icluster.clusterID) + ".ser", edu.oregonstate.util.Constants.RESULT_PATH, true);
            	mergeClusters(cicluster, jcluster);
				State<CorefCluster> newindex = new State<CorefCluster>();
				newindex.add(cicluster.clusterID, cicluster);
				for (Integer id : ids) {
					CorefCluster cluster = clusters.get(id);
					if (!id.equals(ids.get(i)) && !id.equals(ids.get(j))) {
						newindex.add(id, cluster);
					}
				}
				
				neighbors.put(offset, newindex);
				offset += 1;
			}
		}
		ResultOutput.writeTextFile(CDCR.outputFileName, "after create children: total of clusters : " + (size - 1));
		//Cloner cloner = new Cloner();
		//Map<Integer, CorefCluster> copyClusters = cloner.deepClone(clusters);
		// split
		
		for (int i = 0; i < (size - 1); i++) {
			CorefCluster cluster = clusters.get(ids.get(i));
			if (cluster.corefMentions.size() < 2) continue;
			
			
			for (Iterator<Mention> iter = cluster.corefMentions.iterator(); iter.hasNext();) {
				Mention m = iter.next();
				State<CorefCluster> newindex = new State<CorefCluster>();
				for (Integer key : copyClusters.keySet()) {
					newindex.add(key, copyClusters.get(key));
				}
				
				newindex.add(m.mentionID, new CorefCluster(m.mentionID, new HashSet<Mention>(Arrays.asList(m))));
				newindex.getState().get(ids.get(i)).corefMentions.remove(m);
				neighbors.add(newindex);
			}
			
		}
		
		
		//return neighbors;
	}*/
	
	
	private Set<String> adj(State<CorefCluster> index) {
		Set<String> actions = new HashSet<String>();
		Map<Integer, CorefCluster> clusters = index.getState();
		List<Integer> ids = new ArrayList<Integer>();
		for (Integer key : clusters.keySet()) {
			ids.add(key);
		}
		int size = ids.size();
		ResultOutput.writeTextFile(CDCR.outputFileName, "before create children: total of clusters : " + size);
		System.out.println("total of clusters : " + size);
		
		// merge
		for (int i = 0; i < size; i++) {
			Integer i_id = ids.get(i);
			CorefCluster icluster = clusters.get(i_id);
			int isize = icluster.corefMentions.size();
			for (int j = 0; j < i; j++) {
				Integer j_id = ids.get(j);
				CorefCluster jcluster = clusters.get(j_id);
				int jsize = jcluster.corefMentions.size();
				//if (isize == 1 && jsize == 1) {
					String action = i_id + "-" + j_id + "-" + offset;
					actions.add(action);
					offset += 1;
				//}
			}
		}
		ResultOutput.writeTextFile(CDCR.outputFileName, "after create children: total of clusters : " + (size - 1));
		ResultOutput.writeTextFile(CDCR.outputFileName, "the number of candidate sets :" + actions.size());
		System.out.println("the number of candidate sets : " + actions.size());
		return actions;
	}
	
	private void mergeClusters(CorefCluster to, CorefCluster from) {
		int toID = to.clusterID;
	    for (Mention m : from.corefMentions){
	      m.corefClusterID = toID;
	    }
	    if(Constants.SHARE_ATTRIBUTES){
	      to.numbers.addAll(from.numbers);
	      if(to.numbers.size() > 1 && to.numbers.contains(Number.UNKNOWN)) {
	        to.numbers.remove(Number.UNKNOWN);
	      }

	      to.genders.addAll(from.genders);
	      if(to.genders.size() > 1 && to.genders.contains(Gender.UNKNOWN)) {
	        to.genders.remove(Gender.UNKNOWN);
	      }

	      to.animacies.addAll(from.animacies);
	      if(to.animacies.size() > 1 && to.animacies.contains(Animacy.UNKNOWN)) {
	        to.animacies.remove(Animacy.UNKNOWN);
	      }

	      to.nerStrings.addAll(from.nerStrings);
	      if(to.nerStrings.size() > 1 && to.nerStrings.contains("O")) {
	        to.nerStrings.remove("O");
	      }
	      if(to.nerStrings.size() > 1 && to.nerStrings.contains("MISC")) {
	        to.nerStrings.remove("MISC");
	      }
	    }
	    
	    to.heads.addAll(from.heads);
	    to.corefMentions.addAll(from.corefMentions);
	    to.words.addAll(from.words);
	    if(from.firstMention.appearEarlierThan(to.firstMention) && !from.firstMention.isPronominal()) to.firstMention = from.firstMention;
	    if(from.representative.moreRepresentativeThan(to.representative)) to.representative = from.representative;
	}
	
	
	/*
	public void initialize(Document document, State<CorefCluster> initialState, State<CorefCluster> goalState) {
		for (Integer key : document.corefClusters.keySet()) {
			CorefCluster cluster = document.corefClusters.get(key);
			ResultOutput.serialize(cluster, key, edu.oregonstate.util.Constants.RESULT_PATH);
			CorefCluster cpCluster = ResultOutput.deserialize(key.toString() + ".ser", edu.oregonstate.util.Constants.RESULT_PATH, true);
			initialState.add(key, cpCluster);
		}
		
		for (Integer key : document.goldCorefClusters.keySet()) {
			CorefCluster cluster = document.goldCorefClusters.get(key);
			ResultOutput.serialize(cluster, key, edu.oregonstate.util.Constants.RESULT_PATH);
			CorefCluster cpCluster = ResultOutput.deserialize(key.toString() + ".ser", edu.oregonstate.util.Constants.RESULT_PATH, true);
			goalState.add(key, cpCluster);
		}
	}*/
	
	/**
	 * initial state and goal state
	 * we represent the initial state and goal state as coreference trees in order to make the computation tractable.
	 * At first, we do not represent the cluster as a tree
	 * 
	 * At first, we need to make sure that gold Mention and predicted mention has the same number of mentions
	 * 
	 * @param document
	 * @param initialState
	 * @param goalState
	 */
	public void initialize(Document document, State<CorefCluster> initialState) {
		for (Integer key : document.corefClusters.keySet()) {
			CorefCluster cluster = document.corefClusters.get(key);
			//ResultOutput.serialize(cluster, key, edu.oregonstate.util.Constants.RESULT_PATH);
			//CorefCluster cpCluster = ResultOutput.deserialize(key.toString() + ".ser", edu.oregonstate.util.Constants.RESULT_PATH, true);
			CorefCluster cpCluster = new CorefCluster(key, cluster.getCorefMentions());
			initialState.add(key, cpCluster);
		}
		
		/*
		for (Integer key : document.goldCorefClusters.keySet()) {
			CorefCluster cluster = document.goldCorefClusters.get(key);
			ResultOutput.serialize(cluster, key, edu.oregonstate.util.Constants.RESULT_PATH);
			CorefCluster cpCluster = ResultOutput.deserialize(key.toString() + ".ser", edu.oregonstate.util.Constants.RESULT_PATH, true);
			goalState.add(key, cpCluster);
		}
		*/
	}
	
	public void setNextDocument(Document documentState, State<CorefCluster> state) {
		documentState.corefClusters = state.getState();
		for (Integer id : documentState.corefClusters.keySet()) {
			CorefCluster cluster = documentState.corefClusters.get(id);
			for (Mention m : cluster.corefMentions) {
				int mentionID = m.mentionID;
				Mention correspondingMention = documentState.allPredictedMentions.get(mentionID);
				int clusterid = id;
				correspondingMention.corefClusterID = clusterid;
			}
		}
	}
	
	public void addCorefCluster(Document document, Document corpus) {
		  for(Integer id : document.corefClusters.keySet()) {
			  corpus.addCorefCluster(id, document.corefClusters.get(id));
		  }
	}
	
	public void addGoldCorefCluster(Document document, Document corpus) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefCluster(id, document.goldCorefClusters.get(id));
		}
	}
	
	public void addGoldCorefCluster1(Document document, Document corpus) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefCluster1(id, document.goldCorefClusters.get(id));
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
	
	/**
	 * add four fields to the corpus
	 * 
	 * @param document
	 */
	public void add(Document document, Document corpus) {
		addGoldCorefCluster(document, corpus);
		addPredictedMention(document, corpus);
		addGoldMention(document, corpus);
	}
	
	public void merge(Document document, Document corpus) {
		addGoldCorefCluster1(document, corpus);
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
	
	private boolean isGoalState(Document document, State<CorefCluster> state, ScoreType type) {
		boolean isGoal = false;
		Document documentState = new Document();
    	add(document, documentState);
    	setNextDocument(documentState, state);
    	double[] scores = calculatePrecision(documentState, type);
    	double score = scores[0];
		if (score == 1.0) {
			isGoal = true;
		}
		return isGoal;
	}
	
	/** update the corefIDs of predictedMentions */
	public void updatePredictedMentions(Document document) {
		for (Integer id : document.corefClusters.keySet()) {
			CorefCluster cluster = document.corefClusters.get(id);
			for (Mention m : cluster.corefMentions) {
				int mentionID = m.mentionID;
				Mention correspondingMention = document.allPredictedMentions.get(mentionID);
				int clusterid = id;
				correspondingMention.corefClusterID = clusterid;
			}
		}
	}
	
	private boolean isGoalState1(Document document, ScoreType type) {
		boolean isGoal = false;
    	double[] scores = calculatePrecision(document, type);
    	double score = scores[0];
		if (score == 1.0) {
			isGoal = true;
		}
		return isGoal;
	}
	
	
	public void update(Document document, ScoreType type) {
		offset = 2;
		Double globalScore = 0.0;
		Map<Integer, State<CorefCluster>> closedList = new HashMap<Integer, State<CorefCluster>>();
		Map<Integer, State<CorefCluster>> beam = new HashMap<Integer, State<CorefCluster>>();
		State<CorefCluster> initialState = new State<CorefCluster>();
		initialize(document, initialState);
		closedList.put(1, initialState);
		beam.put(1, initialState);
		boolean breakWhile = false;
		int i = 0;
		while(beam.size() != 0) {
			//Map<Integer, String> set = new HashMap<Integer, String>();
			ResultOutput.deleteResult(edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);

			State<CorefCluster> indexState = new State<CorefCluster>();
			System.out.println("the size of beam is :" + beam.size());
			for (Integer id : beam.keySet()) {
				for (Integer key : beam.get(id).getState().keySet()) {
					indexState.add(key, beam.get(id).getState().get(key));
				}
				
				boolean isgoal = isGoalState(document, indexState, type);
				if (isgoal) {
					ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
					ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
					ResultOutput.writeTextFile(CDCR.outputFileName, "global" + type.toString() +" F1 score: 1.0");
					ResultOutput.writeTextFile(scoreOutputPath, "1.0");
					ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
					breakWhile = true;
					break;
				}
				
				System.out.println("The id of the best state in the beam is : " + id);
				//ResultOutput.serialize(state, id, edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
			}
			if (breakWhile) break;
			ResultOutput.writeTextFile(CDCR.outputFileName, "action " + i);
			System.out.println(type.toString() + " loss function : action " + i);
			//Document documentStateState = new Document();
			//add(document, documentStateState);
			//setNextDocument(documentStateState,indexState);
			
			double[] localScores = calculatePrecision(document, type);
			double localScore = localScores[0];
			System.out.println(localScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "local " + type.toString() + " F1 Score: " + Double.toString(localScore));
			ResultOutput.writeTextFile(CDCR.outputFileName, "local " + type.toString() + " precision Score: " + Double.toString(localScores[1]));
			ResultOutput.writeTextFile(CDCR.outputFileName, "local " + type.toString() + " recall Score: " + Double.toString(localScores[2]));
			ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters : \n" + printCluster(document.corefClusters));
			
			ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: " + globalScore.toString());
			if (localScore >= globalScore) globalScore = localScore;
			if (globalScore > localScore) break;
			System.out.println(globalScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: " + globalScore.toString());
			ResultOutput.writeTextFile(scoreOutputPath, globalScore.toString());
			String beforetimeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
			ResultOutput.writeTextFile(CDCR.outputFileName, beforetimeStamp);
			System.out.println(beforetimeStamp);
			
			/** get the candidate lists*/
			Set<String> actions = adj(indexState);
			
			String aftertimeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
			ResultOutput.writeTextFile(CDCR.outputFileName, aftertimeStamp);
			System.out.println(aftertimeStamp);
			
			//set.remove(index);
			beam = new HashMap<Integer, State<CorefCluster>>();
			PriorityQueue<String> pq = new PriorityQueue<String>();
			
			try {
			
			int actionSize = actions.size();
			int increment = 0;
			for (String action : actions) {
				increment += 1;
				System.out.println("the remaining action " + (actionSize - increment));
				String beforetimeStamp1 = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
				System.out.println(beforetimeStamp1);
				
				//State<CorefCluster> neighbor = ResultOutput.deserialize(index.toString() + ".ser", edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH, false);
				String[] ids = action.split("-");
				Integer i_id = Integer.parseInt(ids[0]);
				Integer j_id = Integer.parseInt(ids[1]);
				Map<Integer, CorefCluster> corefClusters = document.corefClusters;
				CorefCluster iCluster = corefClusters.get(i_id);
				CorefCluster jCluster = corefClusters.get(j_id);
				//ResultOutput.serialize(iCluster, i_id, edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
				//CorefCluster cpCluster = ResultOutput.deserialize(i_id + ".ser", edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH, true);
				CorefCluster cpCluster = new CorefCluster(i_id, iCluster.getCorefMentions());
				mergeClusters(cpCluster, jCluster);
				document.corefClusters.put(i_id, cpCluster);
				document.corefClusters.remove(j_id);
				
				updatePredictedMentions(document);
				
				boolean isgoal = isGoalState1(document, type);
				if (isgoal) {
					ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
					ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
					ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: 1.0");
					ResultOutput.writeTextFile(scoreOutputPath, Double.toString(1.0));
					ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
					breakWhile = true;
					break;
				}
				
				//Document documentState = new Document();
            	//add(document, documentState);
            	//setNextDocument(documentState, neighbor);
            	double[] scores = calculatePrecision(document, type);
            	double score = scores[0];
            	boolean isNumber = Double.isNaN(score);
            	
            	// for this case, we need to make sure that the next state should score higher than the previous score
            	// I can modify here
            	if (!isNumber && score >= globalScore) pq.add(action, score);
            	
            	document.corefClusters.put(i_id, iCluster);
            	document.corefClusters.put(j_id, jCluster);
            	
            	updatePredictedMentions(document);
            	
            	String aftertimeStamp1 = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
    			System.out.println(aftertimeStamp1);
			} } catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			if (breakWhile) break;
			while ((pq.size() > 0 ) && (mBeamWidth > beam.size())) {
				String action = pq.next();
				State<CorefCluster> initial = new State<CorefCluster>();
				for (Integer key : indexState.getState().keySet()) {
					initial.add(key, indexState.getState().get(key));
				}
				
				String[] ids = action.split("-");
				Integer i_id = Integer.parseInt(ids[0]);
				Integer j_id = Integer.parseInt(ids[1]);
				CorefCluster iCluster = initial.getState().get(i_id);
				CorefCluster cpCluster = new CorefCluster(i_id, iCluster.getCorefMentions());
				CorefCluster jCluster = initial.getState().get(j_id);
				mergeClusters(cpCluster, jCluster);
				initial.remove(i_id);
				initial.remove(j_id);
				initial.add(i_id, cpCluster);
				
				boolean deplicate = detectDuplicate(closedList, initial);
                if (!deplicate) {
                	CorefCluster cluster_i = document.corefClusters.get(i_id);
                	CorefCluster cluster_j = document.corefClusters.get(j_id);
                	mergeClusters(cluster_i, cluster_j);
                	document.corefClusters.remove(j_id);
                	
                	for (Integer id : document.corefClusters.keySet()) {
            			CorefCluster cluster = document.corefClusters.get(id);
            			for (Mention m : cluster.corefMentions) {
            				int mentionID = m.mentionID;
            				Mention correspondingMention = document.allPredictedMentions.get(mentionID);
            				int clusterid = id;
            				correspondingMention.corefClusterID = clusterid;
            			}
            		}
                	
                    beam.put(Integer.parseInt(ids[2]), initial);
                    closedList.put(Integer.parseInt(ids[2]), initial);
                }
                
			}
			i+= 1;
			
			if (i >= mExpasion || breakWhile) {
					break;
			}
		}
		ResultOutput.deleteResult(edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
	}
	
	// beam search
	/*
	public void update(Document document, ScoreType type) {
		offset = 2;
		Double globalScore = 0.0;
		Map<Integer, State<CorefCluster>> closedList = new HashMap<Integer, State<CorefCluster>>();
		Map<Integer, State<CorefCluster>> beam = new HashMap<Integer, State<CorefCluster>>();
		State<CorefCluster> initialState = new State<CorefCluster>();
		State<CorefCluster> goalState = new State<CorefCluster>();
		initialize(document, initialState, goalState);
		closedList.put(1, initialState);
		beam.put(1, initialState);
		boolean breakWhile = false;
		int i = 0;
		while(beam.size() != 0) {
			Map<Integer, State<CorefCluster>> set = new HashMap<Integer, State<CorefCluster>>();
			Map<Integer, Double> values = new HashMap<Integer, Double>();
			for (Integer id : beam.keySet()) {
				State<CorefCluster> state = beam.get(id);
            	boolean isgoal = isGoalState(document, state, type);
				if (isgoal) {
					ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
					ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
					ResultOutput.writeTextFile(CDCR.outputFileName, "global" + type.toString() +" F1 score: 1.0");
					ResultOutput.writeTextFile(scoreOutputPath, "1.0");
					ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
					breakWhile = true;
					break;
				}
				Document documentState = new Document();
            	add(document, documentState);
            	setNextDocument(documentState, state);
				values.put(id, calculateLossFunction(documentState, type));
				set.put(id, state);
			}
			
			if (breakWhile) break;
			
			Integer index = compare_hashMap_min(values);
			State<CorefCluster> indexState = set.get(index);
			ResultOutput.writeTextFile(CDCR.outputFileName, "action " + i);
			System.out.println(type.toString() + " loss function : action " + i);
			
			Document documentStateState = new Document();
			add(document, documentStateState);
			setNextDocument(documentStateState,indexState);
			
			double[] localScores = calculatePrecision(documentStateState, type);
			double localScore = localScores[0];
			System.out.println(localScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "local" + type.toString() + " F1 Score: " + Double.toString(localScore));
			ResultOutput.writeTextFile(CDCR.outputFileName, "local" + type.toString() + " precision Score: " + Double.toString(localScores[1]));
			ResultOutput.writeTextFile(CDCR.outputFileName, "local" + type.toString() + " recall Score: " + Double.toString(localScores[2]));
			if (localScore > globalScore) globalScore = localScore;
			System.out.println(globalScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "global" + type.toString() +" F1 score: " + globalScore.toString());
			ResultOutput.writeTextFile(scoreOutputPath, globalScore.toString());
			String beforetimeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
			ResultOutput.writeTextFile(CDCR.outputFileName, beforetimeStamp);
			System.out.println(beforetimeStamp);
			Map<Integer, State<CorefCluster>> adjacent = adj(indexState);
			String aftertimeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
			ResultOutput.writeTextFile(CDCR.outputFileName, aftertimeStamp);
			System.out.println(aftertimeStamp);
			
			for (Integer id : adjacent.keySet()) { // consider every pair of cluster, it can be any cluster pair
				State<CorefCluster> neighbor = adjacent.get(id);
            	boolean isgoal = isGoalState(document, neighbor, type);
				if (isgoal) {
					ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
					ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
					ResultOutput.writeTextFile(CDCR.outputFileName, "global" + type.toString() +" F1 score: 1.0");
					ResultOutput.writeTextFile(scoreOutputPath, "1.0");
					ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
					breakWhile = true;
					break;
				}
					
				set.put(id, neighbor);
			}
			
			if (breakWhile) break;
			closedList.put(index, indexState);
			set.remove(index);
			beam = new HashMap<Integer, State<CorefCluster>>();
			while ((set.size() != 0 ) && (mBeamWidth > beam.size())) {
                Map<Integer, Double> heuristicValue = new HashMap<Integer, Double>();
                for (Integer id : set.keySet()) {
                	State<CorefCluster> key = set.get(id);
                	Document documentState = new Document();
                	add(document, documentState);
    				setNextDocument(documentState, key);
                	Double value = calculateLossFunction(documentState, type);
                    heuristicValue.put(id, value);
                }
                Integer minIndex = compare_hashMap_min(heuristicValue);
                State<CorefCluster> state = set.get(minIndex);
                boolean deplicate = detectDuplicate(closedList, state);
                if (!deplicate) {
                        beam.put(minIndex, state);
                }
                
                Iterator<Integer> keys = set.keySet().iterator();
                while(keys.hasNext()) {
                	Integer key = keys.next();
                    if (key.equals(minIndex)) keys.remove();
                }
			}

			i+= 1;
			
			if (i >= mExpasion || breakWhile) {
					break;
			}
		}
		
	}
	*/
	
	private boolean detectDuplicate(Map<Integer, State<CorefCluster>> closedList, State<CorefCluster> index) {
		boolean duplciate = false;
		ScorerCEAF score = new ScorerCEAF();
		for (Integer key : closedList.keySet()) {
			State<CorefCluster> visited = closedList.get(key);
			Map<Integer, CorefCluster> visitedCorefClusters = visited.getState();
			Map<Integer, CorefCluster> indexCorefClusters = index.getState();
			if (visitedCorefClusters.size() != indexCorefClusters.size()) continue;
			//TODO
			// maybe change
			double precisionNumSum = score.scoreHelper(visitedCorefClusters, indexCorefClusters);
			double precision = score.scoreHelper(visitedCorefClusters, visitedCorefClusters);
			double recallNumSum = score.scoreHelper(visitedCorefClusters, indexCorefClusters);
			double recall = score.scoreHelper(indexCorefClusters, indexCorefClusters);
			if ((precisionNumSum == precision) && (recallNumSum == recall)) {
				duplciate = true;
				break;
			}
		}
		return duplciate;
	}
	
	private Matrix getDifference(State<CorefCluster> state, State<CorefCluster> goalState) {
		Counter<String> stateFeature = getFeatures(state);
		Counter<String> goalFeature = getFeatures(goalState);
		Matrix model = new Matrix(stateFeature.size() + 1, 1);
		model.set(0, 0, 0);
		int i = 1;
		for (String feature : stateFeature.keySet()) {
			model.set(i, 0, goalFeature.getCount(feature) - stateFeature.getCount(feature));
			i += 1;
		}
		return model;
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
						scoreOutputPath = edu.oregonstate.util.Constants.TEMPORY_RESULT_PATH + prefix;
						System.out.println("Number of Gold Mentions of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.allGoldMentions.size());
						System.out.println("Number of gold clusters of " + topic + "-" + file.substring(0, file.length() - 5) + " : " + document.goldCorefClusters.size());
						
						if (document.allGoldMentions.size() != document.goldCorefClusters.size()) {
							update(document, type);
						}
						
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
			List<edu.oregonstate.data.Document> anotherdocuments = new ArrayList<edu.oregonstate.data.Document>();
			assert vectors.size() == allPrefix.size();
			for (int i = 0; i < vectors.size(); i++) {
				edu.oregonstate.data.Document document = new edu.oregonstate.data.Document(i, vectors.get(i));
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
				scoreOutputPath = edu.oregonstate.util.Constants.TEMPORY_RESULT_PATH + prefix;
				System.out.println("Number of Gold Mentions of " + topic + "-"  + to + "-" + from  + " : " + todocument.allGoldMentions.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-"  + to + "-" + from  + " : " + todocument.allGoldMentions.size());
				System.out.println("Number of gold clusters of " + topic + "-" + to + "-" + from + " : " + todocument.goldCorefClusters.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + to + "-" + from + " : " + todocument.goldCorefClusters.size());
				System.out.println("Number of coref clusters of " + topic + "-" + to + "-" + from + " : " + todocument.corefClusters.size() + " formed after Stanford and within");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + "-" + to + "-" + from + " : " + todocument.corefClusters.size() + " formed after Stanford and within");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + printCluster(todocument.goldCorefClusters));
				ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + printCluster(todocument.corefClusters));
				
				
				if (todocument.allGoldMentions.size() != todocument.goldCorefClusters.size()) {
					update(todocument, type);
				}
				
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + "-"  + to + "-" + from  + " : " + todocument.allGoldMentions.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + "-" + to + "-" + from + " : " + todocument.goldCorefClusters.size());
				ResultOutput.writeTextFile(CDCR.outputFileName, "Number of coref clusters of " + topic + "-" + to + "-" + from + " : " + todocument.corefClusters.size() + " formed after cross");
				ResultOutput.writeTextFile(CDCR.outputFileName, "Gold clusters : \n" + printCluster(todocument.goldCorefClusters));
				ResultOutput.writeTextFile(CDCR.outputFileName, "Coref Clusters: \n" + printCluster(todocument.corefClusters));
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
	public String printCluster(Map<Integer, CorefCluster> clusters) {
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
	
	/**
	 * just read the annotated example
	 */
	private List<String> readRawText(String filename) {
		// put all documents into one document
		List<String> rawText = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			int i = 0;
			// filename : 1.eecb
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				String strline = line.replaceAll("\\<[^\\>]*\\>", "");
				if (strline.length() < line.length()) {
					rawText.add(strline);
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}

        return rawText;
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
	public void sort(List<String> files) {
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

/*
document.corefClusters = document.goldCorefClusters;
for (Integer id : document.corefClusters.keySet()) {
	CorefCluster cluster = document.corefClusters.get(id);
	for (Mention m : cluster.corefMentions) {
		int mentionID = m.mentionID;
		Mention correspondingMention = document.allPredictedMentions.get(mentionID);
		int clusterid = id;
		correspondingMention.corefClusterID = clusterid;
	}
}
document.corefClusters = new HashMap<Integer, CorefCluster>();
for (Integer id : document.goldCorefClusters.keySet()) {
	CorefCluster cluster = document.goldCorefClusters.get(id);
	ResultOutput.serialize(cluster);
	CorefCluster coCluster = ResultOutput.deserialize(true);
	document.corefClusters.put(id, coCluster);
}
calculatePrecision(document, type);
*/
