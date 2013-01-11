package edu.oregonstate.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.stanford.nlp.stats.Counter;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.io.LargeFileWriting;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;

/**
 * generate constraint
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ConstraintGeneration {

	/* output file */
	private final String mPath;
	
	/* enable the constraint enforced between the previousBestState and currentBestState */
	private final boolean enablePreviousCurrentConstraint;
	
	/* enable the constraint enforced between the states in the beam */
	private final boolean enableBeamConstraint;
	
	/* enable the constraint enforced between the states in the beam and not in the beam */
	private final boolean enableBeamUnBeamConstraint;
	
	public ConstraintGeneration(String path) {
		mPath = path;
		Properties props = ExperimentConstructor.experimentProps;
		enablePreviousCurrentConstraint = Boolean.parseBoolean(props.getProperty( EecbConstants.ENABLEPREVIOUSCCURRENTCCONSTRAINT_PROP, "false"));
		enableBeamConstraint = Boolean.parseBoolean(props.getProperty(EecbConstants.ENABLEBEAMCONSTRAINT_PROP, "true"));
		enableBeamUnBeamConstraint = Boolean.parseBoolean(props.getProperty(EecbConstants.ENABLEBEAMUNBEAMCONSTRAINT_PROP, "true"));
	}
	
	/**
	 * generate constraint
	 * 
	 * @param states
	 * @param previousBestState
	 * @param currentBestState
	 */
	public void generateConstraints(Map<String, State<CorefCluster>> states, FixedSizePriorityQueue<State<CorefCluster>> beam,
			State<CorefCluster> previousBestState, State<CorefCluster> currentBestState) {
		List<String> constraints = new ArrayList<String>();
        
		// remove the state in the beam from the states
		List<State<CorefCluster>> beamLists = beam.getElements();
		for (State<CorefCluster> state : beamLists) {
        	states.remove(state.getID());
        }
		
		// beam constraint
		if (enableBeamConstraint && (beamLists.size() > 1)) {
			List<String> violatedConstraints = generateBeamConstraint(beamLists);
			constraints.addAll(violatedConstraints);
		}
		
		// beam and unbeam constraint
		if (enableBeamUnBeamConstraint) {
			List<String> violatedConstraints = generateBeamUnBeamConstraint(beamLists, states);
			constraints.addAll(violatedConstraints);
		}
		
		// previous and current constraint
		if (enablePreviousCurrentConstraint) {
			String constraint = generateConstraint(currentBestState, previousBestState);
			constraints.add(constraint);
		}
		
		// write constraint
		writeConstraints(constraints, false);
	}
	
	/**
	 * write constraint to file
	 * 
	 * @param constraints
	 * @param delete
	 */
	public void writeConstraints(List<String> constraints, boolean delete) {
		LargeFileWriting writer = new LargeFileWriting(mPath);
		writer.writeArrays(constraints);
	}
	
	/**
	 * generateBeamConstraint
	 * 
	 * @param beamLists
	 * @return
	 */
	private List<String> generateBeamConstraint(List<State<CorefCluster>> beamLists) {
		List<String> constraints = new ArrayList<String>();

		State<CorefCluster> bestState = beamLists.get(0);
		for (int i = 1; i < beamLists.size(); i++) {
			State<CorefCluster> state = beamLists.get(i);
			String constraint = generateConstraint(bestState, state);
			if (!constraint.equals("")) {
				constraints.add(constraint);
			}
		}
		
		return constraints;
	}
	
	/**
	 * generateBeamUnBeamConstraint
	 * 
	 * @param beamLists
	 * @param states
	 * @return
	 */
	private List<String> generateBeamUnBeamConstraint(List<State<CorefCluster>> beamLists, Map<String, State<CorefCluster>> states) {
		List<String> constraints = new ArrayList<String>();
		
		for (State<CorefCluster> goodState : beamLists) {
			for (String stateID : states.keySet()) {
				State<CorefCluster> badState = states.get(stateID);
				String constraint = generateConstraint(goodState, badState);
				if (!constraint.equals("")) {
					constraints.add(constraint);
				}
			}
		}

		return constraints;
	}
	
	/**
	 * generateConstraint
	 * 
	 * @param goodState
	 * @param badState
	 * @return
	 */
	private String generateConstraint(State<CorefCluster> goodState, State<CorefCluster> badState) {
		double goodStateLossScore = goodState.getScore()[0];
		double goodStateCostScore = goodState.getCostScore();
		
		double badStateLossScore = badState.getScore()[0];
		double badStateCostScore = badState.getCostScore();
		String constraint = "";
		
		// if their scores are equal, do not update
		if (goodStateLossScore != badStateLossScore) {
			// violated constraint
			if (goodStateCostScore <= badStateCostScore) {
				constraint = generateViolatedConstraint(goodState, badState);
			}
		}
		
		return constraint;
	}
	
	/**
	 * generate violated constraint
	 * 
	 * @param goodState
	 * @param badState
	 * @return
	 */
	public String generateViolatedConstraint(State<CorefCluster> goodState, State<CorefCluster> badState) {
		Counter<String> goodFeatures = goodState.getFeatures();
		Counter<String> badFeatures = badState.getFeatures();
		
		String[] featureTemplate = FeatureFactory.getFeatures();
		StringBuffer sb = new StringBuffer();
		for (String feature : featureTemplate) {
			double count = goodFeatures.getCount(feature);
			sb.append(count + ",");
		}
		sb.append("-");
		for (String feature : featureTemplate) {
			double count = badFeatures.getCount(feature);
			sb.append(count + ",");
		}
		
		return sb.toString().trim();
	}

}
