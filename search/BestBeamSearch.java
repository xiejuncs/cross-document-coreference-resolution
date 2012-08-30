package edu.oregonstate.search;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import Jama.Matrix;
import edu.oregonstate.features.Feature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.Counter;

/**
 * Implementation of Best First version Beam Search
 * 
 * In this case, we aim for a general Best Beam Search.
 * <p>
 * It does not matter what heuristic function and data structure will be used
 * 
 * At first, we implement the restricted case, just for mention. But we still need to implement the split action. Because
 * it is very important to maintain the singleton mention across the whole process.
 * 
 * It does not matter with the pronoun, because we do not process it until the third phase.
 * 
 * For cluster case, it is very hard, I need to think about for a while
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class BestBeamSearch {
	private List<CorefCluster> mClusters;
	private Document mDocument;
	private Dictionaries mDictionary;
	private Matrix mModel;
	private Set<State<CorefCluster>> closedList;
	private Set<State<CorefCluster>> beam;
	private State<CorefCluster> initialState;
	private State<CorefCluster> goalState;
	private int mBeanWidth;

	/**
	 * initialize the related fields.
	 * 
	 * @param document
	 * @param dictionary
	 * @param model
	 */
	public BestBeamSearch(Document document, Dictionaries dictionary, Matrix model, int beamWidth) {
		mDocument = document;
		mDictionary = dictionary;
		mModel = model;
		mClusters = new ArrayList<CorefCluster>();
		initialize();
		closedList = new HashSet<State<CorefCluster>>();
		beam = new HashSet<State<CorefCluster>>();
		mBeanWidth = beamWidth;
	}
	
	/** initialize the clusters */
	private void initialize() {
		for (Integer key : mDocument.corefClusters.keySet()) {
			CorefCluster cluster = mDocument.corefClusters.get(key);
			mClusters.add(cluster);
			initialState.add(cluster);
		}
		
		for (Integer key : mDocument.goldCorefClusters.keySet()) {
			CorefCluster cluster = mDocument.goldCorefClusters.get(key);
			goalState.add(cluster);
		}
	}
	
	// in oder to calculate the cost function
	private double calculateScore(Counter<String> features) {
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
	
	public Double calculate(State<CorefCluster> state) {
		Double sum = 0.0;
		
		return sum;
	}
	
	// train the model using 
	// In this case, the cost function is a linear combination of features
	// g = w^{T} * \phi(x, n)  
	public void train() {
		closedList.add(initialState);
		beam.add(initialState);
		
		while(beam.size() != 0) {
			Set<State<CorefCluster>> set = new HashSet<State<CorefCluster>>();
			Map<State<CorefCluster>, Double> values = new HashMap<State<CorefCluster>, Double>();
			for (State<CorefCluster> state : beam) {
				values.put(state, calculate(state));
				set.add(state);
			}
			
			State<CorefCluster> index = compare_hashMap_min(values);
			for (Integer neighbor : adj(index)) { // consider every pair of cluster, it can be any cluster pair
				if (neighbor == goalState)
					return;
				set.put(neighbor);
			}
			
			set.remove(index);
			
			beam = new HashSet<State<CorefCluster>>();
			
			while ((set.size() != 0 ) && (beamWidth > beam.size())) {
                HashMap<Integer, Integer> heuristicValue = new HashMap<Integer, Integer>();
                for (Integer key : set.keySet()) {
                        heuristicValue.put(key, heuristic[key]);
                }
                Integer minIndex = compare_hashMap_min(heuristicValue);
                Iterator<Integer> keys = set.keySet().iterator();
                while(keys.hasNext()) {
                        Integer key = keys.next();
                        if (key == minIndex) keys.remove();
                }

                if (!closedList.containsKey(minIndex)) {
                        closedList.put(minIndex, set.get(minIndex));
                        beam.put(minIndex, set.get(minIndex));
                }
			}
		}
		
	}
	
}
