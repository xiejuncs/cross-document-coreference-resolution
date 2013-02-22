package edu.oregonstate.featureExtractor;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import edu.oregonstate.data.EecbSrlAnnotation;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.ling.CoreAnnotations.TokenBeginAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokenEndAnnotation;
import edu.stanford.nlp.trees.Tree;

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
	private SRLExtraction semantic;
	private Map<Integer, List<String>> sentenceidToSentence;
	private Map<Integer, Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>>> extentsWithArgumentRoles;
	private Map<Integer, Integer> matchResult;
	private RuleBasedCorefMentionFinder rule;
	
	public SrlResultIncorporation(String path) {
		this.resultPath = path;
		semantic = new SRLExtraction();
		sentenceidToSentence = new HashMap<Integer, List<String>>();
		extentsWithArgumentRoles = new HashMap<Integer, Map<EecbSrlAnnotation,Map<String,EecbSrlAnnotation>>>();
		matchResult = new HashMap<Integer, Integer>();
		initialize();
		rule = new RuleBasedCorefMentionFinder(); 
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
                Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>> extentWithArgumentRoles = semantic.extractExtent(sentence);
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
	
	/**
	 * align the predict mention with the srl result
	 * at first: match the sentence
	 * and then use byteoffset to match the the arguments of the mentions and set the isEvent as true
	 * 
	 * @param allPredictedMentions
	 */
	/*
	public void alignSRL1(List<List<Mention>> allPredictedMentions) {
		matchSenten(allPredictedMentions);
		
		// iterate e
		for (int i = 0; i < allPredictedMentions.size(); i++) {
			int sentenceID = i;
			boolean contains = matchResult.containsKey(sentenceID);
			if (!contains) continue;
			int correspondingID = matchResult.get(sentenceID);
			Map<Mention, Map<String, Mention>> srlResult = extentsWithArgumentRoles.get(correspondingID);
			Tree root = allPredictedMentions.get(i).get(0).contextParseTree;
			List<CoreLabel> sentence = allPredictedMentions.get(i).get(0).sentenceWords;
			for (int j = 0; j < allPredictedMentions.get(i).size(); j++) {
				Mention mention = allPredictedMentions.get(i).get(j);
				int start = mention.startIndex;
				int end = mention.endIndex;
				for (Mention predicate : srlResult.keySet()) {
					int predicateStart = predicate.startIndex;
					int predicateEnd = predicate.endIndex;
					if ((predicateStart == start) && (predicateEnd == end)) {
						Map<String, Mention> arguments = srlResult.get(predicate);
						if (arguments.size() == 0) continue;
						
						for (String argKey : arguments.keySet()) {
							Mention argument = arguments.get(argKey);
							
							// match the id
							int argumentStart = argument.startIndex;
							int argumentEnd = argument.endIndex;
							argument.originalSpan = sentence.subList(argumentStart, argumentEnd);
							
							findHead(argument, root, sentence);
							
							for (int k = 0; k < allPredictedMentions.get(i).size(); k++) {
								Mention mentionMatch = allPredictedMentions.get(i).get(k);

								if (mentionMatch.headString.equals(argument.headString)) {
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
	*/
	
	// add the closest left and right mentions into the srl incorporation
	// we have added the left and right mentions before
	public void alignSRL(List<List<Mention>> allPredictedMentions) {
		matchSenten(allPredictedMentions);

		for (int i = 0; i < allPredictedMentions.size(); i++) {
			int sentenceID = i;
			boolean contains = matchResult.containsKey(sentenceID);
			if (!contains) continue;
			int correspondingID = matchResult.get(sentenceID);
			Map<EecbSrlAnnotation, Map<String, EecbSrlAnnotation>> srlResult = extentsWithArgumentRoles.get(correspondingID);
			for (EecbSrlAnnotation predicate : srlResult.keySet()) {
				int predicateStart = predicate.getStartOffset();
				int predicateEnd = predicate.getEndOffset();
				for (Mention mention : allPredictedMentions.get(i)) {
					int start = mention.headWord.get(TokenBeginAnnotation.class);
					int end = mention.headWord.get(TokenEndAnnotation.class);

					if ((predicateStart == start) || (predicateEnd == end)) {
						Map<String, EecbSrlAnnotation> arguments = srlResult.get(predicate);
						if (arguments.size() == 0) continue;

						for (String argKey : arguments.keySet()) {
							EecbSrlAnnotation argument = arguments.get(argKey);

							// match the id
							int argumentStart = argument.getStartOffset();
							int argumentEnd = argument.getEndOffset();
							
							for (int k = 0; k < allPredictedMentions.get(i).size(); k++) {
								Mention mentionMatch = allPredictedMentions.get(i).get(k);
								int mentionMatchStart = mentionMatch.headWord.get(TokenBeginAnnotation.class);
								int mentionMatchEnd = mentionMatch.headWord.get(TokenEndAnnotation.class);

								// it is very tricky here, I need to experiment for a while
								// if ((argumentStart <= mentionMatchStart) && (mentionMatchEnd <= argumentEnd))
								if (!argKey.equals("AM-LOC")) {
									if ((argumentStart <= mentionMatchStart) && (argumentEnd >= mentionMatchEnd)) {
										mention.addArgument(argKey, mentionMatch);
										mentionMatch.addPredicate(mention, argKey);
										break;
									}
								} else {
									if ((argumentStart <= mentionMatchStart) && (argumentEnd >= mentionMatchEnd)) {
										mention.addArgument(argKey, mentionMatch);
										mentionMatch.addPredicate(mention, argKey);
										break;
									}
								}
							}
							
							
						}
					}					
				}
			}
		}
	}
	
	/*
	 if (!argKey.equals("AM-LOC")) {
									if ((argumentStart <= mentionMatchStart) && (argumentEnd >= mentionMatchEnd)) {
										mention.setArgument(argKey, mentionMatch);
										mentionMatch.setPredicte(mention);
										mentionMatch.SRLrole = argKey;
										break;
									}
								} else {
									if ((argumentStart <= mentionMatchStart) && (argumentEnd >= mentionMatchEnd)) {
										mention.setArgument(argKey, mentionMatch);
										mentionMatch.setPredicte(mention);
										mentionMatch.SRLrole = argKey;
										break;
									}
								}
	 */
	
	/*
	public void alignSRL2(List<List<Mention>> allPredictedMentions) {
		matchSenten(allPredictedMentions);
		
		// iterate e
		for (int i = 0; i < allPredictedMentions.size(); i++) {
			int sentenceID = i;
			boolean contains = matchResult.containsKey(sentenceID);
			if (!contains) continue;
			int correspondingID = matchResult.get(sentenceID);
			Map<Mention, Map<String, Mention>> srlResult = extentsWithArgumentRoles.get(correspondingID);
			for (int j = 0; j < allPredictedMentions.get(i).size(); j++) {
				Mention mention = allPredictedMentions.get(i).get(j);
				int start = mention.startIndex;
				int end = mention.endIndex;
				for (Mention predicate : srlResult.keySet()) {
					int predicateStart = predicate.startIndex;
					int predicateEnd = predicate.endIndex;
					if ((predicateStart == start) && (predicateEnd == end)) {
						Map<String, Mention> arguments = srlResult.get(predicate);
						if (arguments.size() == 0) continue;
						
						for (String argKey : arguments.keySet()) {
							Mention argument = arguments.get(argKey);
							
							// match the id
							int argumentStart = argument.startIndex;
							int argumentEnd = argument.endIndex;
							for (int k = 0; k < allPredictedMentions.get(i).size(); k++) {
								Mention mentionMatch = allPredictedMentions.get(i).get(k);
								int mentionMatchStart = mentionMatch.startIndex;
								int mentionMatchEnd = mentionMatch.endIndex;
								
								// it is very tricky here, I need to experiment for a while
								// if ((argumentStart <= mentionMatchStart) && (mentionMatchEnd <= argumentEnd))
								if (!argKey.equals("AM-LOC")) {
								if ((argumentStart == mentionMatchStart) && (mentionMatchEnd == argumentEnd)) {
									mention.setArgument(argKey, mentionMatch);
									mentionMatch.setPredicte(mention);
									mentionMatch.SRLrole = argKey;
									break;
								}} else {
									if ((argumentStart <= mentionMatchStart) && (mentionMatchEnd <= argumentEnd)) {
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
	*/
	
}
