package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.general.StringOperation;

public class Weight {

	public static void main(String[] args) {
		String weightPath = "/scratch/JavaFile/result/2013/201303/20130311/Mon-Mar-04-18-13-54-PST-2013-true-null-null/weight.txt";
		List<String> featureTemplate = FeatureFactory.getFeatureTemplate();
		
		String weightString = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(weightPath));
			while ((weightString = br.readLine()) != null) {
				break;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String[] weights = StringOperation.splitString(weightString, ",");
		assert weights.length == featureTemplate.size();
		
		for (int i = 0; i < weights.length; i++) {
			System.out.println(featureTemplate.get(i) + " : " + weights[i]);
		}
	}
	
	
}
