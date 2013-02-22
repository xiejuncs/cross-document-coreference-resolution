package edu.oregonstate.experiment;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;

/**
 * generate the configuration file
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ExperimentGeneration {

	private final String mFolderName;

	private final String mFolderPath;

	//private final String[] iterations = {"200"};

	/* train method */
	//private final String[] trainMethods = {"Online", "OnlineToBatch", "Batch"};
	//private final String[] trainMethods = {"OnlineToBatch", "Online", "PAOnline", "PAOnlineToBatch"};
	private final String[] trainMethods = {"AROWOnlineToBatch"};

	//private final String[] startRates = {"0.1", "0.05", "0.02", "0.01"};

	/* mention type */
	private final String[] mentionTypes = {"gold", "predicted"};
	// private final String[] mentionTypes = {"predicted"};

	/* enable stanford pre-process */
	//private final String[] enableStanfordPreprocess = {"enable", "disable"};
	private final String[] enableStanfordPreprocess = {"enable"};
	
	/* learning rate type */
	private final String[] learningRateTypes = {"constant"};
	//private final String[] learningRateTypes = {"fixed", "constant", "lossscore"};

	/* normalize weight or not */
	private final String[] normalizedWeights = {"normalize"};
	
	private final String[] hyperParameters = {"00001", "0001", "001", "01", "1", "10", "100", "1000"};

	/* stopping criterion */
	private final String[] stoppingCritetions = {"none"};

	public ExperimentGeneration (String path, String folderName) {
		mFolderPath = path + folderName;
		mFolderName = folderName;
	}

	public void generateExperimentFiles() {
		// generate the main folder
		generateMainFolder();

		generateSubFolders();		
	}

	private void generateMainFolder() {
		Command.createDirectory(mFolderPath);
	}

	private void generateSubFolders() {
		for (String mentionType : mentionTypes) {
			for (String enableStanford : enableStanfordPreprocess) {
				for (String trainMethod : trainMethods) {
					for (String learnRateType : learningRateTypes) {
						for (String normalizeWeight : normalizedWeights) {
							for (String stoping : stoppingCritetions) {
								for (String hyperParameter : hyperParameters) {
									String subFolderPath = mFolderPath + "/" + mentionType + "-" + enableStanford + "-" + trainMethod + "-" + 
											               learnRateType + "-" + normalizeWeight + "-" + stoping + "-" + hyperParameter;
									Command.createDirectory(subFolderPath);

									String prefix = mentionType + "-" + enableStanford + "-" + trainMethod + "-" + 
									               learnRateType + "-" + normalizeWeight + "-" + stoping + "-" + hyperParameter;
									generateConfigurationFile(subFolderPath, mentionType, enableStanford, trainMethod, learnRateType, normalizeWeight, stoping, hyperParameter);

									generateRunFile(subFolderPath, prefix);

									generateSimpleFile(subFolderPath, prefix);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * generate config.properties
	 * 
	 * @param subFolderPath
	 * @param method
	 * @param startRate
	 */
	private void generateConfigurationFile(String subFolderPath, String mentionType, String enableStanford, 
			String trainMethod, String learnRateType, String normalizeWeight, String stoping, String hyperParameter) {
		String configPath = subFolderPath + "/config.properties";
		StringBuilder sb = new StringBuilder();
		sb.append("dcoref.dataset.generation = false\n\n");
		sb.append("dcoref.searchtraining = false\n\n");
		sb.append("dcoref.annotators = tokenize, ssplit, pos, lemma, ner, parse, dcoref\n\n");
		sb.append("dcoref.dataset = true\n\n");
		sb.append("dcoref.classifier = StructuredPerceptron\n");
		sb.append("dcoref.classifier.epoch = 20\n");
		sb.append("dcoref.enablepreviouscurrentconstraint = false\n\n");
		sb.append("dcoref.costfunction = LinearCostFunction\n\n");
		sb.append("dcoref.lossfunction = MetricLossFunction\n");
		sb.append("dcoref.lossfunction.score = Pairwise\n\n");
		sb.append("dcoref.search = BeamSearch\n");
		sb.append("dcoref.search.beamwidth = 1\n");
		sb.append("dcoref.search.maximumstep = 600\n\n");

		// specify the mention type
		if (mentionType.equals("gold")) {
			sb.append("dcoref.train.gold = true\n");
			sb.append("dcoref.test.gold = true\n\n");
			sb.append("dcoref.train.postprocess = false\n");
			sb.append("dcoref.test.postprocess = false\n\n");
		} else {
			sb.append("dcoref.train.gold = false\n");
			sb.append("dcoref.test.gold = false\n\n");
			sb.append("dcoref.train.postprocess = true\n");
			sb.append("dcoref.test.postprocess = true\n\n");
		}

		sb.append("dcoref.debug = false\n\n");
		sb.append("dcoref.sievePasses = partial\n\n");
		sb.append("dcoref.training.testing = true\n\n");
		sb.append("dcoref.weight = true\n\n");
		sb.append("dcoref.stoppingcriterion = " + stoping + "\n\n");
		sb.append("dcoref.method.epoch = 1\n");
		sb.append("dcoref.method.function.number.prop = 1\n\n");

		sb.append("dcoref.training.style = " + trainMethod + "\n");

		String normaWeight = "true";
		if (normalizeWeight.equals("unnormalize")) {
			normaWeight = "false";
		}
		sb.append("dcoref.pa.normalize.weight = " + normaWeight + "\n\n");

		String stanfordPreprocess = "true";
		if (enableStanford.equals("disable")) {
			stanfordPreprocess = "false";
		}
		sb.append("dcoref.enable.stanford.processing.during.data.generation = " + stanfordPreprocess + "\n\n");

		if (!learnRateType.equals("fixed")) {
			sb.append("dcoref.pa.learning = true\n");
			String lossScore = "true";
			if (learnRateType.equals("constant")) {
				lossScore = "false";
			}
			sb.append("dcoref.pa.learning.rate.lossscore = " + lossScore + "\n\n");
		} else {
			sb.append("dcoref.structuredperceptron.startrate = 0.02\n\n");
		}
		
		// enable output the best state score or just the normal search score with stopping criterion
		sb.append("\ndcoref.best.search.score = true\n");
		
		// enable post process gold cluster
		sb.append("\ndcoref.gold.cluster.post.process = false\n");
		
		// enable state feature
		sb.append("\ndcoref.state.feature = false\n");
		
		sb.append("dcoref.experiment.hyperparameter = " + hyperParameter + "\n\n");
		
		sb.append("dcoref.just.tune.parameter = true\n\n");
		
		sb.append("dcoref.enable.learned.weight = true\n");
		sb.append("dcoref.learned.weight.path = /nfs/guille/xfern/users/xie/Experiment/corpus/svm-rank-weight/weight" + hyperParameter );
		
		ResultOutput.writeTextFile(configPath, sb.toString().trim());
	}

	private void generateRunFile(String subFolderPath, String prefix) {
		String runPath = subFolderPath + "/run.sh";
		String clusterPath = "/nfs/guille/xfern/users/xie/Experiment/experiment/" + mFolderName + "/" + prefix;
		String command = "java -Xmx8g -jar /nfs/guille/xfern/users/xie/Experiment/jarfile/coreference-resolution.jar " + clusterPath + "/config.properties";
		ResultOutput.writeTextFile(runPath, command);
	}

	private void generateSimpleFile(String subFolderPath, String prefix) {
		String simplePath = subFolderPath + "/simple.sh";
		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/csh\n\n");
		sb.append("# Give the job a name\n");
		sb.append("#$ -N Jun-coreference-resolution-" + prefix + "\n");
		sb.append("# set working directory on all host to\n");
		sb.append("# directory where the job was started\n");
		sb.append("#$ -cwd\n\n");

		String clusterPath = "/nfs/guille/xfern/users/xie/Experiment/experiment/" + mFolderName + "/" + prefix;
		sb.append("# send all process STDOUT (fd 2) to this file\n");
		sb.append("#$ -o " + clusterPath + "/screencross.txt\n\n");

		sb.append("# send all process STDERR (fd 3) to this file\n");
		sb.append("#$ -e " + clusterPath + "/job_outputcross.err\n\n");

		sb.append("# specify the hardware platform to run the job on.\n");
		sb.append("# options are: amd64, em64t, i386, volumejob (use i386 if you don't care)\n");
		sb.append("#$ -q eecs,eecs1,eecs2,share\n\n");

		sb.append("# Commands\n");
		sb.append(clusterPath + "/run.sh\n");
		ResultOutput.writeTextFile(simplePath, sb.toString().trim());

	}

	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/experiment/";

		//get current date time with Date()
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String folderName = dateFormat.format(date);
		//String folderName = "2013-01-29";

		ExperimentGeneration generator = new ExperimentGeneration(path, folderName);
		generator.generateExperimentFiles();
	}

}
