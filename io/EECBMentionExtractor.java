package edu.oregonstate.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.featureExtractor.SrlResultIncorporation;
import edu.oregonstate.io.EecbReader;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.ie.machinereading.structure.EntityMention;
import edu.stanford.nlp.ie.machinereading.structure.EventMention;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.EntityTypeAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.IndexAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.UtteranceAnnotation;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.semgraph.SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.dcoref.MentionExtractor;

/**
 * Extract single topic <COREF> mentions from eecb corpus  
 */
public class EECBMentionExtractor extends EmentionExtractor {

	private String topicPath;
	protected ArrayList<String> files;


	public EECBMentionExtractor(String topic, LexicalizedParser p, Dictionaries dict, Properties props, Semantics semantics, boolean goldonly) throws Exception {
		super(dict, semantics, goldonly);
		stanfordProcessor = loadStanfordProcessor(props);
		baseID = 10000000 * Integer.parseInt(topic);
		goldBaseID = 10000000 * Integer.parseInt(topic);
		String dataPath = ExperimentConstructor.corpusPath + "corpus/EECB1.0/data/";
		topicPath = dataPath + "" + topic + "/";
		eecbReader = new EecbReader(stanfordProcessor, false);
		eecbReader.setLoggerLevel(Level.INFO);
		files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
		sort(files);   // Output [1.eecb, 2.eecb, 3.eecb, 4.eecb, 5.eecb]
	}

	/** sort the files name according to the sequence*/
	public void sort(ArrayList<String> files) {
		Integer[] numbers = new Integer[files.size()];
		for (int i = 0; i < files.size(); i++) {
			numbers[i] = Integer.parseInt(files.get(i).substring(0, files.get(i).length() - 5));
		}
		Arrays.sort(numbers);
		for (int i = 0; i < numbers.length; i++) {
			files.set(i, Integer.toString(numbers[i]) + ".eecb");
		}
	}

	/**
	 * We represent topic as Document class. For example, if there are two documents included 
	 * in this class. Then we need to concatenate them together. As a whole, we do coreference 
	 * resolution. In this way, there are less works involved in this situation.
	 *  
	 *  @param mentionExtractor extractor for each document
	 *  @return topic represented by each topic
	 * extract all mentions in one doc cluster, represented by Mention 
	 */
	public Document inistantiate(String topic) throws Exception {
		List<List<CoreLabel>> allWords = new ArrayList<List<CoreLabel>>();
		List<List<Mention>> allGoldMentions = new ArrayList<List<Mention>>();
		List<List<Mention>> allPredictedMentions = null;
		List<Tree> allTrees = new ArrayList<Tree>();
		Annotation anno = new Annotation("");
		CoNLL2011DocumentReader.Document conllDocument = new CoNLL2011DocumentReader.Document();
		try {
			// call the eecbReader
			anno = eecbReader.read(files, topic);
			stanfordProcessor.annotate(anno);

			List<CoreMap> sentences = anno.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				List<String[]> currentSentence = new ArrayList<String[]>();
				int i = 1;

				for (CoreLabel w : sentence.get(TokensAnnotation.class)) {

					// Add by Jun Xie, create the CoNLL format
					String[] words = new String[13];
					words[0] = topic;
					words[1] = "0";
					words[2] = (i - 1) + "";
					words[3] = w.get(TextAnnotation.class);
					words[4] = w.get(PartOfSpeechAnnotation.class);
					words[5] = "-";
					words[6] = "-";
					words[7] = "-";
					words[8] = "-";
					words[9] = "-";
					words[10] = "-";
					words[11] = "-";
					words[12] = "-";

					w.set(IndexAnnotation.class, i++);
					if(!w.containsKey(UtteranceAnnotation.class)) {
						w.set(UtteranceAnnotation.class, 0);
					}
					currentSentence.add(words);
				}
				conllDocument.addSentence(currentSentence);

				allTrees.add(sentence.get(TreeAnnotation.class));
				allWords.add(sentence.get(TokensAnnotation.class));
				EntityComparator comparator = new EntityComparator();
				EventComparator eventComparator = new EventComparator();
				extractGoldMentions(sentence, allGoldMentions, comparator, eventComparator);
			}

			// judge whether there are mis-annotation in the data
			deleteSameAnnotation(allGoldMentions);

			// Gold Mention or predicted Mentions
			if (goldOnly) { 	
				allPredictedMentions = new ArrayList<List<Mention>>();
				for (int i = 0; i < allGoldMentions.size(); i++) {
					List<Mention> sentence = new ArrayList<Mention>();
					for (int j = 0; j < allGoldMentions.get(i).size(); j++) {
						Mention mention = allGoldMentions.get(i).get(j);
						ResultOutput.serialize(mention, mention.mentionID, mentionRepositoryPath);
						Mention copyMention = ResultOutput.deserialize(Integer.toString(mention.mentionID), mentionRepositoryPath, true);
						copyMention.goldCorefClusterID = -1;
						sentence.add(copyMention);

					}
					allPredictedMentions.add(sentence);
				}

			} else {
				// set the mention id here
				allPredictedMentions = mentionFinder.extractPredictedMentions(anno, baseID, dictionaries);
			}

			/** according to the extraction result, print the original document with different annotation */
			printRawDoc(sentences, allPredictedMentions, false);
			printRawDoc(sentences, allGoldMentions, true);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		MentionExtractor dcorfMentionExtractor = new MentionExtractor(dictionaries, semantics);
		dcorfMentionExtractor.setCurrentDocumentID(topic);
		Document document = dcorfMentionExtractor.arrange(anno, allWords, allTrees, allPredictedMentions, allGoldMentions, true);		
		document.extractGoldCorefClusters();
		document.conllDoc = conllDocument;
		return document;
	}

