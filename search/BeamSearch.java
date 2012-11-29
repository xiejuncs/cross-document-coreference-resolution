package edu.oregonstate.search;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import edu.oregonstate.CorefSystem;
import edu.oregonstate.classifier.IClassifier;
import edu.oregonstate.costfunction.ICostFunction;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.general.MatrixOperation;
import edu.oregonstate.general.PriorityQueue;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.lossfunction.ILossFunction;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.util.DocumentMerge;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.Dictionaries.Animacy;
import edu.stanford.nlp.dcoref.Dictionaries.Gender;
import edu.stanford.nlp.dcoref.Dictionaries.Number;
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
    private int mBeamWidth;
    
    /** search depth */
    private int maximumSearch;
    
    /** id offset */
    private int moffset;
    
    /** document */
    private Document mdocument;
    
    /** dictionary, which is used for generating features */
    private Dictionaries mdictionary;
    
    /** initial weight */
    private double[] mweight;
    
    /** right links */
    private Set<String> mrightLinks;
    
    /** how many real search conducted by beam search */
    private int msearchStep;
    
    /** total weight */
    private double[] mtotalWeight;
    
    /** no of violations */
    private int mviolations;
    
    /** loss function */
    private ILossFunction lossFunction;
    
    /** cost function */
    private ICostFunction costFunction;
    
    /** classifier */
    private IClassifier classifier;
    
    /** score type */
    private ScoreType type;
    
    /** constructor */
    public BeamSearch() {
    	mBeamWidth = (Integer) ExperimentConstructor.getParameter(EecbConstants.SEARCHMETHOD, "beamWidth");
    	maximumSearch = (Integer) ExperimentConstructor.getParameter(EecbConstants.SEARCHMETHOD, "searchStep");
        mdictionary = ExperimentConstructor.mdictionary;
        mrightLinks = new HashSet<String>();
        lossFunction = ExperimentConstructor.createLossFunction((String) ExperimentConstructor.getParameter(EecbConstants.LOSSFUNCTION, "model"));
        costFunction = ExperimentConstructor.createCostFunction((String) ExperimentConstructor.getParameter(EecbConstants.COSTFUNCTION, "model"));
        classifier = ExperimentConstructor.createClassifier((String) ExperimentConstructor.getParameter(EecbConstants.CLASSIFIER, "model"));
        type = (ScoreType) ExperimentConstructor.getParameter(EecbConstants.LOSSFUNCTION, "scoreType");
    }
    
    public int getSearchStep() {
    	return msearchStep;
    }
    
    public int getViolations() {
    	return mviolations;
    }
    
    public double[] getWeight() {
    	return mweight;
    }
    
    public void setWeight(double[] weight) {
    	mweight = weight;
    }
    
    public void setTotalWeight(double[] weight) {
    	mtotalWeight = weight;
    }
    
    public double[] getTotalWeight() {
    	return mtotalWeight;
    }
    
    public void setDocument(Document document){
    	mdocument = document;
    }
    
    /** build the right links for the gold clusters */
    private void initializeRightLinks() {
    	for (CorefCluster cluster : mdocument.goldCorefClusters.values()) {
    		int clusterSize = cluster.getCorefMentions().size();
    		if (clusterSize == 1) continue;
    		List<Integer> ids = new ArrayList<Integer>();
    		for (Mention mention : cluster.getCorefMentions()) {
    			ids.add(mention.mentionID);
    		}
    		
    		for (int i = 0; i < ids.size(); i++) {
                int iID = ids.get(i);
                for (int j = 0; j < i; j++) {
                	int jID = ids.get(j);
                	mrightLinks.add(iID + "-" + jID);
                }
    		}
    	}
    }
    
    // get the neighbors of the current state
    private Set<String> generateCandidateSets(State<CorefCluster> state) {
        Set<String> actions = new HashSet<String>();
        Map<Integer, CorefCluster> clusters = state.getState();
        List<Integer> keys = new ArrayList<Integer>();
        for (Integer key : clusters.keySet()) {
            keys.add(key);
        }
        
        int size = keys.size();
        ResultOutput.writeTextFile(ExperimentConstructor.logFile, "before create children: total of clusters : " + size);
        
        // generate the action
        for (int i = 0; i < size; i++) {
            Integer iID = keys.get(i);
            //CorefCluster icluster = clusters.get(iID);
            //do not deal with pronoun
            //if (icluster.corefMentions.size() == 1 && icluster.firstMention.isPronominal()) {
            //	continue;
            //}
            
            for (int j = 0; j < i; j++) {
                Integer jID = keys.get(j);
                //CorefCluster jcluster = clusters.get(jID);
                //do not deal with pronoun
               // if (jcluster.corefMentions.size() == 1 && jcluster.firstMention.isPronominal()) {
               // 	continue;
                //}
                
                String action = iID + "-" + jID + "-" +  moffset;;
                actions.add(action);
                moffset += 1;
            }
        }
        
        ResultOutput.writeTextFile(ExperimentConstructor.logFile, "after create children: total of clusters : " + (size - 1));
        ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the number of candidate sets :" + actions.size());
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
	 * we represent the initial state as coreference trees in order to make the computation tractable.
	 * At first, we do not represent the cluster as a tree
	 * 
	 * @param document
	 * @param initialState
	 * @param goalState
	 */
    private void initialize(Document document, State<CorefCluster> initialState){
        for (Integer key : document.corefClusters.keySet()) {
            CorefCluster cluster = document.corefClusters.get(key);
            CorefCluster cpCluster = new CorefCluster(key, cluster.getCorefMentions());
            initialState.add(key, cpCluster);
        }
    }
    
	
	/** print the local score */
	private void printScoreInformation(double[] localScores, ScoreType mtype) {
		assert localScores.length == 3;
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "local" + mtype.toString() + " F1 Score: " + Double.toString(localScores[0]));
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "local" + mtype.toString() + " precision Score: " + Double.toString(localScores[1]));
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "local" + mtype.toString() + " recall Score: " + Double.toString(localScores[2]));
	}
	
	/** reach gold state */
	private void reachGoldState() {
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "\n=====================================");
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "reach gold state");
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "=====================================\n");
	}
	
	/**
	 * update the allPredictedMentions
	 * The reason for this is that the corefClusters information has been updated. The mention id should be consistent 
	 * with the allPredictedMentions and corefClusters
	 * 
	 * @param documentState
	 * @param state
	 */
	private void setNextDocument(Document documentState, State<CorefCluster> state) {
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
	private void regenerateFeatures(State<CorefCluster> indexState) {
		setNextDocument(mdocument, indexState);
		for (Integer id : mdocument.corefClusters.keySet()) {
        	CorefCluster cluster = mdocument.corefClusters.get(id);
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
	private void calculateCostScore(State<CorefCluster> initial, String action) {
		String[] ids = action.split("-");
		Integer i_id = Integer.parseInt(ids[0]);
		Integer j_id = Integer.parseInt(ids[1]);
		CorefCluster iCluster = initial.getState().get(i_id);
		CorefCluster cpCluster = new CorefCluster(i_id, iCluster.getCorefMentions());
		CorefCluster jCluster = initial.getState().get(j_id);
		Counter<String> features = Feature.getFeatures(mdocument, iCluster, jCluster, false, mdictionary);
		
		// merge cluster
		mergeClusters(cpCluster, jCluster);
		initial.remove(i_id);
		initial.remove(j_id);
		initial.add(i_id, cpCluster);
		
		// calculate the cost function for the state
		costFunction.setFeatures(features);
		costFunction.setWeight(mweight);
		double costScore = costFunction.calculateCostFunction();
		initial.setFeatures(features);
		initial.setCostScore(costScore);
		initial.setID(action);
	}
	
	/**
	 * conduct beam search on specific document given specific true loss function
	 * In addition, we also need to define the beam search guided by the learned cost function
	 * 
	 * If the search is guided by the cost function, then there is no goal state specified, so we 
	 * do not need to define a function to check whether the current state is the goal state
	 *  
	 * 
	 * @param document
	 * @param type
	 */
	public void trainingSearch() {
		
		// define variables
		initializeRightLinks();
		moffset = 2;
		msearchStep = 0;
		mviolations = 0;
		Double globalScore = 0.0;
		Double globalCostScore = 0.0;
		String globalDetailInformation = "";		
		
		// beam and closed list
		//Set<State<CorefCluster>> closedList = new HashSet<State<CorefCluster>>();
		FixedSizePriorityQueue<State<CorefCluster>> beam = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		State<CorefCluster> initialState = new State<CorefCluster>();
		initialize(mdocument, initialState);
		lossFunction.setDocument(mdocument);
		double[] localScores = lossFunction.getMetricScore();
		initialState.setScore(localScores);
		initialState.setScoreDetailInformation(lossFunction.getDetailScoreInformation());
		beam.add(initialState, localScores[0]);
		
		// the best output y^{*}_{i} uncovered so far evaluated by the loss function
		State<CorefCluster> bestState = new State<CorefCluster>();
		State<CorefCluster> previousBestState = new State<CorefCluster>();
		
		// keep search
		while (beam.size() != 0 && (msearchStep < maximumSearch)) {
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "action " + msearchStep);
			
			// the state with the highest score
			State<CorefCluster> indexState = beam.next();
			double[] localScore = indexState.getScore();
			double score = localScore[0];
			String information = indexState.getScoreDetailInformation();
			printScoreInformation(localScore, type);
			String id = indexState.getID();
			if (!id.equals("")) {
				CorefCluster cluster = indexState.getState().get(Integer.parseInt(id.split("-")[0]));
				ResultOutput.writeTextFile(ExperimentConstructor.logFile, cluster.toString());
			}
			
			// whether continue
			if (score >= globalScore) {
				globalScore = score;
				globalDetailInformation = information;
				
				// best state
				previousBestState = bestState;
				bestState = indexState;
				globalCostScore = indexState.getCostScore();
			}
			if (globalScore > score) break;
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "global " + type.toString() +" F1 score: " + globalScore.toString());
			ResultOutput.writeTextFile(ExperimentConstructor.mscorePath, globalScore.toString() + " " + globalCostScore);
			ResultOutput.writeTextFile(ExperimentConstructor.mScoreDetailPath, globalDetailInformation);
			
			// first check whether it is a goal state
			if (globalScore == 1.0) {
				regenerateFeatures(indexState);
				reachGoldState();
				break;
			}

			// add the node to the explored list
			//closedList.add(indexState);
			
			// update the feature according to new state
			regenerateFeatures(indexState);
			
			try {
				Set<String> actions = generateCandidateSets(indexState);
				PriorityQueue<State<CorefCluster>> statesLossFunction = new PriorityQueue<State<CorefCluster>>();
				Map<String, State<CorefCluster>> states = new HashMap<String, State<CorefCluster>>();
				for (String action : actions) {
					// make a copy of indexState
					State<CorefCluster> initial = new State<CorefCluster>();
					for (Integer key : indexState.getState().keySet()) {
						initial.add(key, indexState.getState().get(key));
					}
					
					calculateCostScore(initial, action);
					//boolean beamContains = detectBeamDuplicate(beam, initial);
					//boolean closedContains = detectClosedDuplicate(closedList, initial);
					
					//if (closedContains) continue;
					
					lossFunction.setState(initial);
					lossFunction.calculateLossFunction();
					double[] stateScore = lossFunction.getLossScore();
					initial.setScore(stateScore);
					initial.setScoreDetailInformation(lossFunction.getDetailScoreInformation());
					
					
					if (beam.isEmpty()) {
						beam.add(initial, stateScore[0]);
					} else {
						double highestPriority = beam.peek().getScore()[0];
						if (highestPriority == stateScore[0]) {
							double beamcostscore = beam.peek().getCostScore();
							if (initial.getCostScore() > beamcostscore) {
								State<CorefCluster> state = beam.next();
								beam.add(initial, highestPriority);
							}
						} else {
							beam.add(initial, stateScore[0]);
						}
					}
					
					statesLossFunction.add(initial, stateScore[0]);
					states.put(action, initial);
				}
				
				/** set classifier information */
				if (ExperimentConstructor.updateWeight) {
					classifier.setBeam(beam);
					classifier.setBestState(bestState);
					classifier.setPreviousBestState(previousBestState);
					classifier.setRightLinks(mrightLinks);
					classifier.setState(states);
					classifier.setStatesLossFunction(statesLossFunction);
					classifier.setWeight(mweight);
					classifier.setTotalWeight(mtotalWeight);
					classifier.setSearchStep(msearchStep);
					classifier.setBestScore(globalScore);
					classifier.train();
					mweight = classifier.getWeight();
					mtotalWeight = classifier.getTotalWeight();
					mviolations += classifier.getViolations();
					
					// normalize the weight
					if (ExperimentConstructor.normalizeWeight) {
						mweight = DoubleOperation.normalize(mweight);
						mtotalWeight = DoubleOperation.normalize(mtotalWeight);
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			msearchStep++;
		}
		
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "the total number of violations is :" + mviolations);
	}
	
	private boolean isAllZeroFeature(Counter<String> features){
		boolean isallzerofeature = true;
		for (int i = 0; i < Feature.featuresName.length; i++) {
 			String feature = Feature.featuresName[i];
 			double value = features.getCount(feature);
 			value = DoubleOperation.transformNaN(value);
 			if (value != 0.0) {
 				isallzerofeature = false;
 			}
 		}
		
		
		return isallzerofeature;
	}
	
	/**
	 * output feature
	 * 
	 * @param states
	 * @param index
	 */
	private void outputFeature(Map<String, State<CorefCluster>> states, int index) {
		String filePath = ExperimentConstructor.currentExperimentFolder + "/" + index;
		for (String key : states.keySet()) {
			StringBuffer sb = new StringBuffer();
			sb.append(key + ",");
			Counter<String> features = states.get(key).getFeatures();
			for (String feature : Feature.featuresName){
				double value = features.getCount(feature);
				sb.append(value + ",");
			}
			double costscore = states.get(key).getCostScore();
			double lossscore = states.get(key).getScore()[0];
			sb.append(costscore + ",");
			sb.append(lossscore);
			ResultOutput.writeTextFile(filePath, sb.toString().trim());
		}
	}
	
	/**
	 * calculate F1 score
	 * 
	 * @param mstate
	 * @param mtype
	 * @return
	 */
	private double[] calculateF1Score(State<CorefCluster> mstate, ScoreType mtype) {
		Document documentState = new Document();
		DocumentMerge dm = new DocumentMerge(mdocument, documentState);
		dm.addDocument();
    	setNextDocument(documentState, mstate);
    	double[] scores = calculateF1(documentState, mtype);
    	return scores;
	}
	
	private double[] calculateF1(Document document, ScoreType type) {
        double F1 = 0.0;
        CorefScorer score;
        String path = "";
        switch(type) {
            case MUC:
                score = new ScorerMUC();
                path = ExperimentConstructor.mMUCScoreDetailPath;
                break;
            case BCubed:
                score = new ScorerBCubed(BCubedType.Bconll);
                path = ExperimentConstructor.mBcubedScoreDetailPath;
                break;
            case CEAF:
                score = new ScorerCEAF();
                path = ExperimentConstructor.mCEAFScoreDetailPath;
                break;
            case Pairwise:
                score = new ScorerPairwise();
                break;
            default:
                score = new ScorerMUC();
                break;
        }
        
        score.calculateScore(document);
        F1 = score.getF1();
        double precision = score.getPrecision();
        double recall = score.getRecall();
        
        double precisionNumSum = score.precisionNumSum;
        double precisionDenSum = score.precisionDenSum;
        double recallNumSum = score.recallNumSum;
        double recallDenSum = score.recallDenSum;
        
        String information = precisionNumSum + " " + precisionDenSum + " " + recallNumSum + " " + recallDenSum;
        ResultOutput.writeTextFile(path, information);
        
        double[] result = {DoubleOperation.transformNaN(F1), DoubleOperation.transformNaN(precision), DoubleOperation.transformNaN(recall)};
        return result;
    }
	
	
	// apply the weight to guide the search
	// In this case, we should choose which one to expand, the minimum cost score or the maximum cost score 
	// In addition, we need to decide in the training part, is the states and costs are the same, maybe they are not same
	// because I use a condition to add the state into the states and beam
	// In addition, I did not use closed list to keep track of the duplicate states, because it costs a lot of time
	// 
	// define termination condition for test search
	public void testingSearch() {
		
		// begin time
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "do testing");
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, ResultOutput.printStructredModel(mweight, Feature.featuresName));
		moffset = 2;
		msearchStep = 0;
		
		double globalCostScore = 0.0;
		double stopscore = 0.0;
		// closed list to track duplicate method
		//Set<State<CorefCluster>> closedList = new HashSet<State<CorefCluster>>();
		FixedSizePriorityQueue<State<CorefCluster>> beam = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		State<CorefCluster> initialState = new State<CorefCluster>();
		initialize(mdocument, initialState);
		beam.add(initialState, 0.0);
		
		while(beam.size() != 0 && (msearchStep < maximumSearch)) {
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "action " + msearchStep);
			
			// the state with the highest cost score and print its related information
			State<CorefCluster> indexState = beam.next();
			lossFunction.setDocument(mdocument);
			lossFunction.setState(initialState);
			double[] scores = lossFunction.getMetricScore();
			printScoreInformation(scores, type);
			printScoreInformation(calculateF1Score(indexState, ScoreType.MUC), ScoreType.MUC);
			printScoreInformation(calculateF1Score(indexState, ScoreType.BCubed), ScoreType.BCubed);
			printScoreInformation(calculateF1Score(indexState, ScoreType.CEAF), ScoreType.CEAF);
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, type + " F1 score " + scores[0]);
			ResultOutput.writeTextFile(ExperimentConstructor.mscorePath, Double.toString(scores[0]) + " " + indexState.getCostScore());
			ResultOutput.writeTextFile(ExperimentConstructor.mScoreDetailPath, lossFunction.getDetailScoreInformation());
			
			if (globalCostScore < indexState.getCostScore()) {
				globalCostScore = indexState.getCostScore();
				stopscore = globalCostScore / ExperimentConstructor.stoppingRate;
			}
			
			if (ExperimentConstructor.stoppingCriterion) {
				if ((indexState.getCostScore() < stopscore)) {
					break;
				}
			}
			
			double localScore = indexState.getCostScore();
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, type.toString() +" Cost score: " + localScore);
			
			//closedList.add(indexState);
			regenerateFeatures(indexState);  // update the mdocument
			
			try {
				/** get the candidate lists*/
				Set<String> actions = generateCandidateSets(indexState);
				List<Double> costScores = new ArrayList<Double>();
				for (String action : actions) {
					State<CorefCluster> initial = new State<CorefCluster>();
					for (Integer key : indexState.getState().keySet()) {
						initial.add(key, indexState.getState().get(key));
					}
					
					calculateCostScore(initial, action);
					costScores.add(initial.getCostScore());
					//boolean closedContains = detectClosedDuplicate(closedList, initial);
					
					//if (closedContains) continue;
					
	            	beam.add(initial, initial.getCostScore());
				}				
				
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			msearchStep++;			
		}
	}
	
	/**
	 * detect whether cost function are all zero, if all are lower than 0, then stop the testing
	 * 
	 * @param costScores
	 * @return
	 */
	private boolean allNegative(List<Double> costScores){
		boolean allNegative = true;
		for (Double score : costScores) {
			if (score > 0) {
				allNegative = false;
			}
		}
		return allNegative;
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
