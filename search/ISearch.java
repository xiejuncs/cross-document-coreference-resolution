package edu.oregonstate.search;

import edu.oregonstate.classifier.Parameter;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;

/**
 * search interface, all search method need to implement this interface
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public interface ISearch {
	
	/* learn weight according to the parameter, and then print training file into phase */
	public Parameter trainingBySearch(Document document, Parameter para, String phase);
	
	/* apply the learned weight to the testing document, and return the best loss state, later, we can output a terminate state for final performance */
	public State<CorefCluster> testingBySearch(Document document, double[] weight, String phase, boolean outputFeature, double stoppingrate);
}