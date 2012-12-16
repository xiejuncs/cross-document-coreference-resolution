package edu.oregonstate.classifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.general.PriorityQueue;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.Counter;

public class StructuredPerceptronConsideringBeamHaltMultiLabel implements IClassifier {

	// the right links according to the gold corpus
	private Set<String> mrightLinks;

	/** each state with its loss function according to max-heap, the first one is the state with the highest score */
	private PriorityQueue<State<CorefCluster>> mstatesLossFunction;

	/** states */
	private Map<String, State<CorefCluster>> mstates;

	/** beam */
	private FixedSizePriorityQueue<State<CorefCluster>> mBeam;

	/** current best state */
	private State<CorefCluster> mBestState;

	/** previous best state */ 
	private State<CorefCluster> mpreviousBestState;

	// weight
	private double[] mWeight;

	/** the good or bad for each state */
	private boolean[] existsGood;

	// no of violations
	private int noOfVilotions;

	// totalWeight
	private double[] mTotalWeight;

	// search index, for output feature
	private int mSearchStep;

	/** return the number of violations */	
	public int getViolations() {
		return noOfVilotions;
	}

	public void setSearchStep(int searchStep) {
		mSearchStep = searchStep;
	}

	public void setTotalWeight(double[] weight) {
		mTotalWeight = weight;
	}

	public void setState(Map<String, State<CorefCluster>> states) {
		mstates = states;
	}

	public double[] getTotalWeight() {
		return mTotalWeight;
	}

	public double[] getWeight() {
		return mWeight;
	}

	public void setWeight(double[] weight) {
		mWeight = weight;
	}

	public void setRightLinks(Set<String> rightLinks){
		mrightLinks = rightLinks;
	}

	public void setStatesLossFunction(PriorityQueue<State<CorefCluster>> statesLossFunction) {
		mstatesLossFunction = statesLossFunction;
	}

	public void setBeam(FixedSizePriorityQueue<State<CorefCluster>> beam) {
		mBeam = beam;
	}

	public void setBestState(State<CorefCluster> bestState) {
		mBestState = bestState;
	}

	public void setPreviousBestState(State<CorefCluster> previousBestState) {
		mpreviousBestState = previousBestState;
	}

	public void setBestScore(double bestScore){

	}

