package edu.oregonstate.experiment;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import edu.oregonstate.method.IMethod;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.oregonstate.classifier.Parameter;
import edu.oregonstate.dataset.DatasetFactory;
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
	 * 
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

			File source = new File(corpusPath + "corpus/documentobject");
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
		String methodName = experimentProps.getProperty(EecbConstants.METHOD_PROP, "Dagger");
		IMethod method = EecbConstructor.createMethodModel(methodName);
		List<Parameter> finalParas = method.executeMethod();
		
		// do final testing
		boolean debug = Boolean.parseBoolean(experimentProps.getProperty(EecbConstants.DEBUG_PROP, "false"));
		if (!debug) {
			
		}
		
		// remove document object
		ResultOutput.deleteResult(experimentResultFolder + "/documentobject");
	}
	
	
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
