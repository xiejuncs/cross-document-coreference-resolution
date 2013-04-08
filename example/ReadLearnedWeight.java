package edu.oregonstate.example;

import java.util.*;

import edu.stanford.nlp.io.IOUtils;

public class ReadLearnedWeight {

	public static void main(String[] args) {
		String filePath = "/nfs/guille/xfern/users/xie/Experiment/corpus/EECB1.0/tokenoutput/file";
		List<String> lines = IOUtils.linesFromFile(filePath);
		Map<String, Integer> maps = new TreeMap<String, Integer>();
		for (String line : lines) {
			String[] elements = line.split("\t");
			String word = elements[1];
			if (!maps.containsKey(word)) {
				maps.put(word, 0);
			}
			int counter = maps.get(word) + 1;
			maps.put(word, counter );
		}
		
		for (String word : maps.keySet()) {
			System.out.println(word + " " + maps.get(word));
		}
		
		
	}
	
}