	public StructuredPerceptronConsideringBeamHaltMultiLabel() {
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
	public void train() {
		noOfVilotions = 0;
		//			boolean exist = existGoodMerge();
		//			if (exist) {
		//				updateWeightForGoodCase();
		//				
		//			} else {
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "there do not exist good states ");
		List<State<CorefCluster>> beamLists = mBeam.getElements();
		for (State<CorefCluster> beam : beamLists) {
			mstates.remove(beam.getID());
		}

		if (beamLists.size() == 1) {
			updateWeightGreedyCase();
		} else {
			updateWeightBeamCase(beamLists);
		}

		// third constraint
		if (mBestState.getCostScore() <= mpreviousBestState.getCostScore()) {

			if (ExperimentConstructor.outputFeature) {
				outputFeatureFurther(beamLists.get(0), mBestState);
			}

			updateFature(mBestState, mpreviousBestState);
			noOfVilotions++;
		}
		//			}
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "No of violated constraints : " + noOfVilotions);
	}

	/**
	 * output feature for further analysis
	 * 
	 * @param goodState
	 * @param badState
	 */
	private void outputFeatureFurther(State<CorefCluster> goodState, State<CorefCluster> badState) {
		String filePath = ExperimentConstructor.currentExperimentFolder + "/" + mSearchStep;
		String features = outputFeature(goodState, badState);
		ResultOutput.writeTextFile(filePath, features);
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

				if (beamState.getScore()[0] == unBeamState.getScore()[0]) continue;

				if (beamState.getCostScore() <= unBeamState.getCostScore()) {
					if (ExperimentConstructor.outputFeature) {
						outputFeatureFurther(beamState, unBeamState);
					}

					updateFature(beamState, unBeamState);
					noOfVilotions++;
				}
			}
		}

		// second constraint
		for (int i = 1; i < beamLists.size(); i++) {
			State<CorefCluster> beamState = beamLists.get(i);

			if (beamLists.get(0).getScore()[0] == beamState.getScore()[0]) continue;

			if ( beamLists.get(0).getCostScore() <= beamState.getCostScore()) {
				if (ExperimentConstructor.outputFeature) {
					outputFeatureFurther(beamLists.get(0), beamState);
				}
				updateFature(beamLists.get(0), beamState);
				noOfVilotions++;
			}

		}
	}

	/** update in greedy case */
	private void updateWeightGreedyCase() {
		State<CorefCluster> haltState = mstates.get("HALT");
		double haltScore = haltState.getScore()[0];
		List<State<CorefCluster>> positiveStates = new ArrayList<State<CorefCluster>>();
		List<State<CorefCluster>> negativeStates = new ArrayList<State<CorefCluster>>();
		for (String id : mstates.keySet()) {
			if (!id.equals("HALT")) {
				State<CorefCluster> state = mstates.get(id);
				double stateScore = state.getScore()[0];
				if (stateScore >= haltScore) {
					positiveStates.add(state);
				} else {
					negativeStates.add(state);
				}
			}
		}

		// three constraints
		// positive should larger than halt
		for (State<CorefCluster> state : positiveStates) {
			if (state.getScore()[0] == haltScore) {
				continue;
			} else {
				if (state.getCostScore() <= haltState.getCostScore()) {
					updateFature(state, haltState);
				}
			}
		}

		// halt should larger than negative
		for (State<CorefCluster> state : negativeStates) {
			if (state.getScore()[0] == haltScore) {
				continue;
			} else {
				if (haltState.getCostScore() <= state.getCostScore()) {
					updateFature(haltState, state);
				}
			}
		}

		// positive should larger than negative
		for (int i = 0; i < positiveStates.size(); i++) {
			State<CorefCluster> positiveState = positiveStates.get(i);
			for (int j = 0; j < negativeStates.size(); j++) {
				State<CorefCluster> negativeState = negativeStates.get(j);

				if (positiveState.getCostScore() <= negativeState.getCostScore()) {
					updateFature(positiveState, negativeState);
				}
			}
		}
	}

	/** 
	 * according to the information we fill the state into good or bad
	 * @param exists
	 * @param good
	 * @param bad
	 */
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

	/**
	 * update features, good minus bad
	 * 
	 * @param good
	 * @param bad
	 */
	private void updateFature(State<CorefCluster> good, State<CorefCluster> bad) {
		Counter<String> goodFeature = good.getFeatures();
		Counter<String> badFeature = bad.getFeatures();
		double[] weightUpdate = new double[ExperimentConstructor.features.length];
		for (int i = 0; i < ExperimentConstructor.features.length; i++) {
			String feature = ExperimentConstructor.features[i];
			double goodValue = goodFeature.getCount(feature);
			double badValue = badFeature.getCount(feature);
			weightUpdate[i] = goodValue - badValue;
		}

		mWeight = DoubleOperation.add(mWeight, weightUpdate);
		mTotalWeight = DoubleOperation.add(mTotalWeight, mWeight);
	}

	/** if not all bad links, then just need to ensure that the good state should be higher than bad state */
	private void updateWeightForGoodCase() {
		// from existsGood, we can know which one is good, which one is bad
		List<State<CorefCluster>> goodStates = new ArrayList<State<CorefCluster>>();
		List<State<CorefCluster>> badStates = new ArrayList<State<CorefCluster>>();
		fillGoodBadLinks(existsGood, goodStates, badStates);

		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "there exists " + goodStates.size() + " good states ");
		for (int i = 0; i < goodStates.size(); i++) {
			for (int j = 0; j < badStates.size(); j++) {
				State<CorefCluster> goodState = goodStates.get(i);
				State<CorefCluster> badState = badStates.get(j);
				if (goodState.getCostScore() <= badState.getCostScore()) {

					// output feature in order for optimize
					if (ExperimentConstructor.outputFeature) {
						outputFeatureFurther(goodState, badState);
					}

					updateFature(goodState, badState);
					noOfVilotions++;
				}
			}
		}
	}


	private String outputFeature(State<CorefCluster> goodState, State<CorefCluster> badState) {
		StringBuffer sb = new StringBuffer();
		for (String feature : ExperimentConstructor.features) {
			double count = goodState.getFeatures().getCount(feature);
			sb.append(count + ",");
		}
		sb.append("-");
		for (String feature : ExperimentConstructor.features) {
			double count = badState.getFeatures().getCount(feature);
			sb.append(count + ",");
		}

		return sb.toString().trim();
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
