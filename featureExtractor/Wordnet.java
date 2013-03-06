package edu.oregonstate.featureExtractor;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.VerbSynset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.smu.tspell.wordnet.WordSense;


/**
 * find the links between synonyms, we need to calculate the percentage of newly-introduced mentions links after the merge 
 * that are wordnet synonyms
 */  
public class Wordnet {
	
	// get an wordnet database instance
	private final WordNetDatabase wordnet;

	public Wordnet() {
		wordnet = WordNetDatabase.getFileInstance();
	}

	/**
	 * get synonym according to the lemma and its synset type
	 * 
	 * @param lemma
	 * @param type
	 * @return
	 */
	public Set<String> getSynonym(String lemma, SynsetType type) {
		Set<String> synonyms = new HashSet<String>();
		Synset[] synsets = wordnet.getSynsets(lemma, type);
		for (Synset synset : synsets) {
			String[] wordforms = synset.getWordForms();
			synonyms.addAll(Arrays.asList(wordforms));
		}

		return synonyms;
	}

	/**
	 * get derivationally form
	 * 
	 * @param lemma
	 * @param type
	 * @return
	 */
	public Set<String> getDerivationallyRelatedForms(String lemma, SynsetType type) {
		Set<String> derivationallyForm = new HashSet<String>();
		Synset[] synsets = wordnet.getSynsets(lemma, type);
		for (Synset synset : synsets) {
			WordSense[] senses = synset.getDerivationallyRelatedForms(lemma);
			for (WordSense sense : senses) {
				derivationallyForm.add(sense.getWordForm());
			}
		}

		return derivationallyForm;
	}

	/**
	 * get noun hypernym
	 * 
	 * @param lemma
	 * @param type
	 * @return
	 */
	public Set<String> getNounHypernym(String lemma) {
		Set<String> hypernyms = new HashSet<String>();
		Synset[] synsets = wordnet.getSynsets(lemma, SynsetType.NOUN);
		for (Synset synset : synsets) {
			NounSynset nounSynset = (NounSynset) synset;
			NounSynset[] hypernymSynset = nounSynset.getHypernyms();
			for (NounSynset set : hypernymSynset) {
				hypernyms.addAll(Arrays.asList(set.getWordForms()));
			}
		}

		return hypernyms;
	}

	/**
	 * get verb hypernym
	 * 
	 * @param lemma
	 * @param type
	 * @return
	 */
	public Set<String> getVerbHypernym(String lemma) {
		Set<String> hypernyms = new HashSet<String>();
		Synset[] synsets = wordnet.getSynsets(lemma, SynsetType.VERB);
		for (Synset synset : synsets) {
			VerbSynset verbSynset = (VerbSynset) synset;
			VerbSynset[] hypernymSynset = verbSynset.getHypernyms();
			for (VerbSynset set : hypernymSynset) {
				hypernyms.addAll(Arrays.asList(set.getWordForms()));
			}
		}

		return hypernyms;
	}	

	/**
	 * set word net path first
	 * 
	 * @param wordnetPath
	 */
	public static void setWordNet(String wordnetPath) {
		System.setProperty("wordnet.database.dir", wordnetPath);
	}

	/**
	 * WORDNET examples
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String wordnetPath = "/home/jun/JavaFile/corpus/WordNet-3.0/dict";
		Wordnet.setWordNet(wordnetPath);

		Wordnet wordnet = new Wordnet();
		Set<String> synonyms = wordnet.getSynonym("region", SynsetType.NOUN);
		Set<String> nounHypernyms = wordnet.getNounHypernym("tent");
		Set<String> verbHypernyms = wordnet.getVerbHypernym("shout");
		Set<String> derivationallyForm = wordnet.getDerivationallyRelatedForms("develop", SynsetType.VERB);

		System.out.println("done");
	}

} 
