package edu.oregonstate.experiment.crosscoreferenceresolution;


import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.util.EecbConstants;

/**
 * do within coreference first, do not use search to guide the within coreference resolution, 
 * combine the within coreference resolution result produced by the Stanford System together,
 * and then do cross corefernce resolution on the combined document, produce the final result
 * 
 * @author xie
 */
public class CrossCoreferenceResolution extends ExperimentConstructor {

	
	
	@Override
	protected void performExperiment() {
		configureExperiment();
		
		if (mDebug) {
			// print every iteration
			
		} else {
			
		}
		
		
		
	}
	
	/** 
	 * Experiment Configuration
	 * classifier: structured perceptron (iteration no: 10)
	 * cost function: linear function
	 * loss function: pairwise
	 * search method: beam search(beam width: 5, search step: 300)
	 * clustering method: none
	 * debug: true
	 * 
	 */
	private void configureExperiment() {
		
		setDebugMode(true);
		
		// define classifier parameter
		addParas(EecbConstants.CLASSIFIER, "noOfIteration", 10);
		
		// define search parameter
		addParas(EecbConstants.SEARCHMETHOD, "beamWidth", 5);
		addParas(EecbConstants.SEARCHMETHOD, "searchStep", 300);
		
		// define dataset 
		addParas(EecbConstants.DATASET, "corpusPath", "../corpus/EECB1.0/data/");
		addParas(EecbConstants.DATASET, "srlpath", "../corpus/tokenoutput/");
		addParas(EecbConstants.DATASET, "sieve", "MarkRole, DiscourseMatch, ExactStringMatch, RelaxedExactStringMatch, PreciseConstructs, StrictHeadMatch1, StrictHeadMatch2, StrictHeadMatch3, StrictHeadMatch4, RelaxedHeadMatch");
		addParas(EecbConstants.DATASET, "annotationPath", "../corpus/mentions-backup.txt");
		addParas(EecbConstants.DATASET, "wordnetConfigurationPath", "../corpus/file_properties.xml");
		addParas(EecbConstants.DATASET, "wordsimilaritypath", "../corpus/sims.lsp");
		
		configureJWordNet();
		configureWordSimilarity();
		
		// define dataset model
		mDatasetMode = createDataSet("WithinCross");
		splitTopics(12);
		createDataSet();
		
		// define classfication model
		mclassifier = createClassifier("StructuredPerceptron");
		
		// define cost function model
		mCostFunction = createCostFunction("LinearFunction");
		
		// define loss function model
		mLossFunction = createLostFunction("PairwiseLossFunction");
		
		// define search method model
		mSearchMethod = createSearchMethod("BeamSearch");

	}
	
	/**
	 * Experiment Entry Point
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		CrossCoreferenceResolution ccr = new CrossCoreferenceResolution();
		ccr.performExperiment();
	}
	
}
