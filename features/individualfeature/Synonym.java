package edu.oregonstate.features.individualfeature;

import edu.oregonstate.features.NumericFeature;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.util.IntPair;

/**
 * The percentage of newly-introduced metnion links after the merge taht are WordNet synonyms
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Synonym extends NumericFeature {

	// whether do pronoun resolution
	private final boolean DOPRONOUN;
	
	public Synonym() {
		featureName = this.getClass().getSimpleName();
		DOPRONOUN = false;
	}
	
	@Override
	public double generateFeatureValue(Document document, CorefCluster former, CorefCluster latter, String mentionType) {
		double synonymNom = 0.0;
		double synonymDenom = 0.0;
		
		for(Mention m1 : former.getCorefMentions()) {
			for(Mention m2 : latter.getCorefMentions()) {
				if(!DOPRONOUN && (m1.isPronominal() || m2.isPronominal())) continue;
				IntPair menPair = new IntPair(Math.min(m1.mentionID, m2.mentionID), Math.max(m1.mentionID, m2.mentionID));

				synonymDenom++;
				if(document.mentionSynonymInWN.contains(menPair)) {
					synonymNom++;
				}
			}
		}
		
		return synonymNom/synonymDenom;
	}

}
