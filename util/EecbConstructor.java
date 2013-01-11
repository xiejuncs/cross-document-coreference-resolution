package edu.oregonstate.util;

import edu.oregonstate.method.IMethod;
import edu.oregonstate.classifier.IClassifier;
import edu.oregonstate.cluster.IClustering;
import edu.oregonstate.costfunction.ICostFunction;
import edu.oregonstate.dataset.IDataSet;
import edu.oregonstate.lossfunction.ILossFunction;
import edu.oregonstate.score.ScorerCEAF;
import edu.oregonstate.search.ISearch;
import edu.stanford.nlp.dcoref.CorefScorer;
import edu.stanford.nlp.dcoref.ScorerBCubed;
import edu.stanford.nlp.dcoref.ScorerMUC;
import edu.stanford.nlp.dcoref.ScorerPairwise;
import edu.stanford.nlp.dcoref.CorefScorer.ScoreType;
import edu.stanford.nlp.dcoref.ScorerBCubed.BCubedType;

/**
 * This is a class to centralize all of the reflection inside of the program
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbConstructor {

	/**
	 * according to the model name, create a classifier to do classification
	 * 
	 * @param modelName
	 * @return
	 */
	public static IClassifier createClassifier(String classficationModel) {
		if (classficationModel == null) throw new RuntimeException("classifier not specified");
		
		if (!classficationModel.contains(".")) {
			classficationModel = "edu.oregonstate.classifier." + classficationModel;
		}
		
		try{
			Class classifierClass = Class.forName(classficationModel);
			IClassifier classifier = (IClassifier) classifierClass.newInstance();
			return classifier;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * according to the cost function name, create a cost function
	 * 
	 * @param costfunction
	 */
	public static ICostFunction createCostFunction(String costfunction) {
		if (costfunction == null) throw new RuntimeException("cost function not specified");
		
		if (!costfunction.contains(".")) {
			costfunction = "edu.oregonstate.costfunction." + costfunction;
		}
		
		try {
			Class costfunctionClass = Class.forName(costfunction);
			ICostFunction costFunction = (ICostFunction) costfunctionClass.newInstance();
			return costFunction;
		} catch (Exception e) {
			throw new RuntimeException("e");
		}
	}
	
	/**
	 * create a loss function
	 * 
	 * @param lossFunction
	 */
	public static ILossFunction createLossFunction(String lossFunction) {
		if (lossFunction == null) throw new RuntimeException("loss function not specified");
		
		if (!lossFunction.contains(".")) {
			lossFunction = "edu.oregonstate.lossfunction." + lossFunction;
		}
		
		try {
			Class lossfunctionClass = Class.forName(lossFunction);
			ILossFunction mlossFunction = (ILossFunction) lossfunctionClass.newInstance();
			return mlossFunction;
		} catch (Exception e) {
			throw new RuntimeException("e");
		}
	}
	
	/**
	 * create a search method
	 * 
	 * @param searchMethod
	 */
	public static ISearch createSearchMethod(String searchMethod) {
		if (searchMethod == null) throw new RuntimeException("search method not specified");
		
		if (!searchMethod.contains(".")) {
			searchMethod = "edu.oregonstate.search." + searchMethod;
		}
		
		try {
			Class searchMethodClass = Class.forName(searchMethod);
			ISearch SearchMethod = (ISearch) searchMethodClass.newInstance();
			return SearchMethod;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * create a clustering method
	 * 
	 * @param clusteringModel
	 */
	public static IClustering createClusteringModel(String clusteringModel) {
		if (clusteringModel == null) throw new RuntimeException("clustering method not specified");
		
		if (!clusteringModel.contains(".")) {
			clusteringModel = "edu.oregonstate.cluster" + clusteringModel;
		}
		
		try {
			Class clusteringClass = Class.forName(clusteringModel);
			IClustering ClusteringModel = (IClustering) clusteringClass.newInstance();
			return ClusteringModel;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * create dataset model
	 * 
	 * @param datasetModel
	 * @return
	 */
	public static IDataSet createDataSetModel(String datasetModel) {
		if (datasetModel == null) throw new RuntimeException("dataset model not specified");
		
		if (!datasetModel.contains(".")) {
			datasetModel = "edu.oregonstate.dataset." + datasetModel;
		}
		
		try {
			Class datasetClass = Class.forName(datasetModel);
			IDataSet dataset = (IDataSet) datasetClass.newInstance();
			return dataset;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * create dataset model
	 * 
	 * @param datasetModel
	 * @return
	 */
	public static IMethod createMethodModel(String methodModel) {
		if (methodModel == null) throw new RuntimeException("method model not specified");
		
		if (!methodModel.contains(".")) {
			methodModel = "edu.oregonstate.method." + methodModel;
		}
		
		try {
			Class methodClass = Class.forName(methodModel);
			IMethod method = (IMethod) methodClass.newInstance();
			return method;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * create scorer according to the scoring type
	 * 
	 * @param type
	 * @return
	 */
	public static CorefScorer createCorefScorer(ScoreType type) {
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
        
        return score;
	}
	
}
