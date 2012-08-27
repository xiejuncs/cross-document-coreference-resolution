package edu.oregonstate.featureExtractor;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import edu.oregonstate.data.SrlAnnotation;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;

/**
 * Incorporate the SRL result from Semantic Parsing software into EECB co-reference resolution
 * SRL result is very important for Joint Entity and Event Co-reference resolution.
 * 
 * Seven out of ten features are based on the SRL result
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SrlResultIncorporation {
	private String resultPath;
	private SemanticOutputInterface semantic;
	private Map<Integer, List<String>> sentenceidToSentence;
	private Map<Integer, Map<SrlAnnotation, Map<String, List<SrlAnnotation>>>> extentsWithArgumentRoles;
	private Map<Integer, Integer> matchResult;
	
	public SrlResultIncorporation(String path) {
		this.resultPath = path;
		semantic = new SemanticOutputInterface();
		sentenceidToSentence = new HashMap<Integer, List<String>>();
		extentsWithArgumentRoles = new HashMap<Integer, Map<SrlAnnotation,Map<String,List<SrlAnnotation>>>>();
		matchResult = new HashMap<Integer, Integer>();
		initialize();
	}
	
	/**
	 * read into the data
	 * 
	 */
	private void initialize() {
		semantic.setDocument(semantic.read(resultPath));
        Map<Integer, List<List<String>>> doc = semantic.getDocument();
        for (Integer id : doc.keySet()) {
                List<List<String>> sentence = doc.get(id);
                Map<SrlAnnotation, Map<String, List<SrlAnnotation>>> extentWithArgumentRoles = semantic.extractExtent(sentence);
                if (extentWithArgumentRoles.size() == 0) continue;
                List<String> tokens = new ArrayList<String>();
                for (List<String> data : sentence) {
                        tokens.add(data.get(1));
                }
                extentsWithArgumentRoles.put(id , extentWithArgumentRoles);
                sentenceidToSentence.put(id, tokens);
        }
	}
	
	/**
	 * match the sentence for allPredictedMentions
	 * 
	 * @param allPredictedMentions
	 */
	private void matchSenten(List<List<Mention>> allPredictedMentions) {
		assert allPredictedMentions.size() == sentenceidToSentence.size();
		
		for (int i = 0; i < allPredictedMentions.size(); i++) {
			
			if (allPredictedMentions.get(i).size() == 0) continue;
			
			List<CoreLabel> sentenceWords = allPredictedMentions.get(i).get(0).sentenceWords;
			StringBuilder sborigi = new StringBuilder();
			for (CoreLabel label : sentenceWords) {
				String word = label.getString(TextAnnotation.class);
				sborigi.append(word);
			}
			String originalsent = sborigi.toString().trim().replace("\\", "");
			for (Integer key : sentenceidToSentence.keySet()) {
				List<String> tokens = sentenceidToSentence.get(key);
				// concatenate the tokens together
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < tokens.size(); j++) {
					sb.append(tokens.get(j));
				}
				String sent = sb.toString().trim().replace("\\", "");
				if (originalsent.contains(sent)) {
					matchResult.put(i, key);
					break;
				}
			}
		}
	}
	
	public static int[] convertByteOffset(String sentence, int startIndex, int endIndex) {
		int astartIndex = 0;
		int aendIndex = 0;
		
		int offset = 0;
		char[] chars = sentence.toCharArray();
		int textLength = chars.length;
		String[] characters = new String[chars.length];
		for (int i = 0; i < textLength; i++) {
			char character = chars[i];
			characters[i] = Character.toString(character);
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < textLength; i++) {
			String character = characters[i];
			if ((offset >= startIndex) && (offset < endIndex) ) {
				if (offset == startIndex) astartIndex = i;
				if (offset == (endIndex-1)) aendIndex = i+1;
				if (offset == startIndex && (character.equals(" "))) continue;
				sb.append(character);
			}
			if (!character.equals(" ")) offset = offset + 1;
		}
		
		int[] byteoffset = new int[2];
		byteoffset[0] = astartIndex;
		byteoffset[1] = aendIndex;
		return byteoffset;
	}
	
	/**
	 * align the predict mention with the srl result
	 * at first: match the sentence
	 * and then use byteoffset to match the the arguments of the mentions and set the isEvent as true
	 * 
	 * @param allPredictedMentions
	 */
	public void alignSRL(List<List<Mention>> allPredictedMentions) {
		matchSenten(allPredictedMentions);
		
		// iterate e
		for (int i = 0; i < allPredictedMentions.size(); i++) {
			int sentenceID = i;
			boolean contains = matchResult.containsKey(sentenceID);
			if (!contains) continue;
			int correspondingID = matchResult.get(sentenceID);
			Map<SrlAnnotation, Map<String, List<SrlAnnotation>>> srlResult = extentsWithArgumentRoles.get(correspondingID);
			for (int j = 0; j < allPredictedMentions.get(i).size(); j++) {
				Mention mention = allPredictedMentions.get(i).get(j);
				int start = mention.getByteStartOffset();
				int end = mention.getByteEndOffset();
				for (SrlAnnotation predicate : srlResult.keySet()) {
					int predicateStart = predicate.getStartOffset();
					int predicateEnd = predicate.getEndOffset();
					if ((predicateStart == start) && (predicateEnd == end)) {
						Map<String, List<SrlAnnotation>> arguments = srlResult.get(predicate);
						if (arguments.size() == 0) continue;
						
						for (String argKey : arguments.keySet()) {
							List<SrlAnnotation> argument = arguments.get(argKey);
							
							// match the id
							int argumentStart = argument.get(0).getStartOffset();
							int argumentEnd = argument.get(argument.size() - 1).getEndOffset();
							for (int k = 0; k < allPredictedMentions.get(i).size(); k++) {
								Mention mentionMatch = allPredictedMentions.get(i).get(k);
								int mentionMatchStart = mentionMatch.getByteStartOffset();
								int mentionMatchEnd = mentionMatch.getByteEndOffset();
								
								// it is very tricky here, I need to experiment for a while
								// if ((argumentStart <= mentionMatchStart) && (mentionMatchEnd <= argumentEnd))
								if ((argumentStart == mentionMatchStart) && (mentionMatchEnd == argumentEnd)) {
									mention.setArgument(argKey, mentionMatch);
									mentionMatch.setPredicte(mention);
									mentionMatch.SRLrole = argKey;
									break;
								}
							}
						}
						// do not need to go through all the documents, just once
						break;
					}
				}
			}
		}
		
		
	}
	
}
