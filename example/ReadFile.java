package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class ReadFile {

	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/corpus/weight.txt";
		List<double[]> weights = new ArrayList<double[]>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String currentLine;
			while((currentLine = br.readLine()) != null) {
				String[] segments = currentLine.split(",");
				double[] weight = new double[segments.length];
				for (int i = 0; i < segments.length; i++) {
					weight[i] = Double.parseDouble(segments[i]);
				}
				weights.add(weight);
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("done");
	}
}
