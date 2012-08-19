package edu.oregonstate.util;

import java.util.List;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.Set;
import java.util.HashSet;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.dictionary.Dictionary;

/**
 * find the links between synonyms, we need to calculate the percentage of newly-introduced mentions links after the merge 
 * that are wordnet synonyms
 */  
public class Wordnet {
        	
	public static boolean findSynonyms(List<String> cluster1, List<String> cluster2) {
		Set<String> synos = new HashSet<String>();
		
		try {
			for (String word : cluster1) {
				IndexWord indexWord = Dictionary.getInstance().getIndexWord(POS.VERB, word);
				if (indexWord == null) continue;
				Synset[] senses = indexWord.getSenses();
				for (int i = 0; i < senses.length; i++) {
					for (String word1 : cluster2) {
						if (senses[i].containsWord(word1)) {
							synos.add(word + "----->" + word1);
						}
					}
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		return (synos.size() > 0) ? true : false;
	}
	
	
	public static void main(String[] args) {
		configureJWordNet();
		List<String> cluster1 = new ArrayList<String>();
		List<String> cluster2 = new ArrayList<String>();
		cluster1.add("hit");
		cluster1.add("strike");
		cluster2.add("strike");
		cluster2.add("join");
		cluster2.add("say");
		Set<String> synos = new HashSet<String>();
	
		try {
			for (String word : cluster1) {
				IndexWord indexWord = Dictionary.getInstance().getIndexWord(POS.VERB, word);
				Synset[] senses = indexWord.getSenses();
				for (int i = 0; i < senses.length; i++) {
					for (String word1 : cluster2) {
						if (senses[i].containsWord(word1)) {
							synos.add(word + "----->" + word1);
						}
					}
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println(synos);
	}

	public static void configureJWordNet() {
		try {
			JWNL.initialize(new FileInputStream(GlobalConstantVariables.WORD_NET_CONFIGURATION_PATH));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
} 
