package edu.oregonstate.training;

import edu.oregonstate.EventCoreference;
import edu.oregonstate.search.BestBeamSearch;
import edu.stanford.nlp.dcoref.Document;

import Jama.Matrix;

/**
 * train heuristic function: One learning heuristic functions for co-reference resolution
 * 
 * Initialize weights of heuristic function w=0
 *		repeat the following for several iterations
		for each training example (x,y)
		repeat 
			Perform a search step
			If there is a search error
				Update weights of heuristic function
			Reset the beam
			end if
		until search uncovers the true output y (terminal node)
	end for
	Compute the averaged weight vector
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class TrainHeuristicFunction {

	// topic in the corpus
	private String[] mTopic;
	
	// provide the static functions which is common
	private EventCoreference ec;
	
	// number of iterations
	private int mEpoch;
	
	public TrainHeuristicFunction(EventCoreference ec, String[] topic, int epoch) {
		this.ec = ec;
		mTopic = topic;
		mEpoch = epoch;
		Train.currentOutputFileName = "TrainHeuristicFunction";
	}
	
	// train the model using search
	public Matrix train(Matrix initialMatrix) {
		Matrix model = initialMatrix;
		Matrix averageModel = initialMatrix;
		for (int i = 0; i < mEpoch; i++) {
			System.out.println("Start train the model:"+ i +"th iteration ============================================================");
			for (String topic : mTopic) {
				System.out.println("begin to process topic" + topic+ "................");
				try {
					Document document = ec.getDocument(topic);
				    ec.corefSystem.coref(document);
				    
				    BestBeamSearch bbs = new BestBeamSearch(document, ec.corefSystem.dictionaries(), model);
				    Matrix weight = bbs.train();
				    averageModel = addWeight(weight, averageModel);
				    
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
				
				System.out.println("end to process topic" + topic+ "................");
			}
			
			System.out.println("End train the model:"+ i +"th iteration ============================================================");
		}
		
		averageModel = divide(averageModel, mEpoch * mTopic.length);
		
		return averageModel;
	}
	
	
	
	/**
	 * add model to the averageModel
	 * <b>NOTE</b>
	 * model and averageModel are both column vector
	 * 
	 * @param model
	 * @param averageModel
	 * @return
	 */
	public Matrix addWeight(Matrix model, Matrix averageModel) {
		for (int i = 0; i < averageModel.getRowDimension(); i++) {
			double updateValue = averageModel.get(i, 0) + model.get(i, 0);
			averageModel.set(i, 0,  updateValue);
		}
		
		return averageModel;
	}
	
	/**
	 * divide the the averageModel by the number of mEpoch * number of topic
	 * 
	 * @param averageModel
	 * @param mEpoch
	 * @return
	 */
	public Matrix divide(Matrix averageModel, int mEpoch) {
		for (int i = 0; i < averageModel.getRowDimension(); i++) {
			double updateValue = averageModel.get(i, 0) / mEpoch;
			averageModel.set(i, 0, updateValue);
		}
		
		return averageModel;
	}

}
