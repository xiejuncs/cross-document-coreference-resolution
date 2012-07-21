package edu.oregonstate.domains.eecb.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.logging.Logger;

import edu.oregonstate.domains.eecb.EecbReader;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntity;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntityMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEvent;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEventMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceRelationMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceSentenceSegmenter;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceToken;
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
	 * <p>
	 * <b>NOTED</b> Two issues:
	 * First: the annotated entity and event mention
	 * Second: the raw text
	 * 
	 * @param prefix
	 * @return
	 * @throws IOException
	 */
	public static EecbDocument parseDocument(String prefix, String annotation) throws Exception {
		mLog.info("Reading Document : " + prefix);
		mLog.info("Reading Annotation file from : " + annotation);
		EecbDocument doc = null;
		// Input the gold mention into the document according to the annotation file
		doc = parseDocument(new File(prefix + ".eecb"));
		doc.setPrefix(prefix);
		
		// read the EecbTokens
		List<List<EecbToken>> sentences = tokenizeAndSegmentSentences(doc.getRawText());
	    doc.setSentences(sentences);
	    for (List<EecbToken> sentence : sentences) {
	      for (EecbToken token : sentence) {
	        doc.addToken(token);
	      }
	    }
		
	    // construct the mEntityMentions matrix
	    Set<String> entityKeys = doc.mEntityMentions.keySet();
	    int sentence;
	    for (String key : entityKeys) {
	    	EecbEntityMention em = doc.mEntityMentions.get(key);
	    	sentence = doc.mTokens.get(em.getExtent().getTokenStart()).getSentence();
	    	// adjust the number of rows if necessary
	        while (sentence >= doc.mSentenceEntityMentions.size()) {
	          doc.mSentenceEntityMentions.add(new ArrayList<EecbEntityMention>());
	          doc.mSentenceEventMentions.add(new ArrayList<EecbEventMention>());
	        }
	        ArrayList<EecbEntityMention> sentEnts = doc.mSentenceEntityMentions.get(sentence);
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
	    Set<String> eventKeys = doc.mEventMentions.keySet();
	    for (String key : eventKeys) {
	        EecbEventMention em = doc.mEventMentions.get(key);
	        sentence = doc.mTokens.get(em.getExtent().getTokenStart()).getSentence();

	        /*
	         * adjust the number of rows if necessary -- if you're wondering why we do
	         * this here again, (after we've done it for entities) it's because we can
	         * have an event with no entities near the end of the document and thus
	         * won't have created rows in mSentence*Mentions
	         */
	        while (sentence >= doc.mSentenceEntityMentions.size()) {
	          doc.mSentenceEntityMentions.add(new ArrayList<EecbEntityMention>());
	          doc.mSentenceEventMentions.add(new ArrayList<EecbEventMention>());
	        }

	        // store the event mentions in increasing order
	        // (a) first, event mentions with no arguments
	        // (b) then by the start position of their head, or
	        // (c) if start is the same, in increasing order of ends
	        ArrayList<EecbEventMention> sentEvents = doc.mSentenceEventMentions.get(sentence);
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
	    
		return doc;
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
			String key = anno[0] + ":" + anno[2];
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
	
	/**
	 * According to the EECB specification, parse one document
	 */
	public static EecbDocument parseDocument(File f) throws Exception {
		String fileID = getKey(f);
		EecbDocument eecbDoc = new EecbDocument(fileID);
		eecbDoc.readRawText(f.getAbsolutePath());
		// READ the mentions.txt file
		HashMap<String, ArrayList<String>> annotations = readAnnotation();
		// get the document's annotation in order to get the gold entities and events
		ArrayList<String> annotation = annotations.get(fileID);
		assert annotation != null;
		String text = eecbDoc.getRawText();
		
		assert text != null;
		String[] sentences = text.split("\n");
		// [V:4, N:27, N:26, N:45, V:7, N:28, N:21, N:22, V:1, N:23, V:3, V:2], V represents the event, N represents the entity
		HashSet<String> corefMap = getCorefID(annotation);
		
		// according to every id
		for (String id : corefMap) {
			if (id.startsWith("N")) {
				// Entity
				EecbEntity entity = new EecbEntity(id);
				for (String anno : annotation) {
					//String key = topicID + ":" + documentID;
					//String value = type + ":" + sentenceNumber + ":" + corefID + ":" + startIndex + ":" + endIndex + ":" + startCharIndex + ":" + endCharIndex;
					// anno N:1:27:3:5:13:27
					String[] annos = anno.split(":");
					String key = annos[0] + ":" + annos[2];
					if (key.equals(id)) {
						String sentence = sentences[Integer.parseInt(annos[1])];
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
					    for (int i = Integer.parseInt(annos[3]); i < Integer.parseInt(annos[4]); i++) {
					    	sb.append(tokens.get(i) + " ");
					    }
					    String mentionText = sb.toString().trim();
					    String ID = fileID + ":" + annos[1] + ":" + annos[3] + ":" + annos[4];
					    EecbCharSeq mention = new EecbCharSeq(mentionText, Integer.parseInt(annos[3]), Integer.parseInt(annos[4]));
					    EecbEntityMention entityMention = new EecbEntityMention(ID, mention, null); // HEAD will be processed later
					    eecbDoc.addEntityMention(entityMention);
					}
				}
				eecbDoc.addEntity(entity);
			} else {
				// Event
				EecbEvent event = new EecbEvent(id);
				for (String anno : annotation) {
					String[] annos = anno.split(":");
					String key = annos[0] + ":" + annos[2];
					if (key.equals(id)) {
						String sentence = sentences[Integer.parseInt(annos[1])];
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
					    for (int i = Integer.parseInt(annos[3]); i < Integer.parseInt(annos[4]); i++) {
					    	sb.append(tokens.get(i) + " ");
					    }
					    String mentionText = sb.toString().trim();
					    String ID = fileID + ":" + annos[1] + ":" + annos[3] + ":" + annos[4];
					    EecbCharSeq mention = new EecbCharSeq(mentionText, Integer.parseInt(annos[3]), Integer.parseInt(annos[4]));
					    EecbEventMention eventMention = new EecbEventMention(ID, mention, mention);
					    eecbDoc.addEventMention(eventMention);
					}
				}
				eecbDoc.addEvent(event);
			}
		}	
		
		return eecbDoc;
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
				String key = topicID + ":" + documentID;
				String value = type + ":" + sentenceNumber + ":" + corefID + ":" + startIndex + ":" + endIndex + ":" + startCharIndex + ":" + endCharIndex;
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
	
	/**
	 * Read the raw text, can be split by \n for convince
	 * 
	 * @param filename
	 * @throws IOException
	 */
	private void readRawText(String filename) throws IOException {
		StringBuffer sb = new StringBuffer();
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				line = line.replaceAll("\\<[^\\>]*\\>", "");
				sb.append(line);  // whether need to add a \n tag in order to make it obvious
				sb.append("\n");
			}
			entitiesBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		mRawText = sb.toString().trim();
	}

}
