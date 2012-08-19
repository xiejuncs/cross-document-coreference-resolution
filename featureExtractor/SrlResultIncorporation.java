package edu.oregonstate.featureExtractor;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import edu.oregonstate.data.EecbCharSeq;
import edu.oregonstate.data.EecbEntityMention;
import edu.oregonstate.data.EecbEventMention;
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
	 * match two sentence and its id
	 * <p>
	 * here we just use the simple match, iterate the two sentence find
	 * 
	 * @param lRawText
	 * @param sentenceidToSentence
	 * @return Map<Integer, Integer> the former is the sentence id in the raw text, and the later is the sentence id in the 
	 *								 transformed result
	 */
	private void matchSentence(List<String> lRawText) {
		// we need to make sure there exists one-one correspondence between the original text and output sentence
		assert lRawText.size() == sentenceidToSentence.size();
		for (int i = 0; i < lRawText.size(); i++) {
			String sentence = lRawText.get(i);
			char[] chars = sentence.toCharArray();
			int textLength = chars.length;
			StringBuilder originalSentencewithoutSpace = new StringBuilder();
			for (int j = 0; j < textLength; j++) {
				char character = chars[j];
				String charac = Character.toString(character);
				if (!charac.equals(" "))	originalSentencewithoutSpace.append(charac);
			}
			String originalsent = originalSentencewithoutSpace.toString().trim();
			for (Integer key : sentenceidToSentence.keySet()) {
				List<String> tokens = sentenceidToSentence.get(key);
				// concatenate the tokens together
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < tokens.size(); j++) {
					sb.append(tokens.get(j));
				}
				String sent = sb.toString().trim();
				if (originalsent.contains(sent)) {
					matchResult.put(i, key);
					break;
				}
			}
		}
		
		/**ensure that every sentence has its corresponding SRL annotations*/
		assert matchResult.size() == lRawText.size();
	}
	
	/**
	 * align the SRL result with the document to make sure that the event has the according arguments
	 * there is  mis-match between the true arguments and the result outputted by SRL
	 * so we need to look at byteStart and byteEnd instead of token. Because different tokenization method
	 * has different tokenization results
	 * 
	 * @param document
	 * @param matchResult
	 */
	public void alignSRL(List<String> lRawText, HashMap<String, EecbEventMention> events ) {
		/** get the match result */
		matchSentence(lRawText);
		
		// in order to distinguish from the gold mention id
		int idoffset = 50000;
		for (String key : events.keySet()) {
			EecbEventMention eventMention = events.get(key);
			int sentenceID = eventMention.sentenceID();
			EecbCharSeq anchor = eventMention.getAnchor();
			int start = anchor.getByteStart();
			int end = anchor.getByteEnd();
			int correspondingID = matchResult.get(sentenceID);
			Map<SrlAnnotation, Map<String, List<SrlAnnotation>>> srlResult = extentsWithArgumentRoles.get(correspondingID);
			// for each predicate in the sentence
			for (SrlAnnotation predicate : srlResult.keySet()) {
				int predicateStart = predicate.getStartOffset();
				int predicateEnd = predicate.getEndOffset();
				if ((predicateStart == start) && (predicateEnd == end)) {
					Map<String, List<SrlAnnotation>> arguments = srlResult.get(predicate);
					if (arguments.size() == 0) continue;
					
					for (String argKey : arguments.keySet()) {
						List<SrlAnnotation> argument = arguments.get(argKey);
						StringBuilder sb = new StringBuilder();
						for (SrlAnnotation token : argument) {
							String word = token.getText();
							sb.append(word + " ");
						}
						//String mentionText = sb.toString().trim();
						int starOffset = argument.get(0).getStartOffset();
						int endOffset = argument.get(argument.size() - 1).getEndOffset();
						EecbCharSeq extent = new EecbCharSeq(sb.toString().trim(), starOffset, endOffset, sentenceID);
						EecbEntityMention em = new EecbEntityMention(Integer.toString(idoffset), extent, null, sentenceID);
						eventMention.addArg(em, argKey);
						idoffset++;
					}
					// do not need to go through all the documents, just once
					break;
				}
			}
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
			String originalsent = sborigi.toString().trim();
			for (Integer key : sentenceidToSentence.keySet()) {
				List<String> tokens = sentenceidToSentence.get(key);
				// concatenate the tokens together
				StringBuilder sb = new StringBuilder();
				for (int j = 0; j < tokens.size(); j++) {
					sb.append(tokens.get(j));
				}
				String sent = sb.toString().trim();
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
	public void alignSRL(List<List<Mention>> allPredictedMentions) {
		matchSenten(allPredictedMentions);
		
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
							StringBuilder sb = new StringBuilder();
							for (SrlAnnotation token : argument) {
								String word = token.getText();
								sb.append(word + " ");
							}
							mention.setArgument(argKey, sb.toString());
						}
						// do not need to go through all the documents, just once
						break;
					}
				}
			}
		}
		
	}
	
	
}
