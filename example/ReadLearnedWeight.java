package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class ReadLearnedWeight {

	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/corpus/learnedweight.txt";
		
		List<String> weights = new ArrayList<String>();
		
		try {
			String sCurrentLine;
			BufferedReader br = new BufferedReader(new FileReader(path));
			while((sCurrentLine = br.readLine()) != null) {
				weights.add(sCurrentLine);
			}
			
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("done");
	}
	
}
