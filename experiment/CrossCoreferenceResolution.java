package edu.oregonstate.experiment;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import edu.oregonstate.method.Dagger;
import edu.oregonstate.method.IMethod;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.dataset.DatasetFactory;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.stanford.nlp.util.StringUtils;

/**
 * cross coreference resolution
 * 
 * @author Jun Xie (xiejuncs@gmail.com)
 *
 */
public class CrossCoreferenceResolution extends ExperimentConstructor {
	
	/**
	 * set experiment properties
	 * 
	 * @param props
	 */
	public CrossCoreferenceResolution(Properties props) {
		super(props);
	}

	/**
	 * perform the cross coreference resolution experiment
	 */
	protected void performExperiment() {
		// copy from existing serialized folder
		boolean dataSetCopy = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DATASET_GENERATION_PROP, "true"));
		
		// 1. generate dataset
		if (dataSetCopy) {
			for (String topic : trainingTopics) {
				String path = experimentResultFolder + "/" + topic;
				Command.createDirectory(path);
			}

			for (String topic : testingTopics) {
				String path = experimentResultFolder + "/" + topic;
				Command.createDirectory(path);
			}
			
			boolean trainGoldOnly = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.TRAIN_GOLD_PROP, "true"));
			String gold = trainGoldOnly ? "gold" : "predicted";

			File source = new File(corpusPath + "corpus/" + gold +"documentobject");
			String serializedOutput = experimentResultFolder + "/documentobject";
			Command.createDirectory(serializedOutput);
			File des = new File(serializedOutput);
			try {
				FileUtils.copyDirectory(source, des);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			DatasetFactory datasetFactory = new DatasetFactory();
			datasetFactory.generateDataSet();
		}
		
		// 2. do search and training
		boolean searchTraining = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.SEARCH_TRAINING_PROP, "true"));
		List<Parameter> finalParas = null;
		if (searchTraining) {
			String methodName = experimentProps.getProperty(EecbConstants.METHOD_PROP, "Dagger");
			IMethod method = EecbConstructor.createMethodModel(methodName);
			finalParas = method.executeMethod();
		}
		
		// 3. enable the learned weight to do testing on training set and testing set
		boolean enableLearnedWeight = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.ENABLE_LEARNED_WEIGHT, "false"));
		if (enableLearnedWeight) {
			// (1) if we have done search and training, then we can use the learned weight to do testing
			// on training set and testing set.
			// (2) if we have not done search and training, then we can use the existing learned weight
			// to do testing on training set and testing set
			//int index = 0;
			Dagger dagger = new Dagger();
			boolean justTuneParameter = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.JUST_TUNE_PARAMETER, "false"));
			
			// whether do post process on the document
			boolean trainPostProcess = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.TRAIN_POSTPROCESS_PROP, "false"));
			boolean testPostProcess = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.TEST_POSTPROCESS_PROP, "false"));
			double[] learnedWeight = null;
			if (finalParas == null) {
				// "/nfs/guille/xfern/users/xie/Experiment/corpus/learnedweight.txt"
				String path = experimentProps.getProperty(EecbConstants.LEARNED_WEIGHT_PATH);
				Map<String, String> datas = ResultOutput.readFiles(path, ":");
				learnedWeight = new double[FeatureFactory.getFeatureTemplate().length];
				for (String key : datas.keySet()) {
					int arrayIndex = Integer.parseInt(key);
					double value = Double.parseDouble(datas.get(key));
					learnedWeight[arrayIndex - 1] = value;
				}
				
//				List<String> weights = ResultOutput.readFiles(path);
//				String weight = weights.get(index);
//				learnedWeight = DoubleOperation.transformString(weight, ",");
				
				
			} else {
				
			}
			
			ResultOutput.writeTextFile(logFile, "learned weight vector : " + DoubleOperation.printArray(learnedWeight) + ", the length is " + learnedWeight.length);
			int modelIndex = Integer.parseInt(experimentProps.getProperty(EecbConstants.METHOD_EPOCH_PROP, "1")) + 1;
			int iterationIndex = 1;
			double stoppingRate = dagger.tuneStoppingRate(learnedWeight, modelIndex, iterationIndex);
			
			// do testing
			if (justTuneParameter) {
				dagger.testDocument(developmentTopics, learnedWeight, modelIndex, iterationIndex, trainPostProcess, "final-validation", false, stoppingRate);
			} else {
				dagger.testDocument(trainingTopics, learnedWeight, modelIndex, iterationIndex, trainPostProcess, "final-training", false, stoppingRate);
				dagger.testDocument(testingTopics, learnedWeight, modelIndex, iterationIndex, testPostProcess, "final-testing", false, stoppingRate);
			}
			
		}
	
		// remove document object
		// 4. remove the document object
		// if data-set copy, then remove,
		// if data-set generated, then not remove
		if (dataSetCopy) {
			ResultOutput.deleteResult(experimentResultFolder + "/documentobject");
		}
	}
	
	/**
	 * The main entry point of the experiment
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 1) {
			 System.out.println("there are more parameters, you just can specify one path parameter.....");
             System.exit(1);
		}
		
		if (args.length == 0) {
			// run the experiment in the local machine for debugging
			args = new String[1];
			args[0] = "../corpus/config.properties";
		}
		
		String[] propArgs = new String[]{"-props", args[0]};
		
		Properties props = StringUtils.argsToProperties(propArgs);
		ExperimentConstructor experiment = new CrossCoreferenceResolution(props);
		ResultOutput.printTime(logFile, "The start of the experiment: ");
		experiment.performExperiment();
		ResultOutput.printTime(logFile, "The end of the experiment");
	}
}
