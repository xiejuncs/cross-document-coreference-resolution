package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class RawTextMatch {

	public static void main(String[] args) {
		RawTextMatch rawTextMatch = new RawTextMatch();
		List<String> files = new ArrayList<String>(Arrays.asList("1.eecb", "2.eecb"));
		String topic = "1";
		rawTextMatch.readRawText(files, topic);
	}
	
	public static Integer[] getSentences(ArrayList<String> annotation) {
		HashSet<String> sentences = new HashSet<String>();
		for (String annos : annotation) {
			String[] anno = annos.split(":");
			String key = anno[2];
			sentences.add(key);
		}
		Integer[] sentenceLines = new Integer[sentences.size()];
		int i = 0;
		for (String sentence : sentences) {
			sentenceLines[i] = Integer.parseInt(sentence);
			i++;
		}
		Arrays.sort(sentenceLines);
		return sentenceLines;
	}
	
	
	private void readRawText(List<String> files, String topic) {
		List<Integer> lines = new ArrayList<Integer>(); 
		int j = 0;
		List<String> rawText = new ArrayList<String>();
		HashMap<String, ArrayList<String>> annotations = readAnnotation();
		Map<Integer, String> topicToDocument = new HashMap<Integer, String>();
		Map<String, Integer> documentToTopic = new HashMap<String, Integer>();
		ArrayList<String> annotation = annotations.get(topic);
		for (String filename : files) {
			String documentID = filename.substring(0, filename.length() - 5);
			System.out.println("Document ID : " + documentID);
			String key = topic + ":" + documentID;
			ArrayList<String> anno = new ArrayList<String>();
			for (String record : annotation) {
				String[] records = record.split(":");
				if (records[1].equals(documentID)) {
					anno.add(record);
				}
			}
			
			System.out.println(anno);
			Integer[] sentences = getSentences(anno);
			for (Integer sentence : sentences) {
				System.out.println(sentence);
			}
			int i = 0;
			try {
				BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
				for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
					line = line.replaceAll("\\<[^\\>]*\\>", "");
					boolean contain = false;
					for (Integer sentence : sentences) {
						if (sentence == i) {
							contain = true;
							break;
						}
					}
					if (contain) { 
						rawText.add(line);
						topicToDocument.put(j, key + ":" + Integer.toString(i));
						documentToTopic.put(key + ":" + Integer.toString(i), j);
						j++;
					}
					i++;
				}
				lines.add(i);
				entitiesBufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		StringBuffer sb = new StringBuffer();
		for (String line : rawText) {
			sb.append(line + "\n");
		}
		System.out.println(sb.toString());
		
		System.out.println(topicToDocument);
		System.out.println(documentToTopic);
	}
	
	/**
	 * <b>NOTE</b>
	 * 
	 * EECB document is annotated by the specification of mentions.txt.
	 * Hence, we need to annotate the plain document according to the mentions.txt
	 * Those annotated entities and events are gold annotations. We need to add those into 
	 * our document class in order to evaluate the accuracy of the proposed algorithms
	 * 
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> readAnnotation() {
		HashMap<String, ArrayList<String>> annotation = new HashMap<String, ArrayList<String>>();
		String mentionPath = "";    // mentions.txt path
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(mentionPath));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				if (line.startsWith("#")) continue;
				String[] record = line.split("\t"); // the separator is \t
				// get every element out of the current record
				String type = record[0];
				String topicID = record[1];
				String documentID = record[2];
				String sentenceNumber = record[3];
				String corefID = record[4];
				String startIndex = record[5];
				String endIndex = record[6];
				String startCharIndex = record[7];
				String endCharIndex = record[8];
				// check whether annotation HashMap contains the key (topic:documentID), 
				// if contains, add the current string combination (type:sentenceNumber:corefID:startIndex:endIndex:startCharIndex:endCharIndex) to the existed ArrayList
				// if not contains, initialize an empty ArrayList, and add the string combination to the empty ArrayList
				String key = topicID;
				String value = type + ":" + documentID + ":"  + sentenceNumber + ":" + corefID + ":" + startIndex + ":" + endIndex + ":" + startCharIndex + ":" + endCharIndex;
				boolean contains = annotation.containsKey(key);
				ArrayList<String> values = new ArrayList<String>();
				if (contains) {
					values = annotation.get(key);
				}
				values.add(value);
				annotation.put(key, values);
			}
			entitiesBufferedReader.close();
		} catch (Exception e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return annotation;
	}
	
}
