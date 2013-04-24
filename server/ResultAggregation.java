package edu.oregonstate.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;

import edu.oregonstate.dataset.TopicGeneration;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.util.StringUtils;

/**
 * aggregate the results created by different jobs, for example
 * during the final testing, different jobs run on different topics,
 * So by aggregate the results produced by different jobs, produce the final 
 * result and output to the experiment logFile 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ResultAggregation extends ExperimentConstructor {

	/** phase, for example the second round */
	private final String phaseIndex;
	
	/** conll result */
	private final String conllResultPath;
	
	/** corefCluster */
	private final String[] COREFCLUSTER = {"goldCorefCluster", "predictedCorefCluster"};
	
	public ResultAggregation(Properties props) {
		super(props);
		
		phaseIndex = props.getProperty(EecbConstants.PHASE_PROP, "0");
		
		conllResultPath = experimentFolder + "/conll/" + phaseIndex;
	}
	
	/**
	 * perform the result aggregation
	 */
	public void performExperiment() {
		TopicGeneration topicGenerator = new TopicGeneration(experimentProps);
		
		String[] trainingTopics = topicGenerator.trainingTopics();
		calculatePerformance(trainingTopics, "trainingtopic");
		
		String[] testingTopics = topicGenerator.testingTopics();
		calculatePerformance(testingTopics, "testingtopic");
		
		String[] developmentTopics = topicGenerator.developmentTopics();
		calculatePerformance(developmentTopics, "developmenttopic");
	}
	
	/**
	 * calculate the performance on the entire set
	 * because each file is independently processed, so the result is generated independently
	 * 
	 * @param topics
	 * @param set
	 */
	private void calculatePerformance(String[] topics, String set) {
		if (topics == null) {
			return;
		}
		
		// whether the output file exist in the disk
		boolean fileExist = true;
		
		String appendPhaseIndex = "";
		if (!phaseIndex.equals("0")) {
			appendPhaseIndex = phaseIndex + "-";
		}
		for (String resultType : COREFCLUSTER) {
			String outputPath = conllResultPath + "/" + resultType + "-" + phaseIndex + "-" + set;
			for (String topic : topics) {
				String topicPath = conllResultPath + "/" + resultType + "-" + appendPhaseIndex + "" + set + "-" + topic;
				
				if (!Command.fileExists(topicPath)) {
					fileExist = false;
					break;
				}
				
				try {
					BufferedReader br = new BufferedReader(new FileReader(topicPath));
					String currentLine =  "";
					while ((currentLine = br.readLine()) != null) {
						ResultOutput.writeTextFile(outputPath, currentLine);
					}
					
					br.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		// CoNLL scoring
		if (fileExist) {
			String goldCorefCluster = conllResultPath + "/goldCorefCluster-" + phaseIndex + "-" + set;
			String predictedCorefCluster = conllResultPath + "/predictedCorefCluster-" + phaseIndex + "-" + set;
			double[] finalScores = ResultOutput.printCorpusResult(experimentLogFile, goldCorefCluster, predictedCorefCluster, "the " + phaseIndex + "'s model 's performance on " + set);
			ResultOutput.writeTextFile(experimentFolder + "/" + set + ".csv", finalScores[0] + "\t" + finalScores[1] + "\t" + finalScores[2] + "\t" + finalScores[3] + "\t" + finalScores[4]);
		}
	}
	
	public static void main(String[] args) {
		if (args.length > 1) {
			System.out.println("there are more parameters, you just can specify one path parameter.....");
			System.exit(1);
		}
		
		if (args.length == 0) {
			// run the experiment in the local machine for debugging
			args = new String[1];
			args[0] = "/nfs/guille/xfern/users/xie/Experiment/experiment/2013-04-23/0-experiment/0-resultaggregation-config.properties";
		}
		
		String[] propArgs = new String[]{"-props", args[0]};
		
		Properties props = StringUtils.argsToProperties(propArgs);
		ExperimentConstructor resultAggregator = new ResultAggregation(props);
		resultAggregator.performExperiment();
	}
	
}
