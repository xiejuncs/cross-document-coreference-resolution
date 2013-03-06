package edu.oregonstate.featureExtractor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import edu.stanford.nlp.stats.ClassicCounter;

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

	// file path
	private final String filePath;

	// data used for mention word feature
	private Map<String, ClassicCounter<String>> datas;

	public WordSimilarity(String path) {
		this.filePath = path;
		datas = new HashMap<String, ClassicCounter<String>>();
	}

	// return the data
	public Map<String, ClassicCounter<String>> getDatas() {
		return datas;
	}

	/** load the word similarity dictionary */
	public void load() {
		try {
			FileInputStream fstream = new FileInputStream(filePath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String strLine;
			boolean pass = true;
			String currentIndex = "";
			ClassicCounter<String> mentionWords = new ClassicCounter<String>();
			int i = 0;
			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("(")) {
					pass = false;
					String[] words = strLine.split(" ");
					currentIndex = words[0].substring(1);
					mentionWords = new ClassicCounter<String>();
					mentionWords.incrementCount(currentIndex);
					i = 1;
				}
				if (pass) continue;
				if (!strLine.startsWith("(") && !strLine.startsWith(")") && i < 12) {
					String[] words = strLine.split("\t");
					mentionWords.incrementCount(words[0]);
					i += 1;
				}

				if (i == 11) {
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