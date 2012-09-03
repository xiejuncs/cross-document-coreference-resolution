package edu.oregonstate.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import com.rits.cloning.Cloner;

import Jama.Matrix;
import edu.oregonstate.CorefSystem;
import edu.oregonstate.features.Feature;
import edu.oregonstate.training.Train;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.stats.ClassicCounter;
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
		Train.currentOutputFileName = "TrainHeuristicFunction";
	}
	
	/** initialize the clusters */
	private void initialize() {
		for (Integer key : mDocument.corefClusters.keySet()) {
			CorefCluster cluster = mDocument.corefClusters.get(key);
			mClusters.add(cluster);
			initialState.add(cluster);
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
	
	private Double calculate(State<CorefCluster> state) {
		Double sum = 0.0;
		Counter<String> features = getFeatures(state);
		sum = calculateScore(features);
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
	
	// given the learned weight, for each initial state,reach the output node
	public void search() {
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
			for (State<CorefCluster> neighbor : adj(index)) { // consider every pair of cluster, it can be any cluster pair
				set.add(neighbor);
			}
			set.remove(index);
			beam = new HashSet<State<CorefCluster>>();
			while ((set.size() != 0 ) && (mBeanWidth > beam.size())) {
                Map<State<CorefCluster>, Double> heuristicValue = new HashMap<State<CorefCluster>, Double>();
                for (State<CorefCluster> key : set) {
                	Double value = calculate(key);
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
		}
	}
	
}
