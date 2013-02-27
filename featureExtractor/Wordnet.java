package edu.oregonstate.featureExtractor;

import java.util.List;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.util.Set;
import java.util.HashSet;

import edu.oregonstate.experiment.ExperimentConstructor;

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
		cluster1.add("earthquake");
		cluster2.add("temblor");
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
		System.out.println(synos);
	}

	public static void configureJWordNet() {
		String WORD_NET_CONFIGURATION_PATH = "/scratch/JavaFile/corpus/file_properties.xml";
		try {
			JWNL.initialize(new FileInputStream(WORD_NET_CONFIGURATION_PATH));
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}
	}
	
} 
