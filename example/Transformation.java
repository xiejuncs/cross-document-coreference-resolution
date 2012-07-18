package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import edu.oregonstate.util.GlobalConstantVariables;

/**
 * Transform ECB to EECB
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Transformation {
	
	public static String ecbPath = "corpus/ECB1.0/data/";
	public static String filename = GlobalConstantVariables.MENTION_ANNOTATION_PATH;
	
	// do annotation for one doc
	public static void annotateDoc(String topic, String doc, HashMap<String, ArrayList<String>> mentions) throws IOException {
		String ecbFile = ecbPath + topic + "/" + doc + ".ecb";
		String eecbFile = "corpus/EECB1.0/data/" + topic + "/" + doc + ".eecb";
		boolean status;
		status = new File("corpus/EECB1.0/data/" + topic + "/").mkdirs();
		assert status == true;
		String sentIdx = "0";
		
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(ecbFile));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				line = line.replaceAll("\\<[^\\>]*\\>", "");
				if (mentions.containsKey(sentIdx))
					line = addAnnotation(line, mentions.get(sentIdx));
				writeToFile(eecbFile, line);
			}
			entitiesBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Write to file line by line
	 * 
	 * @param filename
	 * @param line
	 * @throws IOException
	 */
	public static void writeToFile(String filename, String line) throws IOException {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename), true));
			bw.write(line);
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	
	// add annotation to the line
	public static String addAnnotation(String line, ArrayList<String> mentions) {
		String annotated = "";
		ArrayList<Integer> annoStartCharIdx = new ArrayList<Integer>();
		ArrayList<Integer> annoEndCharIdx = new ArrayList<Integer>();
		
		for (String mention : mentions) {
			int startCharIdx = Integer.parseInt(mention.get(7)); // NOTE HERE, the data structure is wrong, need to revise later
			int endCharIdx = Integer.parseInt(mention.get(8));
			if(contains(startCharIdx, annoStartCharIdx)) annoStartCharIdx.set(startCharIdx, 0);
			if(contains(endCharIdx, annoEndCharIdx)) annoEndCharIdx.set(endCharIdx, 0);
			annoStartCharIdx.set(startCharIdx. mention);
			annoEndCharIdx.set(endCharIdx, mention);
		}
		
		int charIdx = 0;
		for (int i = 0; i < line.length(); i++) {
			char ch = line.charAt(i);
			if (contains(charIdx, annoEndCharIdx)) {
				int annoEnds = annoEndCharIdx.get(charIdx);
				
			}
		}
		return annotated;
	}
	
	/**
	 * Given a search key and an arraylist in any type
	 * If the array contains the key, return true else false.
	 * 
	 * @param <E>
	 * @param search
	 * @param array
	 * @return
	 */
	public static <E> boolean contains(E search, ArrayList<E> array) {
		boolean contain = false;
		for (E element : array) {
			if (search.equals(element)) {
				contain = true;
				break;
			}
		}
		return contain;
	}
	
	/**
	 * convert array to ArrayList
	 * In this case, we create an generic class, and then instantiate it
	 * 
	 * @param <E>
	 * @param intputArray
	 * @return
	 */
	public static <E> ArrayList<E> convertArraytoList(E[] intputArray) {
		ArrayList<E> output = new ArrayList<E>();
		for (E element : intputArray) {
			output.add(element);
		}
		return output;
	}
	
	public static void main(String[] args) throws IOException {
		try {
			String topic = "0";
			String doc = "0";
			HashMap<String, ArrayList<String>> mentions = new HashMap<String, ArrayList<String>>(); 
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				if (line.startsWith("#")) continue;
				String[] tokens = line.split("\t");
				if(!topic.equals("0") && !(topic + "-" + doc).equals(tokens[1] + "-" + tokens[2])) {
					annotateDoc(topic, doc, mentions);
					mentions = new HashMap<String, ArrayList<String>>();
				}
				
				topic = tokens[1];
				doc = tokens[2];
				if (!mentions.containsKey(tokens[3])) {
					mentions.put(tokens[3], new ArrayList<String>());
				}
				ArrayList<String> token = convertArraytoList(tokens);
				mentions.put(tokens[3], token);
			}
			annotateDoc(topic, doc, mentions);
			entitiesBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("Done..........................");
	}
}
