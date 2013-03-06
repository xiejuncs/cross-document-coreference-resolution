package edu.oregonstate.general;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;

/**
 * calculate the final output for each testing file
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FinalScore {

	private String[] mtrainingTopics;
	
	private String[] mtestingTopics;
	
	private String mcorpusPath;
	
	private int mIteration;
	
	private String mscoreType;
	
	public void set (int iteration) {
		mIteration = iteration;
	}
	
	public void setScoreType(String scoreType) {
		mscoreType = scoreType;
	}
	
	/**
	 * calculate the final performance of the specific experiment
	 * 
	 * @param trainingTopics
	 * @param testingTopics
	 * @param corpusPath
	 * @param iteration
	 */
	public FinalScore(String[] trainingTopics, String[] testingTopics, String corpusPath) {
		mtrainingTopics = trainingTopics;
		mtestingTopics = testingTopics;
		mcorpusPath = corpusPath + "/";
	}
	
	/**
	 * calculate the final performance,
	 * 
	 * @return each element contains a double array: f score, precision, recall
	 */
	public void computePerformance() {
		calculateTraining(mtrainingTopics, "train");
		calculateTesting(mtestingTopics, "test");
	}
	
	public void calculateTesting(String[] topics, String mode) {
		List<List<String>> datas = new ArrayList<List<String>>();
		int maximum = Integer.MIN_VALUE;
		String outputPath = mcorpusPath + "finalresult/" + mode + "-" + mscoreType + "-" + mIteration;
		for (String topic : topics) {
			String topicPath = mcorpusPath + topic + "/" + mscoreType + "-" + mode + "-iteration" + mIteration + "-" + topic + "-scoredetail";
			List<String> data = readData(topicPath);
			int size = data.size();
			if (size > maximum) {
				maximum = size;
			}
			datas.add(data);
		}
		
		List<List<double[]>> numericDatas = convertData(datas, maximum);
		
		for (int i = 0; i < maximum; i++) {
			List<double[]> records = new ArrayList<double[]>();
			for (List<double[]> numericData: numericDatas) {
				records.add(numericData.get(i));
			}
			
			double precisionNum = 0.0;
			double precisionDen = 0.0;
			double recallNum = 0.0;
			double recallDen = 0.0;
			
			for (double[] record : records) {
				precisionNum += record[0];
				precisionDen += record[1];
				recallNum += record[2];
				recallDen += record[3];
			}
			
			double precision = precisionNum / precisionDen;
			double recall = recallNum / recallDen;
			
			double f = (2 * precision * recall) / (precision + recall);
			
			String result = f + " " + precision + " " + recall;
			ResultOutput.writeTextFile(outputPath, result);
		}
	}
	
	/**
	 * calculate each iteration's total performance, and sum them together
	 * 
	 * @param topic
	 * @param mode
	 */
	public void calculateTraining(String[] topics, String mode) {
		List<List<String>> datas = new ArrayList<List<String>>();
		int maximum = Integer.MIN_VALUE;
		String outputPath = mcorpusPath + "finalresult/" + mode + "-" + mIteration;
		for (String topic : topics) {
			String topicPath = mcorpusPath + topic + "/" + mode + "-iteration" + mIteration + "-" + topic + "-scoredetail";
			List<String> data = readData(topicPath);
			int size = data.size();
			if (size > maximum) {
				maximum = size;
			}
			datas.add(data);
		}
		
		List<List<double[]>> numericDatas = convertData(datas, maximum);
		
		for (int i = 0; i < maximum; i++) {
			List<double[]> records = new ArrayList<double[]>();
			for (List<double[]> numericData: numericDatas) {
				records.add(numericData.get(i));
			}
			
			double precisionNum = 0.0;
			double precisionDen = 0.0;
			double recallNum = 0.0;
			double recallDen = 0.0;
			
			for (double[] record : records) {
				precisionNum += record[0];
				precisionDen += record[1];
				recallNum += record[2];
				recallDen += record[3];
			}
			
			double precision = precisionNum / precisionDen;
			double recall = recallNum / recallDen;
			
			double f = (2 * precision * recall) / (precision + recall);
			
			String result = f + " " + precision + " " + recall;
			ResultOutput.writeTextFile(outputPath, result);
		}

	}
	
	/**
	 * convert string to double[]
	 * 
	 * @param datas
	 * @return
	 */
	private List<List<double[]>> convertData(List<List<String>> datas, int maximum) {
		List<List<double[]>> numericDatas = new ArrayList<List<double[]>>();
		
		for (List<String> data : datas) {
			List<double[]> numericData = new ArrayList<double[]>();
			converted(data, maximum);
			for (String record : data) {
				String[] elements = record.split(" ");
				double[] numericElements = new double[elements.length];
				for (int i = 0; i < elements.length; i++) {
					numericElements[i] = Double.parseDouble(elements[i]);
				}
				numericData.add(numericElements);
			}
			
			numericDatas.add(numericData);
		}
		
		return numericDatas;
	}
	
	private void converted(List<String> data, int maximum) {
		int size = data.size();
		for (int i = 0; i < maximum - size; i++) {
			data.add(data.get(size - 1));
		}
	}
	
	/**
	 * read data according to path
	 * 
	 * @param path
	 * @return
	 */
	private List<String> readData(String path) {
		List<String> data = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				data.add(currentLine);
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return data;
	}
	
	public static void main(String[] args) {
		int iteration = 10;
		String[] trainingTopics = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
		String[] testingTopics = {"13", "14", "16", "18", "19",  "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", 
								  "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45"};
//		String[] trainingTopics = {"16", "38"};
//		String[] testingTopics = {"3", "20"};
		
		String[] scoreTypes = {"Pairwise", "MUC", "Bcubed", "CEAF"};
		
		
		String corpusPath = "/nfs/guille/xfern/users/xie/Experiment/corpus/TEMPORYRESUT/Thu-Nov-15-16:46:51-PST-2012CrossCoreferenceResolutionGold2.5";
		String finalResultPath = corpusPath + "/finalresult";
		Command.mkdir(finalResultPath);
		FinalScore finalScore = new FinalScore(trainingTopics, testingTopics, corpusPath);
		for (int i = 1; i <= iteration; i++) {
			for (String scoreType : scoreTypes) {
				finalScore.set(i);
				finalScore.setScoreType(scoreType);
				finalScore.computePerformance();
			}
		}
	}
	
}
