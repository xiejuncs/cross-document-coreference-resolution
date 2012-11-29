package edu.oregonstate.general;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import Jama.Matrix;

/**
 * Jun Xie(xiejuncs@gmail.com)
 */
public class MatrixOperation {
	
	/**
	 * Read a matrix from a comma sperated file
	 * 
	 * @param fileName
	 * @return
	 */
	public static Matrix readMatrix(String fileName) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			List<double[]> data_array = new ArrayList<double[]>();

			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				String fields[] = line.split(",");
				double data[] = new double[fields.length];
				for (int i = 0; i < fields.length; i++) {
					data[i] = Double.parseDouble(fields[i]);
				}
				data_array.add(data);
			}
			
			reader.close();
			if (data_array.size() > 0) {
				int cols = data_array.get(0).length;
				int rows = data_array.size();
				Matrix matrix = new Matrix(rows, cols);
				for (int r = 0; r < rows; ++r) {
					for (int c = 0; c < cols; ++c) {
						matrix.set(r, c, data_array.get(r)[c]);
					}
				}
				return matrix;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	    return new Matrix(0, 0);
	}
	
	/**
	 * the last column correspond to the target
	 * Hence, remove target values from the last column of a data set.
	 * <p>
	 * Meanwhile, we need to add 1 to 0 column of each row as a bias term 
	 * 
	 * @param data_set
	 * @return
	 */
	public static Matrix getDataPoints(Matrix data_set) {
		Matrix features = data_set.getMatrix(0, data_set.getRowDimension() - 1, 0, data_set.getColumnDimension() - 2);
		int rows = features.getRowDimension();
		int cols = features.getColumnDimension() + 1;
		Matrix modifiedFeatures = new Matrix(rows, cols);
		for (int r = 0; r < rows; ++r) {
			for (int c = 0; c < cols; ++c) {
				if (c == 0) {
					modifiedFeatures.set(r, c, 1.0);
				} else {
					modifiedFeatures.set(r, c, features.get(r, c-1));
				}
			}
		}
		return modifiedFeatures;
	}
	
	/**
	 * Returns the target values from the last column of a data set.
	 * 
	 * @param data_set
	 * @return
	 */
	public static Matrix getTargets(Matrix data_set) {
	    return data_set.getMatrix(0, data_set.getRowDimension() - 1, data_set.getColumnDimension() - 1, data_set.getColumnDimension() - 1);
	}
	
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
	
	/** 
	 * get average matrix
	 * 
	 * @param averageWeight
	 * @param wholeSearchStep
	 * @return
	 */
	public static Matrix getAverageMatrix (Matrix averageWeight, int wholeSearchStep) {
		Matrix matrix = new Matrix(averageWeight.getRowDimension(), 1);
		for (int i = 0; i < averageWeight.getRowDimension(); i++) {
			matrix.set(i, 0, averageWeight.get(i, 0) / wholeSearchStep);
		}
		return matrix;
	}
	
	/**
	 * matrix normalization
	 * 
	 * @param weight
	 */
	public static Matrix normalization(Matrix weight) {
		double sum = weight.norm2();
		
		if (sum == 0.0) return weight;
		
		for (int i = 0; i < weight.getRowDimension(); i++){
			double value = weight.get(i, 0);
			weight.set(i, 0, value / sum);
		}
		
		return weight;
	}

}
