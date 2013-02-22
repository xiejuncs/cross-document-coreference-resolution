package edu.oregonstate.featureExtractor;

import edu.smu.tspell.wordnet.*;

public class JWNSWordnet {

	
	public static void main(String[] args) {
		System.setProperty("wordnet.database.dir", "/nfs/guille/xfern/users/xie/Experiment/corpus/WordNet-3.0/dict/");
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		
		NounSynset nounSynset; 
		NounSynset[] hyponyms;
		
		
		Synset[] synsets = database.getSynsets("development", SynsetType.NOUN);
		for (int i = 0; i < synsets.length; i++) { 
		    nounSynset = (NounSynset)(synsets[i]);
		    hyponyms = nounSynset.getHyponyms();
		    WordSense[] derivational = synsets[i].getDerivationallyRelatedForms(nounSynset.getWordForms()[0]);
		    System.err.println(nounSynset.getWordForms()[0] + 
		            ": " + nounSynset.getDefinition() + ") has " + hyponyms.length + " hyponyms"); 
		}
	}
	
}
