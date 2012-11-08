package edu.oregonstate.search;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import Jama.Matrix;

import edu.oregonstate.CDCR;
import edu.oregonstate.CorefSystem;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.general.PriorityQueue;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.training.CostFunction;
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
public class BeamSearch {
    
    private int mBeamWidth;
    
    private int maximumSearch;
    
    private static String mscoreOutputPath;
    
    private int moffset;
    
    private ScoreType type;
    
    private Document document;
    
    private Dictionaries dictionary;
    
    private Matrix initialWeight;
    
    private Set<String> rightLinks;
    
    private int searchStep;
    
    private Matrix averageWeight;
    
    private int violations;
    
    public int getSearchStep() {
    	return searchStep;
    }
    
    // constructor
    public BeamSearch(int beamWidth, String scoreOutputPath, ScoreType mtype, Document mdocument, int maximumSearch) {
    	mscoreOutputPath = scoreOutputPath;
        mBeamWidth = beamWidth;
        type = mtype;
        document = mdocument;
        this.maximumSearch = maximumSearch;
        CorefSystem cs = new CorefSystem();
        dictionary = cs.corefSystem.dictionaries();
     	
     	rightLinks = new HashSet<String>();
     	initializeRightLinks();
     	searchStep = 0;
    }
    
    /** build the right links for the gold clusters */
    private void initializeRightLinks() {
    	for (CorefCluster cluster : document.goldCorefClusters.values()) {
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
                	rightLinks.add(iID + "-" + jID);
                }
    		}
    	}
    	
    	System.out.println("Finish build the right links for the gold clusters");
    }
    
    public Matrix getWeight() {
    	return initialWeight;
    }
    
    public void setWeight(Matrix weight) {
    	initialWeight = weight;
    }
    
    public void setAverageWeight(Matrix weight) {
    	averageWeight = weight;
    }
    
    public Matrix getAverageWeight() {
    	return averageWeight;
    }
    
    // calculate F1, Precision and Recall according to the Score Type
    public double[] calculateF1(Document document, ScoreType type) {
        double F1 = 0.0;
        CorefScorer score;
        switch(type) {
            case MUC:
                score = new ScorerMUC();
                break;
            case BCubed:
                score = new ScorerBCubed(BCubedType.Bconll);
                break;
            case CEAF:
                score = new ScorerCEAF();
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
        
        double[] result = {transformNaN(F1), transformNaN(precision), transformNaN(recall)};
        return result;
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
        ResultOutput.writeTextFile(CDCR.outputFileName, "before create children: total of clusters : " + size);
        System.out.println("total of clusters : " + size);
        
        // generate the action
        for (int i = 0; i < size; i++) {
            Integer iID = keys.get(i);
            for (int j = 0; j < i; j++) {
                Integer jID = keys.get(j);
                String action = iID + "-" + jID + "-" +  moffset;;
                actions.add(action);
                moffset += 1;
            }
        }
        
        ResultOutput.writeTextFile(CDCR.outputFileName, "after create children: total of clusters : " + (size - 1));
        ResultOutput.writeTextFile(CDCR.outputFileName, "the number of candidate sets :" + actions.size());
        System.out.println("the number of candidate sets : " + actions.size());
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
	
    private void addGoldCorefCluster(Document document, Document corpus) {
		for (Integer id : document.goldCorefClusters.keySet()) {
			corpus.addGoldCorefCluster(id, document.goldCorefClusters.get(id));
		}
	}
	
	private void addPredictedMention(Document document, Document corpus) {
		for (Integer id : document.allPredictedMentions.keySet()) {
			corpus.addPredictedMention(id, document.allPredictedMentions.get(id));
		}
	}

	private void addGoldMention(Document document, Document corpus) {
		for (Integer id : document.allGoldMentions.keySet()) {
			corpus.addGoldMention(id, document.allGoldMentions.get(id));
		}
	}
	
	/**
	 * add four fields to the corpus
	 * 
	 * @param document
	 */
	private void add(Document document, Document corpus) {
		addGoldCorefCluster(document, corpus);
		addPredictedMention(document, corpus);
		addGoldMention(document, corpus);
	}
	
	/**
	 * test whether the state is a goal state
	 * 
	 * @param document
	 * @param state
	 * @param type
	 * @return
	 */
	private double[] calculateF1Score(State<CorefCluster> state, ScoreType type) {
		Document documentState = new Document();
    	add(document, documentState);
    	setNextDocument(documentState, state);
    	double[] scores = calculateF1(documentState, type);
    	//double score = scores[0];
		return scores;
	}
	
	/** print the local score */
	private void printScoreInformation(double[] localScores, ScoreType mtype) {
		assert localScores.length == 3;
		ResultOutput.writeTextFile(CDCR.outputFileName, "local " + mtype.toString() + " F1 Score: " + Double.toString(localScores[0]));
		ResultOutput.writeTextFile(CDCR.outputFileName, "local " + mtype.toString() + " precision Score: " + Double.toString(localScores[1]));
		ResultOutput.writeTextFile(CDCR.outputFileName, "local " + mtype.toString() + " recall Score: " + Double.toString(localScores[2]));
	}
	
	/** reach gold state */
	private void reachGoldState() {
		ResultOutput.writeTextFile(CDCR.outputFileName, "\n=====================================");
		ResultOutput.writeTextFile(CDCR.outputFileName, "reach gold state");
		ResultOutput.writeTextFile(CDCR.outputFileName, "global" + type.toString() +" F1 score: 1.0");
		ResultOutput.writeTextFile(mscoreOutputPath, "1.0");
		ResultOutput.writeTextFile(CDCR.outputFileName, "=====================================\n");
	}
	
	/** regenerate features for each corefCluster */
	private void regenerateFeatures(State<CorefCluster> indexState) {
		setNextDocument(document, indexState);
		for (Integer id : document.corefClusters.keySet()) {
        	CorefCluster cluster = document.corefClusters.get(id);
        	cluster.regenerateFeature();
        }
	}
	
	/** calculate cost score according to the weight and feature */
	private double calculateCostScore(State<CorefCluster> initial, String action) {
		String[] ids = action.split("-");
		Integer i_id = Integer.parseInt(ids[0]);
		Integer j_id = Integer.parseInt(ids[1]);
		CorefCluster iCluster = initial.getState().get(i_id);
		CorefCluster cpCluster = new CorefCluster(i_id, iCluster.getCorefMentions());
		CorefCluster jCluster = initial.getState().get(j_id);
		Counter<String> features = Feature.getFeatures(document, iCluster, jCluster, false, dictionary);
		CostFunction cf = new CostFunction(features, initialWeight);
		double costScore = cf.calculateScore();
		mergeClusters(cpCluster, jCluster);
		initial.remove(i_id);
		initial.remove(j_id);
		initial.add(i_id, cpCluster);
		
		initial.setFeatures(features);
		initial.setCostScore(costScore);
		initial.setID(action);
		
		return costScore;
	}
	
	private double transformNaN(double value) {
		double result = value;
		if (Double.isNaN(value)) {
			result = 0.0;
		}
		return result;
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
	public void bestFirstBeamSearch() {
		// begin time
		ResultOutput.printTime();
		violations = 0;
		moffset = 2;
		// keep track of the global maximum score
		Double globalScore = 0.0;
		Double globalCostScore = 0.0;
		// closed list to track duplicate method, now I do not use this
		//Set<State<CorefCluster>> closedList = new HashSet<State<CorefCluster>>();
		// initialize a beam using fixed size priority queue with beam width
		FixedSizePriorityQueue<State<CorefCluster>> beam = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		State<CorefCluster> initialState = new State<CorefCluster>();
		/** the best output y^{*}_{i} uncovered so far evaluated by the loss function */
		State<CorefCluster> bestState = new State<CorefCluster>();
		initialize(document, initialState);
		
		double[] localScores = calculateF1(document, type);
		printScoreInformation(localScores, type);
		initialState.setScore(localScores);
		
		// add the state to the beam
		beam.add(initialState, transformNaN(localScores[0]));
		
		boolean breakWhile = false;
		int index = 1;
		
		while (beam.size() != 0 && (index < maximumSearch)) {
			searchStep++;
			ResultOutput.writeTextFile(CDCR.outputFileName, "action " + index);
			System.out.println(type.toString() + " loss function : action " + index);
			
			// the state with the highest score
			State<CorefCluster> indexState = beam.next();
			double[] localScore = indexState.getScore();
			double score = localScore[0];
			printScoreInformation(localScore, type);
			if (score >= globalScore) {
				globalScore = score;
				//TODO
				// update weight here
				bestState = indexState;
				globalCostScore = indexState.getCostScore();
			}
			if (globalScore > score) break;
			System.out.println(globalScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" F1 score: " + globalScore.toString());
			// first check whether it is a goal state
			if (globalScore == 1.0) {
				reachGoldState();
				break;
			}
			ResultOutput.writeTextFile(mscoreOutputPath, globalScore.toString());
			// add the node to the explored list
			//closedList.add(indexState);
			
			// update the feature according to new state
			regenerateFeatures(indexState);
			
			try {
				
				Set<String> actions = generateCandidateSets(indexState);
				int increment = 0;
				int actionSize = actions.size();
				PriorityQueue<State<CorefCluster>> statesLossFunction = new PriorityQueue<State<CorefCluster>>();
				Map<String, State<CorefCluster>> states = new HashMap<String, State<CorefCluster>>();
				for (String action : actions) {
					
					increment += 1;
					System.out.println("the remaining action " + (actionSize - increment));
					// make a copy of indexState
					State<CorefCluster> initial = new State<CorefCluster>();
					for (Integer key : indexState.getState().keySet()) {
						initial.add(key, indexState.getState().get(key));
					}
					
					double costScore = calculateCostScore(initial, action);
					//boolean beamContains = detectBeamDuplicate(beam, initial);
					//boolean closedContains = detectClosedDuplicate(closedList, initial);
					
					//if (closedContains) continue;
					
					double[] stateScore = calculateF1Score(initial, type);
					initial.setScore(stateScore);
            		beam.add(initial, stateScore[0]);
            		statesLossFunction.add(initial, stateScore[0]);
            		states.put(action, initial);
				}
				
				// add the according constraint here
				/*
				SearchConstraint sc = new SearchConstraint(rightLinks, statesLossFunction, beam, states, bestState);
				sc.setInitialWeight(initialWeight);
				sc.setAverageWeight(averageWeight);
				sc.updateWeight();
				initialWeight = sc.getInitialWeight();
				averageWeight = sc.getAverageWeight();
				violations += sc.getViolations();
				*/
				
				StochasticGradient sg = new StochasticGradient(0.9, globalScore, states);
				sg.setInitialWeight(initialWeight);
				sg.updateWeight();
				sg.setCostScore(globalCostScore);
				initialWeight = sg.getInitialWeight();
				
				//initialWeight = initialWeight.plus(sc.getInitialWeight());
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			index++;
			if (breakWhile) break;
		}
		
		ResultOutput.writeTextFile(CDCR.outputFileName, "the total number of violations is :" + violations);
		
		// end time
		ResultOutput.printTime();
	}
	
	// apply the weight to guide the search
	// In this case, we should choose which one to expand, the minimum cost score or the maximum cost score 
	// In addition, we need to decide in the training part, is the states and costs are the same, maybe they are not same
	// because I use a condition to add the state into the states and beam
	// In addition, I did not use closed list to keep track of the depulicate states, because it costs a lot of time
	public void bestFirstBeamSearchForTest() {
		
		// begin time
		ResultOutput.printTime();
		ResultOutput.writeTextFile(CDCR.outputFileName, "do testing");
		ResultOutput.writeTextFile(CDCR.outputFileName, ResultOutput.printStructredModel(initialWeight, Feature.featuresName));
		
		moffset = 2;
		// keep track of the global maximum score
		Double globalScore = Double.MIN_VALUE;
		// closed list to track duplicate method
		//Set<State<CorefCluster>> closedList = new HashSet<State<CorefCluster>>();
		FixedSizePriorityQueue<State<CorefCluster>> beam = new FixedSizePriorityQueue<State<CorefCluster>>(mBeamWidth);
		State<CorefCluster> initialState = new State<CorefCluster>();
		initialize(document, initialState);
		beam.add(initialState, globalScore);
		
		boolean breakWhile = false;
		int index = 1;
		while(beam.size() != 0 && (index < maximumSearch)) {
			// evaluate whether we should stop the search
			// The criterion is that the global score should be higher than the local score. If not, then stop, we also can use step size to control stop
			ResultOutput.writeTextFile(CDCR.outputFileName, "action " + index);
			System.out.println(type.toString() + " loss function : action " + index);
			
			// the state with the highest score
			State<CorefCluster> indexState = beam.next();
			// get its current state
			double[] scores = calculateF1Score(indexState, type);
			printScoreInformation(scores, type);
			printScoreInformation(calculateF1Score(indexState, ScoreType.MUC), ScoreType.MUC);
			printScoreInformation(calculateF1Score(indexState, ScoreType.BCubed), ScoreType.BCubed);
			printScoreInformation(calculateF1Score(indexState, ScoreType.CEAF), ScoreType.CEAF);
			double localScore = indexState.getCostScore();
			if (localScore >= globalScore) {
				globalScore = localScore;
			}
			//if (globalScore > localScore) break;
			System.out.println(globalScore);
			ResultOutput.writeTextFile(CDCR.outputFileName, "global " + type.toString() +" Cost score: " + globalScore.toString());
			ResultOutput.writeTextFile(CDCR.outputFileName, type + " F1 score " + scores[0]);
			
			ResultOutput.writeTextFile(mscoreOutputPath, "score : " + scores[0]);
			//closedList.add(indexState);
			regenerateFeatures(indexState);
			
			try {
				/** get the candidate lists*/
				Set<String> actions = generateCandidateSets(indexState);
				int increment = 0;
				int actionSize = actions.size();
				for (String action : actions) {
					increment += 1;
					System.out.println("the remaining action " + (actionSize - increment));
					State<CorefCluster> initial = new State<CorefCluster>();
					for (Integer key : indexState.getState().keySet()) {
						initial.add(key, indexState.getState().get(key));
					}
					
					double costScore = calculateCostScore(initial, action);
					//boolean closedContains = detectClosedDuplicate(closedList, initial);
					
					//if (closedContains) continue;
					
					//Document documentState = new Document();
	            	//add(document, documentState);
	            	//setNextDocument(documentState, neighbor);
	            	// for this case, we need to make sure that the next state should score higher than the previous score
	            	// I can modify here
	            	//closedList.put(Integer.parseInt(action.split("-")[2]), initial);
	            	beam.add(initial, costScore);
				}				

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
			index++;
			if (breakWhile) break;
		}
		
		// end time
		ResultOutput.printTime();
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
		//TODO
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
			//TODO
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
			//TODO
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
