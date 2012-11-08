package edu.oregonstate.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Jama.Matrix;

import edu.oregonstate.CDCR;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.general.PriorityQueue;
import edu.oregonstate.io.ResultOutput;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.ie.ClassifierCombiner;
import edu.stanford.nlp.stats.Counter;
import edu.oregonstate.search.State;

/**
 * define search constraint here
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class StructuredPerceptron implements Classifier {

	// the right links according to the gold corpus
	private Set<String> mrightLinks;
	
	/** each state with its loss function according to max-heap, the first one is the state with the highest score */
	private PriorityQueue<State<CorefCluster>> mstatesLossFunction;
	
	/** states */
	private Map<String, State<CorefCluster>> mstates;
	
	/** beam */
	private FixedSizePriorityQueue<State<CorefCluster>> mBeam;
	
	/** best beam */
	private State<CorefCluster> mBestState;
	
	private Matrix initialWeight;
	
	/** the good or bad for each state */
	private boolean[] existsGood;
	
	private int noOfVilotions;

	/** return the number of violations */	
	public int getViolations() {
		return noOfVilotions;
	}
	
	private Matrix averageWeight;
	
	public void setAverageWeight(Matrix weight) {
    	averageWeight = weight;
    }
    
    public Matrix getAverageWeight() {
    	return averageWeight;
    }
	
	public Matrix getInitialWeight() {
		return initialWeight;
	}
	
	public void setInitialWeight(Matrix weight) {
		initialWeight = weight;
	}
	
	public StructuredPerceptron(Set<String> rightLink, PriorityQueue<State<CorefCluster>> statesLossFunction, FixedSizePriorityQueue<State<CorefCluster>> beam, 
			Map<String, State<CorefCluster>> states,State<CorefCluster> bestState) {
		mrightLinks = rightLink;
		mstatesLossFunction = statesLossFunction;
		mBeam = beam;
		mBestState = bestState;
		mstates = states;
		
		int dimension = Feature.featuresName.length;
		initialWeight = new Matrix(dimension, 1, 0);
	}
	
	/** test whether the candidate states exist good links*/
	private boolean existGoodMerge() {
		existsGood = existGoodMerge(mstatesLossFunction);
		boolean exist = false;
		for (boolean exi : existsGood) {
			if (exi == true) {
				exist = true;
				break;
			}
		}
		return exist;
	}
	
	/** 
	 * update the weight according to different constraint
	 * rank them first, go through whether they introduce bad links
	 * 1 all introduce bad links, just follow the strategy
	 * 2 if not all bad links, then just need to ensure that the good state should be higher than bad state
	 */
	public void updateWeight() {
		noOfVilotions = 0;
		boolean exist = existGoodMerge();
		if (exist) {
			updateWeightForGoodCase();
			
		} else {
			ResultOutput.writeTextFile(CDCR.outputFileName, "there do not exist good states ");
			List<State<CorefCluster>> beamLists = mBeam.getElements();
			for (State<CorefCluster> beam : beamLists) {
				mstates.remove(beam.getID());
			}
			
			if (beamLists.size() == 1) {
				updateWeightGreedyCase(beamLists);
			} else {
				updateWeightBeamCase(beamLists);
			}
			
			// third constraint
			if (beamLists.get(0).getCostScore() <= mBestState.getCostScore()) {
				updateFature(beamLists.get(0), mBestState);
			}
		}
		ResultOutput.writeTextFile(CDCR.outputFileName, "No of violated constraints : " + noOfVilotions);
	}
	
	/** update in beam case*/ 
	private void updateWeightBeamCase(List<State<CorefCluster>> beamLists) {
		List<State<CorefCluster>> unBeamLists = new ArrayList<State<CorefCluster>>();
		for (String id : mstates.keySet()) {
			unBeamLists.add(mstates.get(id));
		}
		// first constraint: 
		for (int i = 0; i < beamLists.size(); i++) {
			State<CorefCluster> beamState = beamLists.get(i);
			for (int j = 0; j < unBeamLists.size(); j++) {
				State<CorefCluster> unBeamState = unBeamLists.get(j);
				if (beamState.getCostScore() <= unBeamState.getCostScore()) {
					updateFature(beamState, unBeamState);
				}
			}
		}
		
		// second constraint
		for (int i = 1; i < beamLists.size(); i++) {
			State<CorefCluster> beamState = beamLists.get(i);
			if (beamState.getCostScore() >= beamLists.get(0).getCostScore()) {
				updateFature(beamLists.get(0), beamState);
			}

		}
	}
	
	/** update in greedy case */
	private void updateWeightGreedyCase(List<State<CorefCluster>> beamLists) {
		for (String id : mstates.keySet()) {			
			State<CorefCluster> state = mstates.get(id);
			if (state.getCostScore() >= beamLists.get(0).getCostScore()) {
				updateFature(beamLists.get(0), state);
				noOfVilotions++;
			}
		}
	}
	
	/** according to the information we fill the state into good or bad */
	private void fillGoodBadLinks(boolean[] exists, List<State<CorefCluster>> good, List<State<CorefCluster>> bad) {
		for (int i = 0; i < mstatesLossFunction.size(); i++) {
			State<CorefCluster> state = mstatesLossFunction.getElements().get(i);
			if (exists[i]) {
				good.add(state);
			} else {
				bad.add(state);
			}
		}
	}
	
	private void updateFature(State<CorefCluster> good, State<CorefCluster> bad) {
		Counter<String> goodFeature = good.getFeatures();
		Counter<String> badFeature = bad.getFeatures();
		int featureIndex = 0;
		Matrix weightUpdate = new Matrix(Feature.featuresName.length, 1);
		for (String feature : Feature.featuresName) {
			weightUpdate.set(featureIndex, 0, goodFeature.getCount(feature) - badFeature.getCount(feature));
			featureIndex++;
		}
		initialWeight = initialWeight.plus(weightUpdate);
		averageWeight = averageWeight.plus(initialWeight);
	}
	
	/** if not all bad links, then just need to ensure that the good state should be higher than bad state */
	private void updateWeightForGoodCase() {
		// from existsGood, we can know which one is good, which one is bad
		List<State<CorefCluster>> goodStates = new ArrayList<State<CorefCluster>>();
		List<State<CorefCluster>> badStates = new ArrayList<State<CorefCluster>>();
		fillGoodBadLinks(existsGood, goodStates, badStates);
		
		ResultOutput.writeTextFile(CDCR.outputFileName, "there exists " + goodStates.size() + " good states ");
		for (int i = 0; i < goodStates.size(); i++) {
			for (int j = 0; j < badStates.size(); j++) {
				State<CorefCluster> goodState = goodStates.get(i);
				State<CorefCluster> badState = badStates.get(j);
				if (goodState.getCostScore() <= badState.getCostScore()) {
					updateFature(goodState, badState);
					noOfVilotions++;
				}
			}
		}
	}
	
	
	/** test whether there exists good links */
	private boolean[] existGoodMerge(PriorityQueue<State<CorefCluster>> statesLossFunction){
		boolean[] exists = new boolean[statesLossFunction.size()];
		for (int i = 0; i < statesLossFunction.getElements().size(); i++) {
			State<CorefCluster> state = statesLossFunction.getElements().get(i);
			boolean existsGood = true;
			Map<Integer, CorefCluster> clusters = state.getState();
			for (CorefCluster cluster : clusters.values()) {
				int size = cluster.getCorefMentions().size();
				if (size == 1) continue;
				
				boolean existedOuterFor = false;
				List<Integer> ids = new ArrayList<Integer>();
	    		for (Mention mention : cluster.getCorefMentions()) {
	    			ids.add(mention.mentionID);
	    		}
	    		
	    		for (int k = 0; k < ids.size(); k++) {
	                int iID = ids.get(k);
	                boolean existInnerFor = false;
	                for (int j = 0; j < k; j++) {
	                	int jID = ids.get(j);
	                	boolean rightcontians = mrightLinks.contains(iID + "-" + jID);
	                	boolean reverserightcontians = mrightLinks.contains(jID + "-" + iID);
	                	
	                	if (!(rightcontians || reverserightcontians)) {
	                		existsGood = false;
	                		existInnerFor = true;
	                		break;
	                	}
	                }
	                if (existInnerFor) {
	                	existedOuterFor = true;
	                	break;
	                }
	    		}
	    		
	    		if (existedOuterFor) {
	    			break;
	    		}
			}
			exists[i] = existsGood;	
		}
		
		return exists;
	}
	
}
