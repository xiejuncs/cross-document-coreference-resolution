package edu.oregonstate.featureExtractor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * extract the top-ten most-similar words in Dekang Lin's similarity thesaurus for all the nouns/adjectives/verbs in a 
 * cluster
 * <p>
 * Proximity-based Thesaurus: (http://webdocs.cs.ualberta.ca/~lindek/downloads.htm)
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class WordSimilarity {
	
	/* file path */
	private String filePath;
	
	/* datas used for mention word feature */
	private Map<String, List<String>> datas;
	
	public WordSimilarity(String path) {
		this.filePath = path;
		datas = new HashMap<String, List<String>>();
	}
	
	public Map<String, List<String>> getDatas() {
		return datas;
	}
	
	/** initialize the word similarity dictionary */
	public void initialize() {
		try {
			FileInputStream fstream = new FileInputStream(filePath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			String strLine;
			Boolean pass = true;
			String currentIndex = "";
			List<String> mentionWords = new ArrayList<String>();
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("(")) {
					pass = false;
					String[] words = strLine.split(" ");
					currentIndex = words[0].substring(1);
					mentionWords = new ArrayList<String>();
					mentionWords.add(currentIndex);
					i = 1;
				}
				if (pass) continue;
				if (!strLine.startsWith("(") && !strLine.startsWith(")") && i < 11) {
					String[] words = strLine.split("\t");
					mentionWords.add(words[0]);
					i += 1;
				}
				
				if (i == 10) {
					datas.put(currentIndex, mentionWords);
					pass = true;
				}
			}
			
			br.close();
			in.close();
			fstream.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			System.exit(1);
		}		
	}

}
