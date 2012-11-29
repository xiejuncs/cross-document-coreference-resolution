package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import Jama.Matrix;

import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.search.State;
import edu.stanford.nlp.dcoref.CorefCluster;

public class ReadFile {

	public static void main(String[] args) {
		String path = "../corpus/TEMPORYRESUT/Fri-Nov-09-13:53:21-PST-2012/2/";
		List<String> files  = new ArrayList<String>();
		files = new ArrayList<String>(Arrays.asList(new File(path).list()));
		int minimumValue = Integer.MAX_VALUE;
		int maximumValue = Integer.MIN_VALUE;
		for (String file : files) {
			if (!file.startsWith("t")) {
				int no = Integer.parseInt(file);
				if (no < minimumValue) {
					minimumValue = no;
				}
				
				if (no > maximumValue) {
					maximumValue = no;
				}
				
			}
		}
		
		String filePath = path + maximumValue;
		Matrix weight = new Matrix(39, 1, 0.5);
		Matrix totalweight = new Matrix(39, 1, 0);

		try {
			BufferedReader br = new BufferedReader(new FileReader(filePath));
			String currentLine;
			while((currentLine = br.readLine()) != null) {
				String[] segments = currentLine.split("-");
				String goodFeatures = segments[0];
				String badFeatures = segments[1];
				Matrix goodMatrix = convertMatrix(goodFeatures);
				Matrix badMatrix = convertMatrix(badFeatures);
				
				double goodCost = goodMatrix.times(weight).get(0, 0);
				double badCost = badMatrix.times(weight).get(0, 0);
				
				if (goodCost <= badCost) {
					Matrix difference = goodMatrix.minus(badMatrix);
					Matrix transposeDifference = difference.transpose();
					weight.plus(transposeDifference);
					totalweight.plus(weight);
				}
				
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("done");
	}
	
	private static Matrix convertMatrix(String goodFeatures) {
		String[] features = goodFeatures.split(",");
		Matrix matrix = new Matrix(1, features.length);
		for (int i = 0; i < features.length; i++) {
			matrix.set(0, i, Double.parseDouble(features[i]));
		}
		return matrix;
	}
}
