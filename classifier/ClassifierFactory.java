package edu.oregonstate.classifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.dataset.TopicGeneration;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.EecbConstants;
import edu.oregonstate.util.EecbConstructor;
import edu.stanford.nlp.util.StringUtils;

/**
 * Run Classification Method given the Data Path
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ClassifierFactory extends ExperimentConstructor {
	
	/** training topics */
	private final String[] trainingTopics;
	
	/** classifier */
	private final IClassifier classifier;
	
	/** phase, for example the second round */
	private final String phaseIndex;
	
	public ClassifierFactory(Properties props) {
		super(props);
		
		// get training topics
		TopicGeneration topicGenerator = new TopicGeneration(props);
		trainingTopics = topicGenerator.trainingTopics();
		
		// build a classifier
		classifier = EecbConstructor.createClassifier(props.getProperty(EecbConstants.CLASSIFIER_METHOD, "StructuredPerceptron"));
		
		phaseIndex = props.getProperty(EecbConstants.PHASE_PROP, "0");
	}
	
	/**
	 * perform the experiment
	 */
	public void performExperiment() {
		List<String> paths = getPaths();
		
		ResultOutput.writeTextFile(experimentFolder + "/searchstep", "" + paths.size());
		ResultOutput.writeTextFile(experimentLogFile, "the total number of training files : " + paths.size());
		
		Parameter returnPara = classifier.train(paths, Integer.parseInt(phaseIndex));
		ResultOutput.writeTextFile(experimentLogFile, "\n\nThe " + phaseIndex + "'s learned model \n");
		ResultOutput.printParameter(returnPara, experimentLogFile);
		
		// output 
		double[] averageWeight = returnPara.generateWeightForTesting();
		String outputFile = experimentFolder + "/model/model" + phaseIndex;
		String outputString = ResultOutput.printStructredModel(averageWeight, FeatureFactory.getFeatureTemplate());
		ResultOutput.writeTextFile(outputFile, outputString);
	}
	
	/**
	 * get the path of training data
	 * 
	 * @param j
	 * @return
	 */
	private List<String> getPaths() {
		List<String> filePaths = new ArrayList<String>();
		filePaths.addAll(getPaths(trainingTopics));

		return filePaths;
	}
	
	/**
	 * aggregate the training data
	 * 
	 * @param topics
	 * @param j
	 * @return
	 */
	private List<String> getPaths(String[] topics) {
		List<String> allfiles  = new ArrayList<String>();
		for (String topic : topics) {
			List<String> files = getDivisionPaths(topic);
			String topicPath = experimentFolder + "/" + topic + "/data/";
			List<String> filePaths = new ArrayList<String>();
			for (String file : files) {
				filePaths.add(topicPath + file);
			}

			allfiles.addAll(filePaths);
		}

		return allfiles;
	}

	// get a sequence of data file, such as 1, 2, 3, 4, 5
	private List<String> getDivisionPaths(String topic) {
		String topicPath = experimentFolder + "/" + topic + "/data/";
		List<String> files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));

		return files;
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
		ExperimentConstructor classifier = new ClassifierFactory(props);
		classifier.performExperiment();
	}

}
