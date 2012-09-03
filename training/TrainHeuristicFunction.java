package edu.oregonstate.training;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.rits.cloning.Cloner;

import Jama.Matrix;
import edu.oregonstate.CorefSystem;
import edu.oregonstate.features.Feature;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.objectbank.ResettableReaderIteratorFactory;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * train heuristic function for beam search
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainHeuristicFunction {
	
	private int mIteration;
	// topic in the corpus
	private String[] mTopic;
	private Matrix mInitialModel;
	private int mBeanWidth;
	private boolean mErrorType; // true conversative error, false agressive error
	
	public TrainHeuristicFunction(int iteration, String[] topic, Matrix initialModel, int beamWidth, boolean errorType) {
		mIteration = iteration;
		mTopic = topic;
		mInitialModel = initialModel;
		mBeanWidth = beamWidth;
		mErrorType = errorType;
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
	
	private Double calculate(State<CorefCluster> state, Matrix model) {
		Double sum = 0.0;
		Counter<String> features = getFeatures(state);
		sum = calculateScore(features, model);
		return sum;
	}
	
	// create features for each state
	private Counter<String> getFeatures(State<CorefCluster> state) {
		Counter<String> features = new ClassicCounter<String>();
		return features;
	}
		
	// get the largest value
	public static State<CorefCluster> compare_hashMap_min(Map<State<CorefCluster>, Double> scores) {
        Collection<Double> c = scores.values();
        Double minvalue = Collections.min(c);
        Set<State<CorefCluster>> scores_set = scores.keySet();
        Iterator<State<CorefCluster>> scores_it = scores_set.iterator();
        while(scores_it.hasNext()) {
        		State<CorefCluster> id = scores_it.next();
                Double value = scores.get(id);
                if (value == minvalue) {
                        return id;
                }
        }
        return null;
	}
	
	// for each state, get its successor states
	private List<State<CorefCluster>> adj(State<CorefCluster> index) {
		List<State<CorefCluster>> neighbors = new ArrayList<State<CorefCluster>>();
		List<CorefCluster> clusters = index.getState();
		int size = clusters.size();
		for (int i = 0; i < (size - 1); i++) {
			for (int j = 0; j < i; j++) {
				Cloner cloner = new Cloner();
				State<CorefCluster> newindex = cloner.deepClone(index);
				mergeClusters(newindex.get(i), newindex.get(j));
				newindex.remove(j);
				neighbors.add(newindex);
			}
		}
		
		return neighbors;
	}
	
	
	
	private CorefCluster mergeClusters(CorefCluster to, CorefCluster from) {
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
	    SieveCoreferenceSystem.logger.finer("merge clusters: "+toID+" += "+from.clusterID);
	    
	    return to;
	}
	
	public void initialize(Document document, State<CorefCluster> initialState, State<CorefCluster> goalState) {
		for (Integer key : document.corefClusters.keySet()) {
			CorefCluster cluster = document.corefClusters.get(key);
			initialState.add(cluster);
		}
		
		for (Integer key : document.goldCorefClusters.keySet()) {
			CorefCluster cluster = document.goldCorefClusters.get(key);
			goalState.add(cluster);
		}
	}
	
	public Matrix update(Matrix model, Document document) {
		Set<State<CorefCluster>> closedList = new HashSet<State<CorefCluster>>();
		Set<State<CorefCluster>> beam = new HashSet<State<CorefCluster>>();
		State<CorefCluster> initialState = new State<CorefCluster>();
		State<CorefCluster> goalState = new State<CorefCluster>();
		initialize(document, initialState, goalState);
		
		closedList.add(initialState);
		beam.add(initialState);
		
		boolean breakWhile = false;
		while(beam.size() != 0) {
			Set<State<CorefCluster>> set = new HashSet<State<CorefCluster>>();
			Map<State<CorefCluster>, Double> values = new HashMap<State<CorefCluster>, Double>();
			for (State<CorefCluster> state : beam) {
				values.put(state, calculate(state, model));
				set.add(state);
			}
			State<CorefCluster> index = compare_hashMap_min(values);
			for (State<CorefCluster> neighbor : adj(index)) { // consider every pair of cluster, it can be any cluster pair
				if (neighbor.equals(goalState)) {
					breakWhile = true;
					break;
				}
					
				set.add(neighbor);
			}
			set.remove(index);
			beam = new HashSet<State<CorefCluster>>();
			while ((set.size() != 0 ) && (mBeanWidth > beam.size())) {
                Map<State<CorefCluster>, Double> heuristicValue = new HashMap<State<CorefCluster>, Double>();
                for (State<CorefCluster> key : set) {
                	Double value = calculate(key, model);
                    heuristicValue.put(key, value);
                }
                State<CorefCluster> minIndex = compare_hashMap_min(heuristicValue);
                Iterator<State<CorefCluster>> keys = set.iterator();
                while(keys.hasNext()) {
                	State<CorefCluster> key = keys.next();
                    if (key.equals(minIndex)) keys.remove();
                }

                if (!closedList.contains(minIndex)) {
                        closedList.add(minIndex);
                        beam.add(minIndex);
                }
			}
			
			boolean error = false;
			if (mErrorType) {
				error = detectConversativeError(beam, goalState);
				if (error) {
					for (State<CorefCluster> state : beam) {
						model = model.plus(getDifference(state, goalState));
					}
				}
				resetBeam(beam, goalState);
			} else {
				error = detectAgressiveError(beam, goalState);
				//  if any non-target node in our beam is ranked higher than a target node, then we declare a search error.
				Set<CorefCluster> errorCluster = new HashSet<CorefCluster>(); 
				if (error) {
					for (State<CorefCluster> state : beam) {
						model = model.plus(getDifference(state, goalState));
					}
				}
				resetBeam(beam, goalState);
			}
			
			if (breakWhile) {
				break;
			}
		}
		
		return model;
	}
	
	
	private void resetBeam(Set<State<CorefCluster>> beam, State<CorefCluster> goalState) {
		
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
	
	
	private boolean detectConversativeError(Set<State<CorefCluster>> beam, State<CorefCluster> goalState) {
		boolean error = false;
		
		
		return error;
	}
	
	private boolean detectAgressiveError(Set<State<CorefCluster>> beam, State<CorefCluster> goalState) {
		boolean error = false;
		
		
		return error;
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
	public Matrix train() {
		Matrix model = mInitialModel;
		Matrix averageModel = mInitialModel;
		for (int i = 0; i < mIteration; i++) {
			System.out.println("Start train the model:"+ i +"th iteration ============================================================");
			for (String topic : mTopic) {
				System.out.println("begin to process topic" + topic+ "................");
				try {
					CorefSystem cs = new CorefSystem();
					Document document = cs.getDocument(topic);
					cs.corefSystem.coref(document);
					
					model = update(model, document);
				    averageModel = addWeight(model, averageModel);
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				System.out.println("end to process topic" + topic+ "................");
			}
			System.out.println("End train the model:"+ i +"th iteration ============================================================");
		}

		return divide(averageModel, mIteration * mTopic.length);
	}
	
}
