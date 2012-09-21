package edu.oregonstate.training;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import Jama.Matrix;
import edu.oregonstate.CRC_MAIN;
import edu.oregonstate.CorefSystem;
import edu.oregonstate.features.Feature;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.State;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
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

	public TrainHeuristicFunction(int expansion, String[] topic, int beamWidth) {
		mExpasion = expansion;
		mTopic = topic;
		mBeamWidth = beamWidth;
		Train.currentOutputFileName = "TrainHeuristicFunction";
	}
	
	/**
	 * add model to the averageModel
	 * <b>NOTE</b>
	 * model and averageModel are both column vector
	 * 
	 * @param model
	 * @param averageModel
	 * @return
	 */
	public Matrix addWeight(Matrix model, Matrix averageModel) {
		for (int i = 0; i < averageModel.getRowDimension(); i++) {
			double updateValue = averageModel.get(i, 0) + model.get(i, 0);
			averageModel.set(i, 0,  updateValue);
		}
		
		return averageModel;
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
	private Double calculate(Document document) {
		Double sum = 0.0;
		CorefScorer score = new ScorerMUC();
    	score.calculateScore(document);
    	sum = score.getF1();
		return (1 - sum);
	}
	
	// create features for each state
	private Counter<String> getFeatures(State<CorefCluster> state) {
		Counter<String> features = new ClassicCounter<String>();
		return features;
	}
		
	// get the largest value
	public static Integer compare_hashMap_min(Map<Integer, Double> scores) {
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
	private Map<Integer, State<CorefCluster>> adj(State<CorefCluster> index) {
		Map<Integer, State<CorefCluster>> neighbors = new HashMap<Integer, State<CorefCluster>>();
		Map<Integer, CorefCluster> clusters = index.getState();
		List<Integer> ids = new ArrayList<Integer>();
		for (Integer key : clusters.keySet()) {
			ids.add(key);
		}
		int offset = 3;
		int size = ids.size();
		// merge
		for (int i = 0; i < (size - 1); i++) {
			for (int j = 0; j < i; j++) {
				CorefCluster icluster = clusters.get(ids.get(i));
				CorefCluster jcluster = clusters.get(ids.get(j));
				serializeCorefCluster(icluster);
            	CorefCluster cicluster = deserializeCorefCluster(true);
            	mergeClusters(cicluster, jcluster);
				//State<CorefCluster> newindex = deserializeCorefCluster(false);
				State<CorefCluster> newindex = new State<CorefCluster>();
				newindex.add(cicluster.clusterID, cicluster);
				for (Integer id : clusters.keySet()) {
					CorefCluster cluster = clusters.get(id);
					if (!id.equals(ids.get(i)) && !id.equals(ids.get(i))) {
						newindex.add(id, cluster);
					}
				}
				
				neighbors.put(offset, newindex);
				offset += 1;
			}
		}
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
		
		return neighbors;
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
	
	public <T> void serializeCorefCluster(T object) {
		try
	      {
	         FileOutputStream fileOut = new FileOutputStream(GlobalConstantVariables.RESULT_PATH + "object");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(object);
	         out.close();
	         fileOut.close();
	      }catch(IOException i)
	      {
	          i.printStackTrace();
	      }
	}
	
	@SuppressWarnings("unchecked")
	public <T> T deserializeCorefCluster(boolean delete) {
		T cluster = null;
		try
        {
           FileInputStream fileIn = new FileInputStream(GlobalConstantVariables.RESULT_PATH + "object");
           ObjectInputStream in = new ObjectInputStream(fileIn);
           cluster = (T) in.readObject();
           in.close();
           fileIn.close();
       }catch(IOException i) {
           i.printStackTrace(); 
       }catch(ClassNotFoundException c)
       {
           c.printStackTrace();
           System.exit(1);
       }
       if (delete) {
    	   CRC_MAIN.deleteResult(GlobalConstantVariables.RESULT_PATH);
       }
       return cluster;
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
			serializeCorefCluster(cluster);
			CorefCluster cpCluster = deserializeCorefCluster(true);
			initialState.add(key, cpCluster);
		}
		
		for (Integer key : document.goldCorefClusters.keySet()) {
			CorefCluster cluster = document.goldCorefClusters.get(key);
			serializeCorefCluster(cluster);
			CorefCluster cpCluster = deserializeCorefCluster(true);
			goalState.add(key, cpCluster);
		}
	}
	
	public void setNextDocument(Document documentState, State<CorefCluster> state) {
		documentState.corefClusters = state.getState();
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
	
	// beam search
	public void update(Document document) {
		Map<Integer, State<CorefCluster>> closedList = new HashMap<Integer, State<CorefCluster>>();
		Map<Integer, State<CorefCluster>> beam = new HashMap<Integer, State<CorefCluster>>();
		State<CorefCluster> initialState = new State<CorefCluster>();
		State<CorefCluster> goalState = new State<CorefCluster>();
		initialize(document, initialState, goalState);
		goalState.setGoal(true);
		closedList.put(1, initialState);
		beam.put(1, initialState);
		boolean breakWhile = false;
		int i = 0;
		while(beam.size() != 0) {
			Map<Integer, State<CorefCluster>> set = new HashMap<Integer, State<CorefCluster>>();
			Map<Integer, Double> values = new HashMap<Integer, Double>();
			for (Integer id : beam.keySet()) {
				State<CorefCluster> state = beam.get(id);
				Document documentState = new Document();
            	add(document, documentState);
            	setNextDocument(documentState, state);
				values.put(id, calculate(documentState));
				set.put(id, state);
			}
			Integer index = compare_hashMap_min(values);
			State<CorefCluster> indexState = set.get(index);
			System.out.println("action " + i);
			
			Document documentStateState = new Document();
			add(document, documentStateState);
			setNextDocument(documentStateState,indexState);
			
			CorefScorer scoreB3 = new ScorerBCubed(BCubedType.Bconll);
			scoreB3.calculateScore(documentStateState);
			scoreB3.printF1(logger, true);
	    	
	    	CorefScorer ceafscore = new ScorerCEAF();
	    	ceafscore.calculateScore(documentStateState);
	    	ceafscore.printF1(logger, true);

	    	CorefScorer mucscore = new ScorerMUC();
	    	mucscore.calculateScore(documentStateState);
	    	mucscore.printF1(logger, true);
	    	
	    	CorefScorer pairscore = new ScorerPairwise();
	    	pairscore.calculateScore(documentStateState);
	    	pairscore.printF1(logger, true);
	    	//double conllF11 = ( ceafscore.getF1() + mucscore.getF1()) / 3;
	    	double conllF11 = (scoreB3.getF1() + ceafscore.getF1() + mucscore.getF1()) / 3;
	    	System.out.println("conllF1:     " + conllF11 );
			
			Map<Integer, State<CorefCluster>> adjacent = adj(indexState);
			
			for (Integer id : adjacent.keySet()) { // consider every pair of cluster, it can be any cluster pair
				State<CorefCluster> neighbor = adjacent.get(id);
				Document documentState = new Document();
            	add(document, documentState);
            	setNextDocument(documentState, neighbor);
            	double score = calculate(documentState);
				if (score == 1.0) {
					breakWhile = true;
					break;
				}
					
				set.put(id, neighbor);
			}
			set.remove(index);
			beam = new HashMap<Integer, State<CorefCluster>>();
			while ((set.size() != 0 ) && (mBeamWidth > beam.size())) {
                Map<Integer, Double> heuristicValue = new HashMap<Integer, Double>();
                for (Integer id : set.keySet()) {
                	State<CorefCluster> key = set.get(id);
                	Document documentState = new Document();
                	add(document, documentState);
    				setNextDocument(documentState, key);
                	Double value = calculate(documentState);
                    heuristicValue.put(id, value);
                }
                Integer minIndex = compare_hashMap_min(heuristicValue);
                State<CorefCluster> state = set.get(minIndex);
                if (!detectDuplicate(closedList, state)) {
                        closedList.put(minIndex, state);
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
	
	private boolean detectDuplicate(Map<Integer, State<CorefCluster>> closedList, State<CorefCluster> index) {
		boolean duplciate = false;
		ScorerCEAF score = new ScorerCEAF();
		for (Integer key : closedList.keySet()) {
			State<CorefCluster> visited = closedList.get(key);
			Map<Integer, CorefCluster> visitedCorefClusters = visited.getState();
			Map<Integer, CorefCluster> indexCorefClusters = index.getState();
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
	
	/**
	 * divide the the averageModel by the number of mEpoch * number of topic
	 * 
	 * @param averageModel
	 * @param mEpoch
	 * @return
	 */
	public Matrix divide(Matrix averageModel, int mEpoch) {
		for (int i = 0; i < averageModel.getRowDimension(); i++) {
			double updateValue = averageModel.get(i, 0) / mEpoch;
			averageModel.set(i, 0, updateValue);
		}
		
		return averageModel;
	}
	
	// train the model
	// at first, collect the data first, and then use Perceptron to train the model
	public Matrix train() {
		Matrix model = mInitialModel;
		for (String topic : mTopic) {
			System.out.println("Heuristic Function begin to process topic " + topic+ "................");
			try {
				CorefSystem cs = new CorefSystem();
				Document document = cs.getDocument(topic);
				cs.corefSystem.coref(document);

				update(document);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Heuristic Function end to process topic " + topic+ "................");
		}

		return model;
	}
	
}
