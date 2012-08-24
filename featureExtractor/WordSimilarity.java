package edu.oregonstate.featureExtractor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

import edu.oregonstate.util.GlobalConstantVariables;

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
	private String filePath;
	private List<SimWord> datas;
 	
	/** represent each similarity word */
	public static class SimWord {
		String token;
		Double value;
		
		public SimWord(String token, Double value) {
			this.token = token;
			this.value = value;
		}
		
		@Override
		public String toString() {
			return token;
		}
	}
	
	public WordSimilarity(String path) {
		this.filePath = path;
		datas = new ArrayList<SimWord>();
	}
	
	// get top-ten words
	public List<SimWord> getData(String query) {
		initialize(query);
		sort(datas);
		int n = 10;
		List<SimWord> toptenList = new ArrayList<SimWord>();
		if (datas.size() < 10) return toptenList;
		for (int i = 0; i < 10; i++) {
			toptenList.add(datas.get(i));
		}
		return toptenList;
	}
	
	private static boolean more(SimWord v, SimWord w) {
		return (v.value).compareTo((w.value)) > 0;
	}
	
	private static void exch(List<SimWord> names, int i, int j ){
        SimWord t = names.get(i);
        names.set(i, names.get(j));
        names.set(j, t);
	}
	
	private static void sort(List<SimWord> names) {
        int N = names.size();
        for (int i = 0; i < N; i++ ) {
                int min = i;
                for (int j = i+1; j < N; j++) {
                        if (more(names.get(j), names.get(min))) min = j;
                }
                exch(names, i, min);
        }
	}
	
	public static void main(String[] args) {
		WordSimilarity wordSimilarity = new WordSimilarity(GlobalConstantVariables.WORD_SIMILARITY_PATH);
		List<SimWord> words = wordSimilarity.getData("home");
		words.add(new SimWord("home", 0.0));
		System.out.println(words);
	}
	
	/** initialize the word similarity dictionary */
	private void initialize(String query) {
		try {
			FileInputStream fstream = new FileInputStream(filePath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			String strLine;
			Boolean pass = true;
			while ((strLine = br.readLine()) != null) {
				if (strLine.startsWith("(" + query)) {
					pass = false;
				}
				if (pass) continue;
				if (!strLine.startsWith("(") && !strLine.startsWith(")")) {
					String[] words = strLine.split("\t");
					SimWord simword = new SimWord(words[0], Double.parseDouble(words[1]));
					datas.add(simword);
				}
				if (strLine.startsWith(")") && !pass) break;
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
