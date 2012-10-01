package edu.oregonstate.util;

import Jama.Matrix;

public class MatrixOperation {

	/**
	 * divide the the averageModel by the number of mEpoch * number of topic
	 * 
	 * @param averageModel
	 * @param mEpoch
	 * @return
	 */
	public static Matrix divide(Matrix averageModel, int mEpoch) {
		for (int i = 0; i < averageModel.getRowDimension(); i++) {
			double updateValue = averageModel.get(i, 0) / mEpoch;
			averageModel.set(i, 0, updateValue);
		}
		
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
	public static Matrix addWeight(Matrix model, Matrix averageModel) {
		for (int i = 0; i < averageModel.getRowDimension(); i++) {
			double updateValue = averageModel.get(i, 0) + model.get(i, 0);
			averageModel.set(i, 0,  updateValue);
		}
		
		return averageModel;
	}
}
