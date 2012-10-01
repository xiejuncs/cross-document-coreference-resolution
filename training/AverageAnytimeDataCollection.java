package edu.oregonstate.training;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.oregonstate.util.Constants;

/**
 * 
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class AverageAnytimeDataCollection {

	public static void main(String[] args) {
		String parameter = "30-1";
		List<String> files  = new ArrayList<String>();
		String topicPath = Constants.TEMPORY_RESULT_PATH;
		files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
		List<String> mucFiles = new ArrayList<String>();
		List<String> bcuedFiles = new ArrayList<String>();
		List<String> pairwiseFiles = new ArrayList<String>();
		List<String> ceafFiles = new ArrayList<String>();
		for (String file : files) {
			if (file.contains(parameter + "-MUC")) {
				mucFiles.add(file);
				continue;
			}
			
			if (file.contains(parameter + "-BCubed")) {
				bcuedFiles.add(file);
				continue;
			}
			
			if (file.contains(parameter + "-Pairwise")) {
				pairwiseFiles.add(file);
				continue;
			}
			
			if (file.contains(parameter + "-CEAF")) {
				ceafFiles.add(file);
				continue;
			}
		}
		
		List<List<Double>> mucScores = fillScore(mucFiles);
		List<List<Double>> bcubedScores = fillScore(bcuedFiles);
		List<List<Double>> pairwiseScores = fillScore(pairwiseFiles);
		List<List<Double>> ceafScores = fillScore(ceafFiles);
		
		List<Double> mucscore = addScore(mucScores);
		List<Double> bcubedscore = addScore(bcubedScores);
		List<Double> pairwisescore = addScore(pairwiseScores);
		List<Double> ceafscore = addScore(ceafScores);
		
		System.out.println("Done");
	}
	
	public static List<Double> addScore(List<List<Double>> scores) {
		List<Double> score = new ArrayList<Double>();
		int maxLength = 0;
		for (List<Double> sc : scores) {
			int length = sc.size();
			if (length > maxLength) maxLength = length;
		}
		
		for (int i = 0; i < maxLength; i++) {
			score.add(0.0);
		}
		
		for (List<Double> sc : scores){
			int length = sc.size();
			if (maxLength > length) {
				int difference = maxLength - length;
				double value = sc.get(length - 1);
				for (int i = 0; i < difference; i++) {
					sc.add(value);
				}
			}
		}
		
		for (List<Double> sc : scores){
			for (int i = 0; i < sc.size(); i++) {
				score.set(i, score.get(i) + sc.get(i));
			}
		}
		
		for (int i = 0; i < score.size(); i++) {
			score.set(i, score.get(i) / scores.size());
		}
		
		return score;
	}
	
	public static List<List<Double>> fillScore(List<String> files) {
		List<List<Double>> scores = new ArrayList<List<Double>>();
		for (String file : files) {
			try {
				BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(Constants.TEMPORY_RESULT_PATH + file));
				List<Double> score = new ArrayList<Double>();
				for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
					score.add(Double.parseDouble(line));
				}
				scores.add(score);
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return scores;
	}
}
