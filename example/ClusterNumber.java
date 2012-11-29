package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class ClusterNumber {

	public static void main(String[] args) {
		List<String> data = new ArrayList<String>();
		String path = "/scratch/JavaFile/corpus/mentions.txt";
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				if (currentLine.startsWith("##")) continue;
				data.add(currentLine);
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		Map<Integer, Set<String>> datas = new TreeMap<Integer, Set<String>>();
		Map<Integer, List<String>> dataStrings = new TreeMap<Integer, List<String>>();
		Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
		for (String string : data) {
			String[] elements = string.split("\t");
			int topic = Integer.parseInt(elements[1]);
			if (!datas.containsKey(topic)) {
				datas.put(topic, new HashSet<String>());
			}
			datas.get(topic).add(elements[0] + elements[4]);
			
			int count = 0;
			if (counts.containsKey(topic)) {
				count = counts.get(topic);
			}
			counts.put(topic, count + 1);
			
			if (!dataStrings.containsKey(topic)) {
				dataStrings.put(topic, new ArrayList<String>());
			}
			
			dataStrings.get(topic).add(string);
			
		}
		
		for (Integer id : datas.keySet()) {
			System.out.println(id + " " + counts.get(id) + " " + datas.get(id).size());
			List<String> mentions = dataStrings.get(id);
			Map<Integer, List<String>> mentionCount = new TreeMap<Integer, List<String>>();
			for (String mention : mentions) {
				String[] elements = mention.split("\t");
				int document = Integer.parseInt(elements[2]);
				if (!mentionCount.containsKey(document)) {
					mentionCount.put(document, new ArrayList<String>());
				}
				int mentionID = 10000000 * id + 100000 * document;
				mentionCount.get(document).add(mentionID + "");
			}
			
			for (Integer document : mentionCount.keySet()) {
				System.out.println(document + " " + mentionCount.get(document).size());
			}
			System.out.println("\n");
		}
			
			
		/*
		Set<String> nclusters = new HashSet<String>();
		Set<String> vclusters = new HashSet<String>();
		for (String string : data) {
			String[] elements = string.split("\t");
			if (elements[0].equals("N")) {
				nclusters.add(elements[0] + elements[4]);
			} else {
				vclusters.add(elements[0] + elements[4]);
			}
		}
		
		System.out.println(nclusters.size() + vclusters.size());
		System.out.println(nclusters);
		System.out.println(vclusters);
		*/
		
	}
	
	
}
