package edu.oregonstate.experiment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.EecbConstants;

/**
 * Process the data to generate the constraints for SVM-ranking.
 * {@link http://www.cs.cornell.edu/people/tj/svm_light/svm_rank.html} 
 * 
 * The preference is specified according to the target value:
 * 3 qid:1 1:1 2:1 3:0 4:0.2 5:0 # 1A
 * 2 qid:1 1:0 2:0 3:1 4:0.1 5:1 # 1B
 * 1A should be ranked higher than 1B according to the heuristic function
 * 
 * In our case, we just need to give a high value to good state, and then 
 * give a low value to bad state. Because we do not distinguish the rank among 
 * the bad states, the values of bad states are all same. In our task, we assign 2
 * to good states and 1 to bad states.
 * 
 * NOTE : 
 * 1. the lines in the input files have to be sorted by increasing qid.
 * 2. Feature/value pairs MUST be ordered by increasing feature number.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ProcessDataSVM {

	// training topics
	private final String[] mTrainingTopics;
	
	// data path is the experiment path
	private final String mDataPath;
	
	// training set file name
	private final String mFileName;
	
	// no of qid
	private int noOfQID;
	
	public ProcessDataSVM(String dataPath, String fileName) {
		mDataPath = dataPath;
		String[] experimentTopics = EecbConstants.stanfordTotalTopics;
		int index = 12;
		mTrainingTopics = splitTopics(index, experimentTopics);
		noOfQID = 0;
		mFileName = fileName;
	}
	
	/**
	 * according to feature templates to generate data
	 * 1. qid should be positive 
	 * 2. # is a comment
	 * 3. feature id should be increased
	 */
	public void processGeneratedData() {		
		System.out.println("the number of data path : " + mTrainingTopics.length + "\n");
		
		ResultOutput.deleteFile(mFileName);
		String outputPath = mFileName;
		
		// each topic is a unit
		for (String topic : mTrainingTopics) {
			String path = mDataPath + "/" + topic + "/data/1000";
			System.out.println(topic + " : " + path + "\n");
			
			List<String> dataset = readTextData(path);
			
			int topicID = Integer.parseInt(topic) * 1000;
			List<String> records = generateOutput(dataset, topicID);
			writeRawinText(records, outputPath);
		}
		
		System.out.println("done " + noOfQID);
	}
	
	private void writeRawinText(List<String> records, String path) {
		File file = new File(path);
		try {
			FileWriter writer = new FileWriter(file, true);
			for (String record: records) {
				writer.write(record);
				writer.write("\n");
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private List<String> generateOutput(List<String> dataset, int topicID) {
		List<String> records = new ArrayList<String>();
		int actionIndex = 0;
		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
		int featureSize = featureTemplate.size();
		
		for (String data : dataset) {
			if (data.equals("NEWDATASET")) {
				actionIndex += 1;
				noOfQID += 1;
				records.add("# query " + (topicID + actionIndex));
				continue;
			}
			
			int qid = topicID + actionIndex;
			double[] feature = generateFeature(data, featureSize);
			int targetValue = 1;
			
			if (data.startsWith("G")) {
				targetValue = 2;
			}
			
			String record = generateString(targetValue, qid, feature);
			
			records.add(record);
		}
		
		return records;
	}
	
	private String generateString(int targetValue, int qid, double[] feature) {
		StringBuilder sb = new StringBuilder();
		sb.append(targetValue + " ");
		sb.append("qid:" + qid + " ");
		for (int index = 0; index < feature.length; index++) {
			double value = feature[index];
			if (value > 0) {
				sb.append((index + 1) + ":" + value + " ");
			}
		}
		
		if (targetValue == 2) {
			sb.append("# g");
		}
		
		return sb.toString().trim();
	}
	
	/**
	 * generate feature from sparse feature representation
	 * 
	 * @param data
	 * @param featureSize
	 * @return
	 */
	private double[] generateFeature(String data, int featureSize) {
		String[] elements = data.split("\t");
		double[] feature = new double[featureSize];
		if (elements.length != 1) {
			
			for (int i = 1; i < elements.length; i++) {
				String featureString = elements[i];
				String[] featureStringDetail = featureString.split(":");
				int index = Integer.parseInt(featureStringDetail[0]);
				double value = Double.parseDouble(featureStringDetail[1]);
				feature[index] = value;
			}
			
		}
		
		return feature;
	}
	
	/**
	 * read data
	 * 	
	 * @param path
	 * @return
	 */
	private List<String> readTextData(String path) {
		List<String> dataset = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				
				dataset.add(line);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return dataset;
	}
	
	
	/**
	 * Split the topics based on the index, return the first index topics
	 * 
	 * @param index
	 * @param topics
	 * @return
	 */
	private String[] splitTopics(int index, String[] topics) {
		String[] splitTopics = new String[index];
		
		for (int i = 0; i < topics.length; i++) {
			if (i < index) {
				splitTopics[i] = topics[i];
			}
		}
		
		return splitTopics;
	}
	
	/**
	 * given the data path and the training topics, extract the constraints for each topic
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String dataPath = "/scratch/JavaFile/result/2013/201302/20130218/Sun-Feb-17-22-47-42-PST-2013-trp-tep-f-os-BeamSearch-eS-StructuredPerceptron-20-AROWOnline-0.5-unnormalize-PACons-none-trP-teP-Pairwise";
		String fileName = "predictedCorefTrain.dat";
		
		ProcessDataSVM dataProcess = new ProcessDataSVM(dataPath, fileName);
		dataProcess.processGeneratedData();
	}
}
