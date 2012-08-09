package edu.oregonstate.domains.eecb.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.logging.Logger;

import edu.oregonstate.data.SrlAnnotation;
import edu.oregonstate.domains.eecb.EecbReader;
import edu.oregonstate.example.SemanticOutputInterface;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.tokensregex.types.Expressions.OrExpression;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * Stores the EECB elements annotated in this Document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbDocument extends EecbElement {
	/** 
	 * document preifx
	 * Like this : /scratch/JavaFile/stanford-corenlp-2012-05-22/data/AFP_ENG_20030304.0250
	 * Or : /scratch/JavaFile/stanford-corenlp-2012-05-22/corpus/EECB2.0/data/1/1 (for 1.eecb)
	 */
	private String mPrefix;
	
	/**All entities*/
	private HashMap<String, EecbEntity> mEntities;
	
	/** all entity mentions */
	private HashMap<String, EecbEntityMention> mEntityMentions;
	
	/** All entity mentions in a given sentence, sorted in textual order */
	private ArrayList<ArrayList<EecbEntityMention>> mSentenceEntityMentions;
	
	/** all events */
	private HashMap<String, EecbEvent> mEvents;
	
	/** all event mentions */
	private HashMap<String, EecbEventMention> mEventMentions;
	
	/** all event mentions in a given sentence, sorted in textual order */
	private ArrayList<ArrayList<EecbEventMention>> mSentenceEventMentions;
	
	/** The list of all tokens in the document, sorted in textual order */
	private Vector<EecbToken> mTokens;
	
	/**  List of all sentences in the document  */
	private List<List<EecbToken>> mSentences;
	
	/** The raw byte document, no preprocessing */
	private String mRawText;
	
	/** use a list of string to represent the raw text of a document, each string is a sentence */
	private List<String> lRawText;
	
	/** In order to output the intermediate results to semantic role labeling software. Each line is separated by a new line except the last one */
	private String nRawText;
	
	/** In order to know the correspondence between the topic document and documents contained in this directory */
	Map<Integer, String> topicToDocument;
	Map<String, Integer> documentToTopic;
	
	public void setSentences(List<List<EecbToken>> sentences) {
	    mSentences = sentences;
	}
	
	static Logger mLog = Logger.getLogger(EecbReader.class.getName());
	
	public EecbDocument(String id) {
		super(id);
		mEntities = new HashMap<String, EecbEntity>();
		mEntityMentions = new HashMap<String, EecbEntityMention>();
		mSentenceEntityMentions = new ArrayList<ArrayList<EecbEntityMention>>();
		
		mEvents = new HashMap<String, EecbEvent>();
		mEventMentions = new HashMap<String, EecbEventMention>();
		mSentenceEventMentions = new ArrayList<ArrayList<EecbEventMention>>();
		
		mTokens = new Vector<EecbToken>();
		
		topicToDocument = new HashMap<Integer, String>();
		documentToTopic = new HashMap<String, Integer>();
	}
	
	public String getRawText() {
		return this.mRawText;
	}
	
	public void setPrefix(String mPrefix) {
		this.mPrefix = mPrefix;
	}
	
	public List<String> getlRawText() {
		return this.lRawText;
	}
	
	public int getSentenceCount() {
	    return mSentenceEntityMentions.size();
	}
	
	public List<EecbToken> getSentence(int index) {
	    return mSentences.get(index);
	}
	
	public void addEventMention(EecbEventMention e) {
	    mEventMentions.put(e.getId(), e);
	}
	
	public void addEntityMention(EecbEntityMention em) {
	    mEntityMentions.put(em.getId(), em);
	}
	
	public ArrayList<EecbEntityMention> getEntityMentions(int sent) {
	    return mSentenceEntityMentions.get(sent);
	}
	
	public ArrayList<EecbEventMention> getEventMentions(int sent) {
	    return mSentenceEventMentions.get(sent);
	}
	
	public Set<String> getKeySetEntities() {
	    return mEntities.keySet();
	}
	
	public EecbEntity getEntity(String id) {
	    return mEntities.get(id);
	}
	
	public void addEntity(EecbEntity e) {
	    mEntities.put(e.getId(), e);
	}
	
	public void addEvent(EecbEvent r) {
	    mEvents.put(r.getId(), r);
	}
	
	public void addToken(EecbToken t) {
	    mTokens.add(t);
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
	private static Map<Integer, Integer> matchSentence(List<String> lRawText, Map<Integer, List<String>> sentenceidToSentence) {
		// we need to make sure there exists one-one correspondence between the original text and output sentence
		assert lRawText.size() == sentenceidToSentence.size();
		Map<Integer, Integer> matchResult = new HashMap<Integer, Integer>();
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
				if (originalsent.equals(sent)) {
					matchResult.put(i, key);
					break;
				}
			}
		}
		
		/**ensure that every sentence has its corresponding SRL annotations*/
		assert matchResult.size() == lRawText.size();
		return matchResult;
	}
	
	/**
	 * align the SRL result with the document to make sure that the event has the according arguments
	 * there is  mis-match between the true arguments and the result outputted by SRL
	 * so we need to look at byteStart and byteEnd not token. Because different tokenization method
	 * has different tokenization results
	 * 
	 * @param document
	 * @param matchResult
	 */
	private static void alignSRL(EecbDocument document, Map<Integer, Integer> matchResult, 
			Map<Integer, Map<SrlAnnotation, Map<String, List<SrlAnnotation>>>> extentsWithArgumentRoles) {
		
		HashMap<String, EecbEventMention> events = document.mEventMentions;
		int idOffset = 0;
		for (String key : events.keySet()) {
			
			EecbEventMention eventMention = events.get(key);
			int sentenceID = eventMention.getSentence();
			EecbCharSeq anchor = eventMention.getAnchor();
			int start = anchor.getByteStart();
			int end = anchor.getByteEnd();
			int correspondingID = matchResult.get(sentenceID);
			Map<SrlAnnotation, Map<String, List<SrlAnnotation>>> srlResult = extentsWithArgumentRoles.get(Integer.toString(correspondingID));
			
			if (srlResult == null) continue;
			
			for (SrlAnnotation predicate : srlResult.keySet()) {
				int predicateStart = predicate.getStartOffset();
				int predicateEnd = predicate.getEndOffset();
				if ((predicateStart == start) && (predicateEnd == end)) {
					Map<String, List<SrlAnnotation>> arguments = new HashMap<String, List<SrlAnnotation>>();
					for (String argKey : arguments.keySet()) {
						List<SrlAnnotation> argument = arguments.get(argKey);
						StringBuilder sb = new StringBuilder();
						for (SrlAnnotation token : argument) {
							String word = token.getText();
							sb.append(word + " ");
						}
						String mentionText = sb.toString().trim();
						int starOffset = argument.get(0).getStartOffset();
						int endOffset = argument.get(argument.size() - 1).getEndOffset();
						EecbCharSeq mention = new EecbCharSeq(mentionText, starOffset, endOffset);
					    EecbEntityMention entityMention = new EecbEntityMention(Integer.toString(idOffset), mention, null, sentenceID); // HEAD will be processed later
						idOffset++;
						eventMention.addArg(entityMention, argKey);
					}
					
					// do not need to go through all the documents, just once
					break;
				}
			}

		}
		
	}
	
	/**
	 * read the eecb file, and I need to incorporate the SRL result here
	 * <p>
	 * the reason is that I parse the entity and event here. I need to match them together according to the byteoffset and tokenoffset
	 * sometimes, if I can not match the byteoffset and tokenoffset, then need to shrink the extent in order to match part of the 
	 * annotated mention
	 * 
	 * 
	 * @param files
	 * @return
	 */
	public static EecbDocument parseDocument(List<String> files, String topic) {
		// document is actually a topic, just for convince
		EecbDocument document = new EecbDocument(topic);
		document.readRawText(files, topic);
		// incorporate the SRL result
		
		/** Example Start */
		String file = "corpus/topic1rawtext.output";
		SemanticOutputInterface semantic = new SemanticOutputInterface();
		semantic.setDocument(semantic.read(file));
		Map<Integer, List<List<String>>> doc = semantic.getDocument();
		 
		 
		Map<Integer, Map<SrlAnnotation, Map<String, List<SrlAnnotation>>>> extentsWithArgumentRoles = new HashMap<Integer, Map<SrlAnnotation,Map<String,List<SrlAnnotation>>>>();
		Map<Integer, List<String>> sentenceidToSentence = new HashMap<Integer, List<String>>();
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
		
		// the corresponding between the raw text and the semantic role labeling result
		Map<Integer, Integer> matchResult = matchSentence(document.lRawText, sentenceidToSentence);
		
		/** Example End */
		
		// get the document's annotation in order to get the gold entities and events
		parseDocument(document);
		
		alignSRL(document, matchResult, extentsWithArgumentRoles);   // align the srl result with the document

		// read the EecbTokens
		List<List<EecbToken>> sentences = tokenizeAndSegmentSentences(document.getRawText());
		document.setSentences(sentences);
	    for (List<EecbToken> sentence : sentences) {
	      for (EecbToken token : sentence) {
	    	  document.addToken(token);
	      }
	    }
		
	    // construct the mEntityMentions matrix
	    Set<String> entityKeys = document.mEntityMentions.keySet();
	    int sentence;
	    for (String key : entityKeys) {
	    	EecbEntityMention em = document.mEntityMentions.get(key);
	    	sentence = em.getSentence();
	    	
	    	// adjust the number of rows if necessary
	        while (sentence >= document.mSentenceEntityMentions.size()) {
	        	document.mSentenceEntityMentions.add(new ArrayList<EecbEntityMention>());
	        	document.mSentenceEventMentions.add(new ArrayList<EecbEventMention>());
	        }
	        ArrayList<EecbEntityMention> sentEnts = document.mSentenceEntityMentions.get(sentence);
	        boolean added = false;
	        for (int i = 0; i < sentEnts.size(); i++) {
	          EecbEntityMention crt = sentEnts.get(i);
	          if ((crt.getExtent().getTokenStart() > em.getExtent().getTokenStart())) {
	            sentEnts.add(i, em);
	            added = true;
	            break;
	          }
	        }
	        if (!added) {
	          sentEnts.add(em);
	        }
	    }
	    
	    // construct the mEventMentions matrix
	    Set<String> eventKeys = document.mEventMentions.keySet();
	    for (String key : eventKeys) {
	        EecbEventMention em = document.mEventMentions.get(key);
	        sentence = em.getSentence(); // add sentence id

	        /*
	         * adjust the number of rows if necessary -- if you're wondering why we do
	         * this here again, (after we've done it for entities) it's because we can
	         * have an event with no entities near the end of the document and thus
	         * won't have created rows in mSentence*Mentions
	         */
	        while (sentence >= document.mSentenceEntityMentions.size()) {
	        	document.mSentenceEntityMentions.add(new ArrayList<EecbEntityMention>());
	        	document.mSentenceEventMentions.add(new ArrayList<EecbEventMention>());
	        }

	        // store the event mentions in increasing order
	        // (a) first, event mentions with no arguments
	        // (b) then by the start position of their head, or
	        // (c) if start is the same, in increasing order of ends
	        ArrayList<EecbEventMention> sentEvents = document.mSentenceEventMentions.get(sentence);
	        boolean added = false;
	        for (int i = 0; i < sentEvents.size(); i++) {
	          EecbEventMention crt = sentEvents.get(i);
	          if ((crt.getExtent().getTokenStart() > em.getExtent().getTokenStart())) {
	            sentEvents.add(i, em);
	            added = true;
	            break;
	          }
	        }
	        if (!added) {
	          sentEvents.add(em);
	        }
	    }
		
		return document;
	}
	
	/**
	 * parse the documents in order to align the mentions
	 * 
	 * @param document
	 * @param matchResult
	 * @param extentsWithArgumentRoles
	 */
	public static void parseDocument(EecbDocument document){
		// READ the mentions.txt file
		HashMap<String, ArrayList<String>> annotations = readAnnotation();
		String documentID = document.getId();
		List<String> sentences = document.lRawText;
		ArrayList<String> annotation = annotations.get(documentID);
		
		assert annotation != null;
		HashSet<String> corefMap = getCorefID(annotation);
		
		// according to every id
		for (String id : corefMap) {
			if (id.startsWith("N")) {
				// Entity
				EecbEntity entity = new EecbEntity(documentID + ":" + id);
				int idOffset = 1;
				for (String anno : annotation) {
					//String key = topicID;
					//String value = type + ":" + documentID +":" + sentenceNumber + ":" + corefID + ":" + startIndex + ":" + endIndex + ":" + startCharIndex + ":" + endCharIndex;
					// anno N:1:1:27:3:5:13:27
					String[] annos = anno.split(":");
					String key = annos[0] + ":" + annos[3];
					if (key.equals(id)) {
						String sentenceID = documentID + ":" + annos[1] + ":" + annos[2];
						int documentSentenceID = document.documentToTopic.get(sentenceID);
						String sentence = sentences.get(documentSentenceID);
					    String mentionText = getMentionExtent(sentence, Integer.parseInt(annos[6]), Integer.parseInt(annos[7]));
					    String ID = documentID + ":N" + annos[3] + ":" + Integer.toString(idOffset);
					    EecbCharSeq mention = new EecbCharSeq(mentionText, Integer.parseInt(annos[6]), Integer.parseInt(annos[7]));
					    EecbEntityMention entityMention = new EecbEntityMention(ID, mention, null, documentSentenceID); // HEAD will be processed later
					    document.addEntityMention(entityMention);
					    entity.addMention(entityMention);
					    idOffset++;
					}
				}
				document.addEntity(entity);
			} else {
				// Event
				EecbEvent event = new EecbEvent(documentID + ":" + id);
				int idOffset = 1;
				for (String anno : annotation) {
					String[] annos = anno.split(":");
					String key = annos[0] + ":" + annos[2];
					if (key.equals(id)) {
						String sentenceID = documentID + ":" + annos[1] + ":" + annos[2];
						int documentSentenceID = document.documentToTopic.get(sentenceID);
						String sentence = sentences.get(documentSentenceID);
						String mentionText = getMentionExtent(sentence, Integer.parseInt(annos[6]), Integer.parseInt(annos[7]));
					    String ID = documentID + ":V" + annos[3] + ":" + Integer.toString(idOffset);
					    EecbCharSeq extent = new EecbCharSeq(sentence, 0, getSenteceSize(sentence));
					    EecbCharSeq mention = new EecbCharSeq(mentionText, Integer.parseInt(annos[6]), Integer.parseInt(annos[7]));
					    EecbEventMention eventMention = new EecbEventMention(ID, extent, mention, documentSentenceID);
					    document.addEventMention(eventMention);
					    event.addMention(eventMention);
					    idOffset++;
					}
				}
				document.addEvent(event);
			}
		}	
	}
	
	/** how many characters the sentence has except the whitespace*/
	public static int getSenteceSize(String sentence) {
		sentence = sentence.replaceAll("\\s", "");
		return sentence.length();
	}
	
	/**
	 * According to the EECB corpus mentions.txt format, extract the mention text for sepcific startCharIndex and endCharIndex
	 * 
	 * @param sentence the original text used for extracting the mention extent according to the EECB corpus mentions.txt format
	 * @param startIndex the startCharIndex extracted from the mentions.txt
	 * @param endIndex  the endCharIndex extracted from the mentions.txt
	 * @return the mention extent
	 */
	public static String getMentionExtent(String sentence, int startIndex, int endIndex) {
		int offset = 0;
		char[] chars = sentence.toCharArray();
		int textLength = chars.length;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < textLength; i++) {
			String character = Character.toString(chars[i]);
			if (!character.equals(" ")) offset = offset + 1;
			
			if ((offset > startIndex) && (offset <= endIndex) ) {
				if (offset == endIndex && (character.equals(" "))) continue;
				sb.append(character);
			}
		}
		return sb.toString();
	}
	
	// from f's name, get the combination of its topic and file name
	public static String getKey(String filename, String topic) {
		String key;
		key = topic + ":" + filename;
		key = key.substring(0, key.length() - 5);
		return key;
	}
	
	/**
	 * Read the raw text
	 * 
	 * @param files
	 * @param topic
	 */
	private void readRawText(List<String> files, String topic) {
		List<Integer> lines = new ArrayList<Integer>(); 
		int j = 0;
		List<String> rawText = new ArrayList<String>();
		HashMap<String, ArrayList<String>> annotations = readAnnotation();
		ArrayList<String> annotation = annotations.get(topic);
		for (String filename : files) {
			String documentID = filename.substring(0, filename.length() - 5);
			String key = topic + ":" + documentID;
			ArrayList<String> anno = new ArrayList<String>();
			for (String record : annotation) {
				String[] records = record.split(":");
				if (records[1].equals(documentID)) {
					anno.add(record);
				}
			}
			filename = GlobalConstantVariables.CORPUS_PATH + topic + "/" + filename;
			Integer[] sentences = getSentences(anno);
			int i = 0;
			try {
				BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
				for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
					line = line.replaceAll("\\<[^\\>]*\\>", "");
					boolean contain = false;
					for (Integer sentence : sentences) {
						if (sentence == i) {
							contain = true;
							break;
						}
					}
					if (contain) { 
						rawText.add(line);
						topicToDocument.put(j, key + ":" + Integer.toString(i));
						documentToTopic.put(key + ":" + Integer.toString(i), j);
						j++;
					}
					i++;
				}
				lines.add(i);
				entitiesBufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		lRawText = rawText;
		StringBuilder sb = new StringBuilder();
		StringBuilder nsb = new StringBuilder();
		for (String line : rawText) {
			sb.append(line + "\n");
			nsb.append(line + "\n\n");
		}
		mRawText = sb.toString().trim();
		nRawText = nsb.toString().trim();
	}
	
	/**
	 * Tokenize the raw text
	 * 
	 * @param filenamePrefix
	 * @return
	 */
	public static List<List<EecbToken>> tokenizeAndSegmentSentences(String rawText) {
		List<List<EecbToken>> sentences = new ArrayList<List<EecbToken>>();
		String[] sens = rawText.split("\n");
		for (int i = 0; i < sens.length; i++) {
			List<EecbToken> sentence = new ArrayList<EecbToken>();
			String sen = sens[i];
			Properties props = new Properties();
		    props.put("annotators", "tokenize, ssplit, pos, lemma");
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		    Annotation seAnno = new Annotation(sen);
		    pipeline.annotate(seAnno);
		    List<CoreMap> seSentences = seAnno.get(SentencesAnnotation.class);
		    for(CoreMap ses : seSentences) {
		    	for (int j = 0; j < ses.get(TokensAnnotation.class).size(); j++) {
		    		CoreLabel token = ses.get(TokensAnnotation.class).get(j);
		    		String word = token.get(TextAnnotation.class);
		    		EecbToken eecbToken = new EecbToken(word, "", "", Integer.toString(j), Integer.toString(j+1), i);
		    		sentence.add(eecbToken);
		    	}
		    }
		    sentences.add(sentence);
		}
		
		return sentences;
	}
	
	// from f's name, get the combination of its topic and file name
	public static String getKey(File f) {
		String key = "";
		String path = f.getAbsolutePath();
		String[] paras = path.split("/");
		key = paras[paras.length-2] + ":" + paras[paras.length-1];
		key = key.substring(0, key.length() - 5);
		return key;
	}
	
	// according to every coref ID, and add the entity mention into the document
	public static HashSet<String> getCorefID(ArrayList<String> annotation) {
		HashSet<String> corefMap = new HashSet<String>();
		for (String annos : annotation) {
			String[] anno = annos.split(":");
			String key = anno[0] + ":" + anno[3];
			corefMap.add(key);
		}
		return corefMap;
	}
	
	/**
	 * Read in the sentence, and then parse its word textual form into the EecbToken
	 * 
	 * @param sentences
	 * @return
	 */
	public static List<List<EecbToken>> parseSentense(String[] sentences) {
		List<List<EecbToken>> sens = new ArrayList<List<EecbToken>>();
		for (int i = 0; i < sentences.length; i++) {
			String sentence = sentences[i];
			List<EecbToken> sen = new ArrayList<EecbToken>();
			int start = 0;
			Properties props = new Properties();
		    props.put("annotators", "tokenize, ssplit");
		    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		    Annotation seAnno = new Annotation(sentence);
		    pipeline.annotate(seAnno);
		    List<CoreMap> seSentences = seAnno.get(SentencesAnnotation.class);
		    for(CoreMap seSentence : seSentences) {
		    	for (CoreLabel token : seSentence.get(TokensAnnotation.class)) {
		    		String word = token.get(TextAnnotation.class);
		    		EecbToken eecbToken = new EecbToken(word, "", "", Integer.toString(start), Integer.toString(start + 1), i); // Need to add the lemma
		    		sen.add(eecbToken);
		    		start = start + 1;
		    	}
		    }
		    sens.add(sen);
		}
		return sens;
	}
	
	public static Integer[] getSentences(ArrayList<String> annotation) {
		HashSet<String> sentences = new HashSet<String>();
		for (String annos : annotation) {
			String[] anno = annos.split(":");
			String key = anno[2];
			sentences.add(key);
		}
		
		Integer[] sentenceLines = new Integer[sentences.size()];
		int i = 0;
		for (String sentence : sentences) {
			sentenceLines[i] = Integer.parseInt(sentence);
			i++;
		}
		Arrays.sort(sentenceLines);
		
		return sentenceLines;
	}
	
	/**
	 * <b>NOTE</b>
	 * 
	 * EECB document is annotated by the specification of mentions.txt.
	 * Hence, we need to annotate the plain document according to the mentions.txt
	 * Those annotated entities and events are gold annotations. We need to add those into 
	 * our document class in order to evaluate the accuracy of the proposed algorithms
	 * 
	 * @return
	 */
	public static HashMap<String, ArrayList<String>> readAnnotation() {
		HashMap<String, ArrayList<String>> annotation = new HashMap<String, ArrayList<String>>();
		String mentionPath = GlobalConstantVariables.MENTION_ANNOTATION_PATH;    // mentions.txt path
		
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(mentionPath));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				if (line.startsWith("#")) continue;
				String[] record = line.split("\t"); // the separator is \t
				// get every element out of the current record
				String type = record[0];
				String topicID = record[1];
				String documentID = record[2];
				String sentenceNumber = record[3];
				String corefID = record[4];
				String startIndex = record[5];
				String endIndex = record[6];
				String startCharIndex = record[7];
				String endCharIndex = record[8];
				// check whether annotation HashMap contains the key (topic:documentID), 
				// if contains, add the current string combination (type:sentenceNumber:corefID:startIndex:endIndex:startCharIndex:endCharIndex) to the existed ArrayList
				// if not contains, initialize an empty ArrayList, and add the string combination to the empty ArrayList
				String key = topicID;
				String value = type + ":" + documentID + ":" + sentenceNumber + ":" + corefID + ":" + startIndex + ":" + endIndex + ":" + startCharIndex + ":" + endCharIndex;
				boolean contains = annotation.containsKey(key);
				ArrayList<String> values = new ArrayList<String>();
				if (contains) {
					values = annotation.get(key);
					
				}
				values.add(value);
				annotation.put(key, values);
			}
			entitiesBufferedReader.close();
		} catch (Exception e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return annotation;
	}

}