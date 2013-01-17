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
	
	/**
	 * constructor function
	 * 
	 * @param path
	 */
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
		List<String> allConstraints = new ArrayList<String>();
        
		// remove the state in the beam from the states
		List<State<CorefCluster>> beamLists = beam.getElements();
		for (State<CorefCluster> state : beamLists) {
        	states.remove(state.getID());
        }
		
		// beam constraint
		if (enableBeamConstraint && (beamLists.size() > 1)) {
			List<String> constraints = generateBeamConstraint(beamLists);
			allConstraints.addAll(constraints);
		}
		
		// beam and unbeam constraint
		if (enableBeamUnBeamConstraint) {
			List<String> constraints = generateBeamUnBeamConstraint(beamLists, states);
			allConstraints.addAll(constraints);
		}
		
		// previous and current constraint
		if (enablePreviousCurrentConstraint) {
			List<String> constraint = generateConstraint(currentBestState, previousBestState);
			allConstraints.addAll(constraint);
		}
		
		// write constraint
		writeConstraints(allConstraints);
	}
	
	/**
	 * write constraint to file
	 * 
	 * @param constraints
	 */
	public void writeConstraints(List<String> constraints) {
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
		constraints.add(buildGoodConstraint(bestState));
		
		for (int i = 1; i < beamLists.size(); i++) {
			State<CorefCluster> state = beamLists.get(i);
			String constraint = buildBadConstraint(state);
			constraints.add(constraint);
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
		constraints.add("NEWDATASET");
		
		// good constraint
		for (State<CorefCluster> state : beamLists) {
			String constraint = buildGoodConstraint(state);
			constraints.add(constraint);
		}
		
		// bad constraint
		for (String stateID : states.keySet()) {
			State<CorefCluster> state = states.get(stateID);
			String constraint = buildBadConstraint(state);
			// This kind of data does not matter
			if (constraint.length() == 1) continue;
			constraints.add(constraint);
		}

		return constraints;
	}
	
	// build good constraint
	public String buildGoodConstraint(State<CorefCluster> state) {
		return ("G\t" + buildSparseConstraint(state).toString()).trim();
	}
	
	// build bad constraint
	public String buildBadConstraint(State<CorefCluster> state) {
		return ("B\t" + buildSparseConstraint(state).toString()).trim();
	}
	
	/**
	 * build sparse representation of the features, which means just record the nonzero value into the file
	 * 
	 * @param state
	 * @return
	 */
	private String buildSparseConstraint(State<CorefCluster> state) {
		String[] featureTemplate = FeatureFactory.getFeatures();
		StringBuilder sb = new StringBuilder();
		Counter<String> features = state.getFeatures();
		
		for (int i = 0; i < featureTemplate.length; i++) {
			String feature = featureTemplate[i];
			double value = features.getCount(feature);
			if (value != 0.0) {
				sb.append(i + ":" + value + "\t");
			}
		}
		
		return sb.toString().trim();
	}
	
	/**
	 * generateConstraint
	 * 
	 * @param goodState
	 * @param badState
	 * @return
	 */
	private List<String> generateConstraint(State<CorefCluster> goodState, State<CorefCluster> badState) {
		List<String> constraints = new ArrayList<String>();
		
		constraints.add(buildGoodConstraint(goodState));
		constraints.add(buildBadConstraint(badState));
		
		return constraints;
	}

}
