package edu.oregonstate.search;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.costfunction.ICostFunction;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.features.FeatureVectorGenerator;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.io.LargeFileWriting;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.lossfunction.ILossFunction;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

/**
 * rank them first, go through whether they introduce bad links
 * 1 all introduce bad links, just follow the strategy
 * 2 if not all bad links, then just need to ensure that the good state should be higher than bad state
 *
 * Implementation of Best First version Beam Search (not breadth first beam search)
 * 
 * In this case, we aim for a general Best Beam Search.
 * <p>
 * It does not matter what heuristic function and data structure will be used
 * 
 * At first, we implement the unrestricted case, just for mentions. But we still need to implement the split action. Because
 * it is very important to maintain the singleton mention across the whole process. Later time
 * 
 * Print the beam information and cost information in order to trace the rightness of the program
 * Feature should have function allows evaluation of the state, pass the state and return the global feature
 *  
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class BeamSearch implements ISearch {
    
	/** beam width */
    private final int mBeamWidth;
    
    /** search depth */
    private final int maximumSearch;
    
    /** loss function */
    private ILossFunction lossFunction;
    
    /** cost function */
    private ICostFunction costFunction;
    
    /** score type */
    private final ScoreType type;
    
    /* experiment properties */
    private final Properties mProps;
    
    /* log file path */
    private final String logFile;
    
    /* experiment result folder */
    private final String experimentResultFolder;
    
    /* debug mode */
    private final boolean mDebug;
    
    /* enable state feature or just action feature */
    private final boolean enableStateFeature;
    
    /** constructor */
    public BeamSearch() {
    	mProps = ExperimentConstructor.experimentProps;
    	logFile = ExperimentConstructor.logFile;
    	experimentResultFolder = ExperimentConstructor.experimentResultFolder;
    	
    	//
    	// set the configuration constant 
    	//
    	mBeamWidth = Integer.parseInt(mProps.getProperty(EecbConstants.SEARCH_BEAMWIDTH_PROP));
    	maximumSearch = Integer.parseInt(mProps.getProperty(EecbConstants.SEARCH_MAXIMUMSTEP_PROP));
    	type = CorefScorer.ScoreType.valueOf(mProps.getProperty(EecbConstants.LOSSFUNCTION_SCORE_PROP));
        lossFunction = EecbConstructor.createLossFunction(mProps.getProperty(EecbConstants.LOSSFUNCTION_PROP, "MetricLossFunction"));
        costFunction = EecbConstructor.createCostFunction(mProps.getProperty(EecbConstants.COSTFUNCTION_PROP, "LinearCostFunction"));
        ResultOutput.writeTextFile(logFile, "\nBeam Search Configuration : " + "width : " + mBeamWidth + "; maximumSearch : " + 
        							maximumSearch + "; type : " + type.toString() + "; lossFunction : " + 
        							mProps.getProperty(EecbConstants.LOSSFUNCTION_PROP) + "; costFunction : " + mProps.getProperty(EecbConstants.COSTFUNCTION_PROP));
        enableStateFeature = Boolean.parseBoolean(mProps.getProperty(EecbConstants.ENABLE_STATE_FEATURE, "false"));
        
        // debug mode
        //mDebug = Boolean.parseBoolean(mProps.getProperty(EecbConstants.DEBUG_PROP, "true"));
        mDebug = false;
    }
    
    /**
     * get the neighbors of the current state
     * 
     * do not deal with the pronoun case, because the pronoun is still the singleton cluster all the time
     * so pull out the cluster and determine whether the first mention of the cluster is Pronominal. If the 
     * first mention is Pronominal, then jump out of the loop
     * 
     * 
     * 
     * @param state
     * @return
     */
    private Set<String> generateCandidateSets(State<CorefCluster> state) {
        Set<String> actions = new HashSet<String>();
        
        // get the numbe of clusters
        Map<Integer, CorefCluster> clusters = state.getState();
        List<Integer> keys = new ArrayList<Integer>();
        for (Integer key : clusters.keySet()) {
            keys.add(key);
        }
        int size = keys.size();
        
        // generate the action
        ResultOutput.writeTextFile(logFile, "before create children: total of clusters : " + size);
        for (int i = 0; i < size; i++) {
            Integer iID = keys.get(i);
            CorefCluster icluster = clusters.get(iID);
            if (icluster.corefMentions.size() == 1 && icluster.firstMention.isPronominal()) {
            	continue;
            }
            
            for (int j = 0; j < i; j++) {
                Integer jID = keys.get(j);
                CorefCluster jcluster = clusters.get(jID);
                if (jcluster.corefMentions.size() == 1 && jcluster.firstMention.isPronominal()) {
                 	continue;
                }
                String action = iID + "-" + jID;
                actions.add(action);
            }
        }
        ResultOutput.writeTextFile(logFile, "after create children: total of clusters : " + (size - 1));
        
        // Add HALT action
        String stopping = mProps.getProperty(EecbConstants.STOPPING_CRITERION, "none");
		if (stopping.equals("halt")) {
			actions.add("HALT");
		}
        
		// print the total number of candidate sets
        ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the number of candidate sets : " + actions.size());
        return actions;
    }
    
    // merge two clusters
    private void mergeClusters(CorefCluster to, CorefCluster from) {
        int toID = to.clusterID;
        for (Mention m : from.corefMentions) {
            m.corefClusterID = toID;
        }
        
        if (Constants.SHARE_ATTRIBUTES) {
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
	 * initial state
	 * 
	 * @param document
	 */
    private State<CorefCluster> initialize(Document document){
    	State<CorefCluster> initialState = new State<CorefCluster>();
    	
        for (Integer key : document.corefClusters.keySet()) {
            CorefCluster cluster = document.corefClusters.get(key);
            CorefCluster cpCluster = new CorefCluster(key, cluster.getCorefMentions());
            initialState.add(key, cpCluster);
        }
        
        return initialState;
    }
    
    /**
     * align the document with the state
     * 
     * @param documentState
     * @param state
     */
    private void generateStateDocument(Document documentState, State<CorefCluster> state) {
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
    
    /** 
     * after choose the best state, update the document the chosen state,
     * and then regenerate features for each cluster
     * 
     * @param indexState
     */
    private void regenerateFeatures(Document document, State<CorefCluster> indexState) {
    	generateStateDocument(document, indexState);
    	for (Integer id : document.corefClusters.keySet()) {
    		CorefCluster cluster = document.corefClusters.get(id);
    		cluster.regenerateFeature();
    	}
    }
	
	/** 
	 * calculate cost score according to the weight and feature.
	 * The cost function model is set at the beginning of the experiment
	 * 
	 * @param initial
	 * @param action
	 * @return
	 */
	private void calculateCostScore(State<CorefCluster> initial, String action, Document document, double[] weight) {
		// update the document and generate new features
		
		String[] ids = action.split("-");
		Integer i_id = Integer.parseInt(ids[0]);
		Integer j_id = Integer.parseInt(ids[1]);
		CorefCluster iCluster = initial.getState().get(i_id);
		CorefCluster cpCluster = new CorefCluster(i_id, iCluster.getCorefMentions());
		CorefCluster jCluster = initial.getState().get(j_id);

		// generate features
		Counter<String> features = null;
		if (!enableStateFeature) {
			features = FeatureVectorGenerator.getFeatures(document, iCluster, jCluster);
		}
		
		// merge cluster
		mergeClusters(cpCluster, jCluster);
		initial.remove(i_id);
		initial.remove(j_id);
		initial.add(i_id, cpCluster);
		
		if (enableStateFeature) {
			//features = FeatureFactory.getFeatures(document, initial, dictionaries);
		}
		
		// calculate the cost function for the state
		double costScore = costFunction.calculateCostFunction(features, weight);
		
		// set the fields of the state
		String actionDescription = iCluster.toString() + " <-- " + jCluster.toString() + "\n" +
									ResultOutput.buildCounterFeatureString(iCluster.predictedCentroid) + "\n" +
									ResultOutput.buildCounterFeatureString(jCluster.predictedCentroid);
		initial.setID(action);
		initial.setActionDescription(actionDescription);
		initial.setFeatures(features);
		initial.setCostScore(costScore);
		initial.setToClusterPredictedCentroid(iCluster.predictedCentroid);
		initial.setFromClusterPredictedCentroid(jCluster.predictedCentroid);
	}
	
	/**
	 * 
	 * conduct beam search on specific document given specific true loss function
	 * In addition, we also need to define the beam search guided by the learned cost function
	 * 
	 * If the search is guided by the cost function, then there is no goal state specified, so we 
	 * do not need to define a function to check whether the current state is the goal state
	 * 
	 */
	public void trainingBySearch(Document document, Parameter para, String phaseID) {
		Double globalScore = 0.0;
		double[] weight = para.getWeight();
		String trainingDataPath = experimentResultFolder + "/" + document.getID() + "/data";
		
		// beam
		FixedSizePriorityQueue<State<CorefCluster>> beam = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		State<CorefCluster> initialState = initialize(document);
		double[] localScores = lossFunction.getMetricScore(document);
		initialState.setScore(localScores);
		beam.add(initialState, localScores[0]);
		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
		
		// the best output y^{*}_{i} uncovered so far evaluated by the loss function
		State<CorefCluster> bestState = new State<CorefCluster>();
		State<CorefCluster> previousBestState = new State<CorefCluster>();
		
		// keep search
		int msearchStep = 1;
		while (beam.size() != 0 && (msearchStep < maximumSearch)) {
			// the state with the highest score
			State<CorefCluster> state = beam.next();
			
			// debug information
			// ResultOutput.writeTextFile(logFile, state.featureString());
			ResultOutput.writeTextFile(logFile, "action " + msearchStep + " : " + state.getID() );
			ResultOutput.printScoreInformation(state.getScore(), type, logFile);
			ResultOutput.writeTextFile(logFile, "global " + type.toString() +" F1 score: " + globalScore);
			
			regenerateFeatures(document, state);
			// print the debug information
			if (mDebug) {
				//ResultOutput.printParameters(document, document.getID(), logFile);
				//ResultOutput.writeTextFile(logFile, "action name : " + state.getID());
				//ResultOutput.printClusterFeatures(document, logFile, msearchStep);
			}
			
			// generate actions and learn weights
			try {
				Set<String> actions = generateCandidateSets(state);
				Map<String, State<CorefCluster>> states = new HashMap<String, State<CorefCluster>>();
				
				for (String action : actions) {
					// make a copy of state
					State<CorefCluster> initial = new State<CorefCluster>();
					double[] stateScore = {globalScore, 0.0, 0.0};
					
					if (action.equals("HALT")) {
						// HALT action
						initial.setID("HALT");
						initial.setScore(stateScore);
						initial.setCostScore(weight[featureTemplate.size() - 1]);
						Counter<String> features = buildHaltFeature();
						initial.setFeatures(features);
					} else {
						// NOT HALT ACTION
						for (Integer key : state.getState().keySet()) {
							initial.add(key, state.getState().get(key));
						}

						calculateCostScore(initial, action, document, weight);
						stateScore = lossFunction.calculateLossFunction(document, initial);
						initial.setScore(stateScore);
					}
					
					beam.add(initial, initial.getScore()[0]);
					states.put(action, initial);
				}
				
				State<CorefCluster> stateinBeam = beam.peek();
				double priority = beam.getPriority();
				if (priority >= globalScore) {
					globalScore = priority;
					previousBestState = bestState;
					bestState = stateinBeam;
				}
				
				// stopping criterion
				if ((globalScore == 1.0) || (globalScore > priority) || (stateinBeam.getID().equals("HALT")) ) {
					ResultOutput.writeTextFile(logFile, "search stop with the loss score : " + globalScore);
					generateStateDocument(document, bestState);
					
					//ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.goldCorefClusters));
					break;
				}
				
				// output constraints
				int id = Integer.parseInt(phaseID);
				String path = trainingDataPath + "/" + id;
				ConstraintGeneration constraintGenerator = new ConstraintGeneration(path);
				constraintGenerator.generateConstraints(states, beam, previousBestState, bestState);
				
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			msearchStep++;
		}
	
	}
	
	// apply the weight to guide the search
	// In this case, we should choose which one to expand, the minimum cost score or the maximum cost score 
	// In addition, we need to decide in the training part, is the states and costs are the same, maybe they are not same
	// because I use a condition to add the state into the states and beam
	// In addition, I did not use closed list to keep track of the duplicate states, because it costs a lot of time
	// 
	// define termination condition for test search
	public State<CorefCluster> testingBySearch(Document document, double[] weight, String phaseID, boolean outputFeature, double stoppingRate) {
		String stopping = mProps.getProperty(EecbConstants.STOPPING_CRITERION);
		String mscorePath = experimentResultFolder + "/" + document.getID() + "/" + phaseID;
		String trainingDataPath = experimentResultFolder + "/" + document.getID() + "/data";
		String bestLossScorePath = experimentResultFolder + "/" + document.getID() + "/" + phaseID + "-bestlossscore";
		
		double globalCostScore = 0.0;
		double stopscore = 0.0;
		if (stoppingRate == 0.0) {
			stoppingRate = Double.parseDouble(mProps.getProperty(EecbConstants.STOPPING_RATE, "2"));
		}
		double bestLossScore = 0.0;        // in order to track the state with the highest loss score
		State<CorefCluster> bestLostState = new State<CorefCluster>();
		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
		
		// closed list to track duplicate method
		FixedSizePriorityQueue<State<CorefCluster>> beam = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		State<CorefCluster> initialState = initialize(document);
		double[] localScores = lossFunction.getMetricScore(document);
		initialState.setScore(localScores);
		beam.add(initialState, 0.0);
		State<CorefCluster> previousBestState = new State<CorefCluster>();
		
		// do search
		int msearchStep = 1;
		boolean stopSearch = false;
		while(beam.size() != 0 && (msearchStep < maximumSearch)) {
			// the state with the highest cost score and print its related information
			State<CorefCluster> state = beam.next();
			
			// generate new document state
			regenerateFeatures(document, state);
			
			// debug information
			ResultOutput.writeTextFile(logFile, state.getActionDescription());
			ResultOutput.writeTextFile(logFile, state.getID() + " : " + state.featureString());
			if (mDebug) {
				
				ResultOutput.writeTextFile(logFile, "\npredicted clusters : " + document.corefClusters.size() + "\n");
				ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.corefClusters));
				ResultOutput.writeTextFile(logFile, "\ngold clusters\n");
				ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(document.goldCorefClusters));
			}
			ResultOutput.writeTextFile(logFile, "action " + msearchStep);
			double[] scores = state.getScore();
			ResultOutput.printScoreInformation(scores, type, logFile);
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, type + " F1 score " + scores[0]);
			ResultOutput.writeTextFile(mscorePath, Double.toString(scores[0]) + " " + state.getCostScore());
			double localScore = state.getCostScore();
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, type.toString() +" Cost score: " + localScore);
			
			// print the debug information
			if (mDebug) {
				//ResultOutput.printParameters(document, document.getID(), logFile);
				//ResultOutput.writeTextFile(logFile, "action name : " + state.getID());
//				ResultOutput.writeTextFile(logFile, ResultOutput.printCluster(state.getState()));
				//ResultOutput.printClusterFeatures(document, logFile, msearchStep);
			}
			
			try {
				/** get the candidate lists*/
				Set<String> actions = generateCandidateSets(state);
				Map<String, State<CorefCluster>> states = new HashMap<String, State<CorefCluster>>();
				String localBestLossStateID = "";
				double localBestLossStateScore = 0.0;
				for (String action : actions) {
					State<CorefCluster> initial = new State<CorefCluster>();
					if (action.equals("HALT")) {
						initial.setID("HALT");
						initial.setCostScore(weight[featureTemplate.size() - 1]);
						Counter<String> features = buildHaltFeature();
						initial.setFeatures(features);
					} else {
						for (Integer key : state.getState().keySet()) {
							initial.add(key, state.getState().get(key));
						}

						calculateCostScore(initial, action, document, weight);

						/* the best loss score uncovered during search*/
						double[] stateScore = lossFunction.calculateLossFunction(document, initial);
						initial.setScore(stateScore);
						if (stateScore[0] > bestLossScore) {
							bestLostState = initial;
							bestLossScore = stateScore[0];
						}
						
						if (stateScore[0] > localBestLossStateScore) {
							localBestLossStateScore = stateScore[0];
							localBestLossStateID = action;
						}
					}
					
					// deal with tie
					// if violated the constraint, not add into the beam
					//boolean conformHardConstraint = satisfyHardConstraint(initial);
					//if (conformHardConstraint) {
					beam.add(initial, initial.getCostScore());
					//}
	            	states.put(action, initial);
				}
				
				if (beam.size() == 0) {
					break;
				}
				
				// print the local best state
				//if (mDebug) {
					State<CorefCluster> localBestState = states.get(localBestLossStateID);
					ResultOutput.writeTextFile(logFile, "\n best local state : \n");
					ResultOutput.writeTextFile(logFile, localBestState.getScore()[0] + " " + localBestState.getActionDescription());
					ResultOutput.writeTextFile(logFile, localBestState.getID() + " : " + localBestState.featureString() + " : " + localBestState.getCostScore());
					ResultOutput.writeTextFile(logFile, "\n");
					
					
					ResultOutput.writeTextFile(logFile, "\n best state : \n");
					ResultOutput.writeTextFile(logFile, bestLostState.getScore()[0] + " " + bestLostState.getActionDescription());
					ResultOutput.writeTextFile(logFile, bestLostState.getID() + " : " + bestLostState.featureString() + " : " + bestLostState.getCostScore());
					ResultOutput.writeTextFile(logFile, "\n");
				//}
				
				State<CorefCluster> stateinBeam = beam.peek();
				
				// halt stopping condition
				if (stopping.equals("halt")) {
					if (stateinBeam.getID().equals("HALT")) {
						generateStateDocument(document, previousBestState);
						stopSearch = true;
					} else {
						previousBestState = stateinBeam;
					}
				}
				
				// tuning stopping condition
				if (stopping.equals("tuning")) {
					if (globalCostScore < stateinBeam.getCostScore()) {
						globalCostScore = stateinBeam.getCostScore();
						stopscore = globalCostScore / stoppingRate;
					}
					
					if ((stateinBeam.getCostScore() < stopscore)) {
						generateStateDocument(document, previousBestState);
						stopSearch = true;
					} else {
						previousBestState = stateinBeam;
					}
				}
				
				// if satisfied, stop the search
				if (stopSearch) {
					break;
				}
				
				// output feature
				if (outputFeature && beam.size() > 0) {
					String id = beam.peek().getID();
					ResultOutput.writeTextFile(logFile, "best loss id : " + localBestLossStateID + "; beam state id : " + id + "\n");
					
					if (!localBestLossStateID.equals(id)) {
						generateOutput(states, localBestLossStateID, phaseID, trainingDataPath, msearchStep);
					}
				}
				
				ResultOutput.writeTextFile(bestLossScorePath, msearchStep + "\t" + bestLossScore);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			msearchStep++;
		}
		
		State<CorefCluster> copyBestLossState = new State<CorefCluster>();
		for (Integer key : bestLostState.getState().keySet()) {
			copyBestLossState.add(key, bestLostState.getState().get(key));
		}
	
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the best loss state score for " + document.getID() + " is " + bestLossScore);
		return copyBestLossState;
	}
	
	/**
	 * whether this state conforms hard constraint, which is defined as 
	 * 
	 * @param initial
	 * @return
	 */
	private boolean satisfyHardConstraint(State<CorefCluster> initial) {
		boolean conformHardConstraint = true;
		
		HashMap<String, ClassicCounter<String>> toPredictedCentroid = initial.getToClusterPredictedCentroid();
		HashMap<String, ClassicCounter<String>> fromPredictedCentroid = initial.getFromClusterPredictedCentroid();
		
		for (String feature : toPredictedCentroid.keySet()) {
			if (feature.startsWith("SRL") && !feature.contains("SRLAGREECOUNT")) {
				Counter<String> centFeature1 = toPredictedCentroid.get(feature);
				Counter<String> centFeature2 = fromPredictedCentroid.get(feature);
				
				int maximumSize = centFeature1.size() > centFeature2.size() ? centFeature1.size() : centFeature2.size();
				
				Set<String> featureSet = new HashSet<String>();
				featureSet.addAll(centFeature1.keySet());
				featureSet.addAll(centFeature2.keySet());
				
				if (featureSet.size() > maximumSize) {
					conformHardConstraint = false;
					break;
				}
			}
		}
		
		
		return conformHardConstraint;
	}
	
	/**
	 * Let ns* = arg min_{ns \in C(s)} Loss(ns)
	 * Let \hat{ns} = arg min_{ns \in C(s)} F_{i-1}(ns)
	 *
	 *if Loss(ns*) < Loss(\hat{ns})  //Error
	 * 		Generate one constraint for every ns \in C(s) - ns\*: ns\* > n
	 * @param states
	 * @param bestLossStateID
	 * @param phaseID
	 */
	private void generateOutput(Map<String, State<CorefCluster>> states, String bestLossStateID, String phaseID, String trainingDataPath, int searchStep) {
		List<String> constraints = new ArrayList<String>();
		constraints.add("NEWDATASET");
		int id = Integer.parseInt(phaseID);
		String path = trainingDataPath + "/" + id;
		ConstraintGeneration generator = new ConstraintGeneration(path);
		
		State<CorefCluster> goodState = states.get(bestLossStateID);
		constraints.add(generator.buildGoodConstraint(goodState));
		
		states.remove(bestLossStateID);
		
		for (String key : states.keySet()) {
			State<CorefCluster> state = states.get(key);
			String constraint = generator.buildBadConstraint(state);
			constraints.add(constraint);
		}
		
		// output the files
		LargeFileWriting writer = new LargeFileWriting(path);
		writer.writeArrays(constraints);
	}
	
	/**
	 * build halt feature, halt feature is 1, other features are all zero
	 * 
	 * @return
	 */
	private Counter<String> buildHaltFeature() {
		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
		Counter<String> features = new ClassicCounter<String>();
		for (String feature : featureTemplate) {
			if (feature.equals("HALT")) {
				features.setCount(feature, 1.0);
			} else {
				features.setCount(feature, 0.0);
			}
		}
		
		return features;
	}
	
	/** whether the two nodes are the same node */
	private boolean detectSameNode(State<CorefCluster> visited, State<CorefCluster> index) {
		boolean same = false;
		Map<Integer, CorefCluster> visitedCorefClusters = visited.getState();
		Map<Integer, CorefCluster> indexCorefClusters = index.getState();
		ScorerCEAF score = new ScorerCEAF();
		if (visitedCorefClusters.size() != indexCorefClusters.size()) {
			return same;
		}
		// maybe change
		double precisionNumSum = score.scoreHelper(visitedCorefClusters, indexCorefClusters);
		double precision = score.scoreHelper(visitedCorefClusters, visitedCorefClusters);
		double recallNumSum = score.scoreHelper(visitedCorefClusters, indexCorefClusters);
		double recall = score.scoreHelper(indexCorefClusters, indexCorefClusters);
		if ((precisionNumSum == precision) && (recallNumSum == recall)) {
			same = true;
		}
		return same;
	}
	
	
	private boolean detectBeamDuplicate(FixedSizePriorityQueue<State<CorefCluster>> beam, State<CorefCluster> index) {
		boolean duplciate = false;
		ScorerCEAF score = new ScorerCEAF();
		FixedSizePriorityQueue<State<CorefCluster>> beamcopy = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		while (!beam.isEmpty()) {
			State<CorefCluster> visited = beam.next();
			Map<Integer, CorefCluster> visitedCorefClusters = visited.getState();
			Map<Integer, CorefCluster> indexCorefClusters = index.getState();
			if (visitedCorefClusters.size() != indexCorefClusters.size()) continue;
			// maybe change
			double precisionNumSum = score.scoreHelper(visitedCorefClusters, indexCorefClusters);
			double precision = score.scoreHelper(visitedCorefClusters, visitedCorefClusters);
			double recallNumSum = score.scoreHelper(visitedCorefClusters, indexCorefClusters);
			double recall = score.scoreHelper(indexCorefClusters, indexCorefClusters);
			if ((precisionNumSum == precision) && (recallNumSum == recall)) {
				duplciate = true;
				break;
			}
			
			beamcopy.add(visited, visited.getScore()[0]);
		}
		
		while (!beamcopy.isEmpty()) {
			State<CorefCluster> cluster = beamcopy.next();
			beam.add(cluster, cluster.getScore()[0]);
		}
		
		return duplciate;
	}
	
	
	// detect the duplicate nodes
	private boolean detectClosedDuplicate(Set<State<CorefCluster>> closedList, State<CorefCluster> index) {
		boolean duplciate = false;
		ScorerCEAF score = new ScorerCEAF();
		for (State<CorefCluster> visited : closedList) {
			Map<Integer, CorefCluster> visitedCorefClusters = visited.getState();
			Map<Integer, CorefCluster> indexCorefClusters = index.getState();
			if (visitedCorefClusters.size() != indexCorefClusters.size()) continue;
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
	
}
