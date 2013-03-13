package edu.oregonstate.experiment;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.oregonstate.method.IMethod;
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
	public void performExperiment() {
		// 1. generate dataset
//		DatasetFactory dataset = new DatasetFactory();
//		dataset.generateDataSet();
		
		// 2. do search and training
//		boolean searchTraining = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DOTRAINING_PROP, "true"));
//		List<Parameter> finalParas = null;
//		if (searchTraining) {
//			String methodName = experimentProps.getProperty(EecbConstants.METHOD_PROP, "Dagger");
//			IMethod method = EecbConstructor.createMethodModel(methodName);
//			finalParas = method.executeMethod();
//		}
		
//		// 3. enable the learned weight to do testing on training set and testing set
//		boolean existedWeight = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.EXISTEDWEIGHT_PROP, "false"));
//		if (existedWeight) {			
//			// whether do post process on the document
//			boolean postProcess = ExperimentConstructor.postProcess;
//			
//			double[] learnedWeight = new double[FeatureFactory.getFeatureTemplate().size()];
//			// "/nfs/guille/xfern/users/xie/Experiment/corpus/learnedweight.txt"
//			String path = experimentProps.getProperty(EecbConstants.EXISTEDWEIGHT_PROP);
//			Map<String, String> datas = ResultOutput.readFiles(path, ":");
//			learnedWeight = new double[FeatureFactory.getFeatureTemplate().size()];
//			for (String key : datas.keySet()) {
//				int arrayIndex = Integer.parseInt(key);
//				double value = Double.parseDouble(datas.get(key));
//				learnedWeight[arrayIndex - 1] = value;
//			}
//			for (int i = 0; i < learnedWeight.length; i++) {
//				ResultOutput.writeTextFile(logFile, FeatureFactory.getFeatureTemplate().get(i) + " : " + learnedWeight[i]);
//			}
//			
//			ResultOutput.writeTextFile(logFile, "learned weight vector : " + DoubleOperation.printArray(learnedWeight) + ", the length is " + learnedWeight.length);
//			int modelIndex = Integer.parseInt(experimentProps.getProperty(EecbConstants.METHOD_EPOCH_PROP, "1")) + 1;
//			int iterationIndex = 1;
//			double stoppingRate = ExperimentConfigurationFactory.tuneStoppingRate(learnedWeight, iterationIndex);
//			
//		}
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
