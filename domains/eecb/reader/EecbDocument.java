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

import edu.oregonstate.domains.eecb.EecbReader;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
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
	 * read the eecb file
	 * 
	 * @param files
	 * @return
	 */
	public static EecbDocument parseDocument(List<String> files, String topic) {
		// document is actually a topic, just for convince
		EecbDocument document = new EecbDocument(topic);
		document.readRawText(files, topic);
		// get the document's annotation in order to get the gold entities and events
		parseDocument(document);
		
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
				EecbEntity entity = new EecbEntity(id);
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
						// tokenize the sentence in order to get the annotation entity and event
						Properties props = new Properties();
					    props.put("annotators", "tokenize, ssplit");
					    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
					    Annotation seAnno = new Annotation(sentence);
					    pipeline.annotate(seAnno);
					    List<CoreMap> seSentences = seAnno.get(SentencesAnnotation.class);
					    ArrayList<String> tokens = new ArrayList<String>();
					    for(CoreMap sen : seSentences) {
					    	for (CoreLabel token : sen.get(TokensAnnotation.class)) {
					    		String word = token.get(TextAnnotation.class);
					    		tokens.add(word);
					    	}
					    }
					    StringBuilder sb = new StringBuilder();
					    for (int i = Integer.parseInt(annos[4]); i < Integer.parseInt(annos[5]); i++) {
					    	sb.append(tokens.get(i) + " ");
					    }
					    String mentionText = sb.toString().trim();
					    String ID = documentID + ":" + annos[1] + ":" + annos[2] + ":" + annos[3] + ":" + annos[4] + ":" + annos[5];
					    EecbCharSeq mention = new EecbCharSeq(mentionText, Integer.parseInt(annos[4]), Integer.parseInt(annos[5]));
					    EecbEntityMention entityMention = new EecbEntityMention(ID, mention, null, documentSentenceID); // HEAD will be processed later
					    document.addEntityMention(entityMention);
					    entity.addMention(entityMention);
					}
				}
				document.addEntity(entity);
			} else {
				// Event
				EecbEvent event = new EecbEvent(id);
				for (String anno : annotation) {
					String[] annos = anno.split(":");
					String key = annos[0] + ":" + annos[2];
					if (key.equals(id)) {
						String sentenceID = documentID + ":" + annos[1] + ":" + annos[2];
						int documentSentenceID = document.documentToTopic.get(sentenceID);
						String sentence = sentences.get(documentSentenceID);
						// tokenize the sentence in order to get the annotation entity and event
						Properties props = new Properties();
					    props.put("annotators", "tokenize, ssplit");
					    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
					    Annotation seAnno = new Annotation(sentence);
					    pipeline.annotate(seAnno);
					    List<CoreMap> seSentences = seAnno.get(SentencesAnnotation.class);
					    ArrayList<String> tokens = new ArrayList<String>();
					    for(CoreMap sen : seSentences) {
					    	for (CoreLabel token : sen.get(TokensAnnotation.class)) {
					    		String word = token.get(TextAnnotation.class);
					    		tokens.add(word);
					    	}
					    }
					    StringBuilder sb = new StringBuilder();
					    for (int i = Integer.parseInt(annos[4]); i < Integer.parseInt(annos[5]); i++) {
					    	sb.append(tokens.get(i) + " ");
					    }
					    String mentionText = sb.toString().trim();
					    String ID = documentID + ":" + annos[1] + ":" + annos[2] + ":" + annos[3] + ":" + annos[4] + ":" + annos[5];
					    EecbCharSeq mention = new EecbCharSeq(mentionText, Integer.parseInt(annos[4]), Integer.parseInt(annos[5]));
					    EecbEventMention eventMention = new EecbEventMention(ID, mention, mention, documentSentenceID);
					    document.addEventMention(eventMention);
					    event.addMention(eventMention);
					}
				}
				document.addEvent(event);
			}
		}	
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
		StringBuffer sb = new StringBuffer();
		for (String line : rawText) {
			sb.append(line + "\n");
		}
		mRawText = sb.toString().trim();
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
		    		String lemma = token.get(LemmaAnnotation.class);
		    		EecbToken eecbToken = new EecbToken(word, lemma, Integer.toString(j), Integer.toString(j+1), i);
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
		    		EecbToken eecbToken = new EecbToken(word, "", Integer.toString(start), Integer.toString(start + 1), i); // Need to add the lemma
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