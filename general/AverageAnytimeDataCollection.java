package edu.oregonstate.general;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * For EECB corpus, we need to do cross coreference resolution task. There are two steps:
 * 1. do within first
 * 2. do cross coreference resolution.
 * 
 * We guide the search using the four loss functions. In order to evaluate the overall performance 
 * of all within coreference resolution, we average the result.
 * 
 * There are cases, all scores all zero, we remove this kind of instance. Meanwhile, we also remove the other loss scores.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class AverageAnytimeDataCollection {
	
	private static String topicPath = "../corpus/SEARCHRESULT/20121003/logfile/";

	public static void main(String[] args) {
		String parameter = "30-1";
		List<String> files  = new ArrayList<String>();
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
		
		Collections.sort(mucFiles);
		Collections.sort(bcuedFiles);
		Collections.sort(pairwiseFiles);
		Collections.sort(ceafFiles);
		
		
		List<List<Double>> mucScores = fillScore(mucFiles);
		List<List<Double>> bcubedScores = fillScore(bcuedFiles);
		List<List<Double>> pairwiseScores = fillScore(pairwiseFiles);
		List<List<Double>> ceafScores = fillScore(ceafFiles);
		
		List<Double> mucscorewithzero = addScore(mucScores);
		List<Double> bcubedscorewithzero = addScore(bcubedScores);
		List<Double> pairwisescorewithzero = addScore(pairwiseScores);
		List<Double> ceafscorewithzero = addScore(ceafScores);
		
		int size = mucFiles.size();
		List<List<Double>> mucScores1 = new ArrayList<List<Double>>();
		List<List<Double>> bcubedScores1 = new ArrayList<List<Double>>();
		List<List<Double>> pairwiseScores1 = new ArrayList<List<Double>>();
		List<List<Double>> ceafScores1 = new ArrayList<List<Double>>();
		for (int i = 0; i < size; i++) {
			List<Double> mucscore = mucScores.get(i);
			List<Double> bcubedscore = bcubedScores.get(i);
			List<Double> pairwisescore = pairwiseScores.get(i);
			List<Double> ceafscore = ceafScores.get(i);
			
			if (isAllZero(mucscore) || isAllZero(bcubedscore) || isAllZero(pairwisescore) || isAllZero(ceafscore)) continue;
			
			mucScores1.add(mucscore);
			bcubedScores1.add(bcubedscore);
			pairwiseScores1.add(pairwisescore);
			ceafScores1.add(ceafscore);
		}
		
		List<Double> mucscorewithoutzero = addScore(mucScores1);
		List<Double> bcubedscorewithoutzero = addScore(bcubedScores1);
		List<Double> pairwisescorewithoutzero = addScore(pairwiseScores1);
		List<Double> ceafscorewithoutzero = addScore(ceafScores1);
		
		System.out.println("Done");
	}
	
	/** determine whether a list contains all zero */
	public static boolean isAllZero(List<Double> score) {
		boolean isallzero = true;
		for (Double sc : score) {
			if (sc > 0.0) {
				isallzero = false;
				break;
			}
		}
		
		return isallzero;
	}
	
	/** add score together and divide the number of files */
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
	
	/** read values from the file */
	public static List<List<Double>> fillScore(List<String> files) {
		List<List<Double>> scores = new ArrayList<List<Double>>();
		for (String file : files) {
			try {
				BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(topicPath + file));
				List<Double> score = new ArrayList<Double>();
				for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
					score.add(Double.parseDouble(line));
				}
				scores.add(score);
				entitiesBufferedReader.close();
			}
			catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		return scores;
	}
}
