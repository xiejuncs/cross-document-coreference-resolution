package edu.oregonstate.training;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import Jama.Matrix;
import edu.oregonstate.CDCR;
import edu.oregonstate.CorefSystem;
import edu.oregonstate.data.PriorityQueue;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * train heuristic function for beam search
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainHeuristicFunction {
	
	private int mExpasion;
	public static final Logger logger = Logger.getLogger(TrainHeuristicFunction.class.getName());
	// topic in the corpus
	private String[] mTopic;
	private Matrix mInitialModel;
	private int mBeamWidth;
	private int offset;
	private static String scoreOutputPath;

	public TrainHeuristicFunction(int expansion, String[] topic, int beamWidth) {
		mExpasion = expansion;
		mTopic = topic;
		mBeamWidth = beamWidth;
		Train.currentOutputFileName = "TrainHeuristicFunction";
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
	
	//TODO
	// do not define neighbors, just serialize the newindex to id.ser, and then in the update phase, deserize it to a state
	// for each state, get its successor states
	// there are two actions, one is Merge, one is Split
	private void adj(State<CorefCluster> index, Document document, double localScore, ScoreType type) {
		//Map<Integer, State<CorefCluster>> neighbors = new HashMap<Integer, State<CorefCluster>>();
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
			CorefCluster icluster = clusters.get(ids.get(i));
			for (int j = 0; j < i; j++) {
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
				//ResultOutput.serialize(newindex, offset);
				//neighbors.put(offset, newindex);
				Document documentState = new Document();
            	add(document, documentState);
            	setNextDocument(documentState, newindex);
            	double[] localScores = calculatePrecision(documentState, type);
            	double score = localScores[0];
            	if (score >= localScore) {
            		ResultOutput.serialize(newindex, offset, edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
            	}
				offset += 1;
			}
		}
		ResultOutput.writeTextFile(CDCR.outputFileName, "before create children: total of clusters : " + (size - 1));
		//Cloner cloner = new Cloner();
		//Map<Integer, CorefCluster> copyClusters = cloner.deepClone(clusters);
		// split
		/*
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
		*/
		
		//return neighbors;
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
	
	public void addGoldCorefCluster(Document document, Document corpus) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefCluster(id, document.goldCorefClusters.get(id));
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
	
	/**
	 * beam search
	 * using the serialized and deserialized to avoid the memory overhead
	 * 
	 * @param document
	 * @param type
	 */
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
			//Map<Integer, State<CorefCluster>> set = new HashMap<Integer, State<CorefCluster>>();
			ResultOutput.deleteResult(edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
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
				ResultOutput.serialize(state, id, edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
				//set.put(id, state);
			}
			if (breakWhile) break;
			
			Integer index = compare_hashMap_min(values);
			State<CorefCluster> indexState = ResultOutput.deserialize(index.toString() + ".ser", edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH, true);
			//State<CorefCluster> indexState = ResultOutput.deserialize(index.toString() + ".ser");
			ResultOutput.writeTextFile(CDCR.outputFileName, "action " + i);
			System.out.println(type.toString() + " loss function : action " + i);
			
			Document documentStateState = new Document();
			add(document, documentStateState);
			setNextDocument(documentStateState,indexState);
			
			double[] localScores = calculatePrecision(documentStateState, type);
			double localScore = localScores[0];
			System.out.println(localScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "local " + type.toString() + " F1 Score: " + Double.toString(localScore));
			ResultOutput.writeTextFile(CDCR.outputFileName, "local " + type.toString() + " precision Score: " + Double.toString(localScores[1]));
			ResultOutput.writeTextFile(CDCR.outputFileName, "local " + type.toString() + " recall Score: " + Double.toString(localScores[2]));
			if (localScore > globalScore) globalScore = localScore;
			System.out.println(globalScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: " + globalScore.toString());
			ResultOutput.writeTextFile(scoreOutputPath, globalScore.toString());
			String beforetimeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
			ResultOutput.writeTextFile(CDCR.outputFileName, beforetimeStamp);
			System.out.println(beforetimeStamp);
			adj(indexState, document, localScore, type);
			String aftertimeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
			ResultOutput.writeTextFile(CDCR.outputFileName, aftertimeStamp);
			System.out.println(aftertimeStamp);
			
			closedList.put(index, indexState);
			//set.remove(index);
			beam = new HashMap<Integer, State<CorefCluster>>();
			PriorityQueue<State<CorefCluster>> pq = new PriorityQueue<State<CorefCluster>>();
			
			List<String> files  = new ArrayList<String>();
			String topicPath = edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH;
			files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
			for (String file : files) {
				State<CorefCluster> neighbor = ResultOutput.deserialize(file, edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH, false);
				boolean isgoal = isGoalState(document, neighbor, type);
				if (isgoal) {
					ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
					ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
					ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: 1.0");
					ResultOutput.writeTextFile(scoreOutputPath, Double.toString(1.0));
					ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
					breakWhile = true;
					break;
				}
				
				Document documentState = new Document();
            	add(document, documentState);
            	setNextDocument(documentState, neighbor);
            	double[] scores = calculatePrecision(documentState, type);
            	double score = scores[0];
            	neighbor.setID(Integer.parseInt(file.substring(0, file.length() - 4)));
            	pq.add(neighbor, score);
				
				//set.put(Integer.parseInt(fileName), neighbor);
			}
			if (breakWhile) break;
			while ((pq.size() > 0 ) && (mBeamWidth > beam.size())) {
				State<CorefCluster> state = pq.next();
				boolean deplicate = detectDuplicate(closedList, state);
                if (!deplicate) {
                    beam.put(state.getID(), state);
                }
			}
			
			/*
			for (Integer id : adjacent.keySet()) { // consider every pair of cluster, it can be any cluster pair
				State<CorefCluster> neighbor = adjacent.get(id);
				Document documentState = new Document();
            	add(document, documentState);
            	setNextDocument(documentState, neighbor);
            	double[] scores = calculatePrecision(documentState, type);
            	double score = scores[0];
				if (score == 1.0) {
					ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
					ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
					ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: " + score);
					ResultOutput.writeTextFile(scoreOutputPath, Double.toString(score));
					ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
					breakWhile = true;
					break;
				}
					
				set.put(id, neighbor);
			}
			*/
			
			
			
			
			
			/*
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
			*/
			
			i+= 1;
			
			if (i >= mExpasion || breakWhile) {
					break;
			}
		}
		ResultOutput.deleteResult(edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
	}
	
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
			ResultOutput.writeTextFile(CDCR.outputFileName, "begin to process topic" + topic + "................");
			try {
				for (ScoreType type : ScoreType.values()) {
					CorefSystem cs = new CorefSystem();
					Document document = cs.getDocument(topic);
					ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Gold Mentions of " + topic + " : " + document.allGoldMentions.size());
					ResultOutput.writeTextFile(CDCR.outputFileName, "Number of gold clusters of " + topic + " : " + document.goldCorefClusters.size());
					cs.corefSystem.coref(document);
					ResultOutput.writeTextFile(CDCR.outputFileName, "Number of Clusters After Stanford's System Preprocess of " + topic + " : " + document.corefClusters.size());
					ResultOutput.writeTextFile(CDCR.outputFileName, "Stanford System merges " + (document.allGoldMentions.size() - document.corefClusters.size()) + " mentions");
					System.out.println(type.toString());
					ResultOutput.writeTextFile(CDCR.outputFileName, type.toString());
					String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
					String prefix = "CROSS-" + mExpasion + "-" + mBeamWidth + "-" + type.toString() + "-" + topic + "-";
					prefix = prefix.concat(timeStamp);
					scoreOutputPath = edu.oregonstate.util.Constants.TEMPORY_RESULT_PATH + prefix;
					ResultOutput.deleteResult(edu.oregonstate.util.Constants.ADJACENT_INTERMEDIATE_RESULT_PATH);
					update(document, type);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			ResultOutput.writeTextFile(CDCR.outputFileName, "end to process topic" + topic + "................");
		}

		return model;
	}
	
}
