package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * For each topic, there are several documents included in the topic directory
 * In order to take advantage of Stanford multi-sieves system, we concatenate them
 * together. And as a whole document to do coreference resolution.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class DocumentConcatenation {
	
	public static void main(String[] args) {
		String topicPath = "corpus/EECB2.0/data/1/";
		String[] files = new File(topicPath).list();
		ArrayList<String> fileNumbers = new ArrayList(Arrays.asList(files));
		//for (String filename : files) {
			//fileNumbers.add(filename);
		//}
		Collections.sort(fileNumbers);
		
		for (String in : fileNumbers) System.out.println(in);
		
		List<Integer> lines = new ArrayList<Integer>(); 
		int i = 0;
		ArrayList<String> rawText = new ArrayList<String>();
		for (String filename : fileNumbers) {
			filename = topicPath + filename;
			try {
				BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
				for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
					i++;
					line = line.replaceAll("\\<[^\\>]*\\>", "");
					rawText.add(line);
				}
				lines.add(i);
				entitiesBufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		for (String sentence : rawText) System.out.println(sentence);
		
		for (int j = 0; j < lines.size(); j++) {
			System.out.println(lines.get(j));
			if (j==0) {
				List<String> document1 = rawText.subList(0, lines.get(j));
				System.out.println(document1);
			} else {
				List<String> document1 = rawText.subList(lines.get(j-1), lines.get(j));
				System.out.println(document1);
			}
		}
		
		
		System.out.println("done");
	}
}
