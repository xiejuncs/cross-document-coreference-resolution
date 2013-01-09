package edu.oregonstate.experiment.dataset;

import edu.oregonstate.util.EecbConstructor;

/**
 * Create a series of Document objects which are serialized
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class DatasetFactory {

	/* whether put all documents of a topic together, true CrossTopic, false WithinCross */
	public static IDataSet createDataSet(boolean dataSetMode) {
		IDataSet mDatasetMode;
		
		// initialize the DatasetMode
		if (dataSetMode) {
			mDatasetMode = EecbConstructor.createDataSetModel("CrossTopic");
		} else {
			mDatasetMode = EecbConstructor.createDataSetModel("WithinCross");
		}
		
		return mDatasetMode;
	}
	
}