	/** 
	 * there is some mis-annotation included in the corpus, if two mentions of the same sentence included 
	 * the same start index and end index, then there is mis-annotation, then remove the two mentions
	 * 
	 * @param allGoldMentions
	 */
	public void deleteSameAnnotation(List<List<Mention>> allGoldMentions) {
		List<List<Integer>> allRepeats = new ArrayList<List<Integer>>();

		for(int sentNum = 0; sentNum < allGoldMentions.size(); sentNum++) {
			List<Integer> repeats = new ArrayList<Integer>();
			List<Mention> golds = allGoldMentions.get(sentNum);

			Map<String, List<Integer>> statistics = new HashMap<String, List<Integer>>();
			for (int i = 0; i < golds.size(); i++) {
				Mention gold = golds.get(i);
				int start = gold.startIndex;
				int end = gold.endIndex;
				String key = start + "-" + end;
				if (!statistics.containsKey(key)) {
					statistics.put(key, new ArrayList<Integer>());
				}

				statistics.get(key).add(i);
			}

			for (String key : statistics.keySet()) {
				List<Integer> ids = statistics.get(key);
				if (ids.size() > 1) {
					repeats.addAll(ids);
				}
			}

			Collections.sort(repeats, Collections.reverseOrder());
			allRepeats.add(repeats);
		}

		for(int sentNum = 0; sentNum < allGoldMentions.size(); sentNum++) {
			List<Mention> golds = allGoldMentions.get(sentNum);
			List<Integer> repeats = allRepeats.get(sentNum);

			for (Integer deleteKey : repeats) {
				golds.remove(deleteKey.intValue());
			}
		}

	}

	@Override
	public String toString() {
		return "EECBMentionExtractor: [ topicPath : " + topicPath + ", Length of file pool : " + files.size() +"]"; 
	}


	/**
	 * add closest mention to the mention
	 * 
	 * @param orderedMentionBySentence
	 */
	public static void addClosetMentions(List<List<Mention>> orderedMentionBySentence) {
		for (int i = 0; i < orderedMentionBySentence.size(); i++) {
			List<Mention> orderedMentions = orderedMentionBySentence.get(i);
			for (int j = 0; j < orderedMentions.size(); j++) {
				Mention mention = orderedMentions.get(j);

				// just focus on event mentions
				if (!mention.isVerb) continue;

				if (j == 0) {
					mention.closetLeft = null;
					int k = 1;
					while (k < orderedMentions.size()) {
						Mention rightMention = orderedMentions.get(k);
						if (!rightMention.isVerb) {
							mention.closetRight = rightMention;
							break;
						}
						k++;
					}

				} else if (j == (orderedMentions.size() - 1)) {
					mention.closetRight = null;
					int k = j - 1;
					while (k >= 0) {
						Mention leftMention = orderedMentions.get(k);
						if (!leftMention.isVerb) {
							mention.closetLeft = leftMention;
							break;
						}
						k--;
					}
				} else {
					int left = j - 1;
					int right = j + 1;

					while (right < orderedMentions.size()) {
						Mention rightMention = orderedMentions.get(right);
						if (!rightMention.isVerb) {
							mention.closetRight = rightMention;
							break;
						}
						right++;
					}

					while (left >= 0) {
						Mention leftMention = orderedMentions.get(left);
						if (!leftMention.isVerb) {
							mention.closetLeft = leftMention;
							break;
						}
						left--;
					}
				}
			}
		}
	}
	
	/**
	 * align the srl result with the predicted mentions
	 * 
	 * @param topic
	 * @param predictedOrderedMentionsBySentence
	 * @param srlPath
	 */
	public static void srlIncorporation(String topic, List<List<Mention>> predictedOrderedMentionsBySentence) {
		String corpusPath = ExperimentConstructor.corpusPath;
		String srlPath = corpusPath + "corpus/tokenoutput/";
		
		SrlResultIncorporation srlResult = new SrlResultIncorporation(srlPath + topic + ".output");
		srlResult.alignSRL(predictedOrderedMentionsBySentence);
	}
	
}
