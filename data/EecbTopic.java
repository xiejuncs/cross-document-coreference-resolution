package edu.oregonstate.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.logging.Logger;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.MatchException;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetEndAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * Stores the one annotated EECB topic in this topic
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbTopic extends EecbElement {
	
	/** use topic id directly in order to show some information */
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
	
	/** how many documents contained in this topic */
	private List<String> mFiles;

	/** In order to know the correspondence between the topic document and documents contained in this directory */
	Map<String, Integer> documentPositioninTopic;
	Map<Integer, String> sentencePositionDocument;
	
	private int baseID;
	
	static Logger mLog = Logger.getLogger(EecbTopic.class.getName());
	
	public EecbTopic(String id, List<String> files) {
		super(id);
		mFiles = files;
		mPrefix = id;
		baseID = 10000000 * Integer.parseInt(id);
		
		mEntities = new HashMap<String, EecbEntity>();
		mEntityMentions = new HashMap<String, EecbEntityMention>();
		mSentenceEntityMentions = new ArrayList<ArrayList<EecbEntityMention>>();
		
		mEvents = new HashMap<String, EecbEvent>();
		mEventMentions = new HashMap<String, EecbEventMention>();
		mSentenceEventMentions = new ArrayList<ArrayList<EecbEventMention>>();
		
		mTokens = new Vector<EecbToken>();
		documentPositioninTopic = new HashMap<String, Integer>();
		sentencePositionDocument = new HashMap<Integer, String>();
		lRawText = new ArrayList<String>();
	}
	
	public HashMap<String, EecbEntityMention> getEntityMentions() {
		return this.mEntityMentions;
	}
	
	public HashMap<String, EecbEventMention> getEventMention() {
		return this.mEventMentions;
	}
	
	public String getRawText() {
		return this.mRawText;
	}
	
	public EecbToken getToken(int i) {
	    return mTokens.get(i);
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
	
	// matchCharSeqs debugging helper function
	private String tokensWithByteSpan(int start, int end) {
	    StringBuffer buf = new StringBuffer();
	    boolean doPrint = false;
	    buf.append("...");
	    for (int i = 0; i < mTokens.size(); i++) {
	      // start printing
	      if (doPrint == false && mTokens.get(i).getByteOffset().start() > start - 20
	          && mTokens.get(i).getByteOffset().end() < end) {
	        doPrint = true;
	      }

	      // end printing
	      else if (doPrint == true && mTokens.get(i).getByteOffset().start() > end + 20) {
	        doPrint = false;
	      }

	      if (doPrint) {
	        buf.append(" " + mTokens.get(i).toString());
	      }
	    }
	    buf.append("...");
	    return buf.toString();
	  }
	
	/**
	   * Matches all relevant mentions, i.e. entities and anchors, to tokens Note:
	   * entity mentions may match with multiple tokens!
	   */
	public void matchCharSeqs() {
		// match the head and extent of entity mentions
		Set<String> keys = mEntityMentions.keySet();
		for (String key : keys) {
			EecbEntityMention m = mEntityMentions.get(key);
			// match the extent charseq to 1+ phrase(s)
			try {
				m.getExtent().match(mTokens);
			} catch (MatchException e) {
				mLog.severe("READER ERROR: Failed to match entity mention extent: " + "[" + m.getExtent().getText() + ", "
						+ m.getExtent().getByteStart() + ", " + m.getExtent().getByteEnd() + ", " + m.sentenceID() + "]");
				System.out.println(mPrefix + "-" +sentencePositionDocument.get(m.sentenceID()));
				mLog.severe("Document tokens: " + tokensWithByteSpan(m.getExtent().getByteStart(), m.getExtent().getByteEnd()));
				mLog.severe("Document prefix: " + mID);
				System.exit(1);
			}
		}

		Set<String> eventKeys = mEventMentions.keySet();
		for (String key : eventKeys) {
			EecbEventMention m = mEventMentions.get(key);
			// match the extent charseq to 1+ phrase(s)
			try {
				m.getExtent().match(mTokens);
			} catch (MatchException e) {
				mLog.severe("READER ERROR: Failed to match event mention extent: " + "[" + m.getExtent().getText() + ", "
						+ m.getExtent().getByteStart() + ", " + m.getExtent().getByteEnd() + "]");
				mLog.severe("Document tokens: " + tokensWithByteSpan(m.getExtent().getByteStart(), m.getExtent().getByteEnd()));
				mLog.severe("Document prefix: " + mID);
				System.exit(1);
			}
			// match the anchor
			try {
				m.getAnchor().match(mTokens);
			} catch (MatchException e) {
				mLog.severe("READER ERROR: Failed to match event mention extent: " + "[" + m.getAnchor().getText() + ", "
						+ m.getAnchor().getByteStart() + ", " + m.getAnchor().getByteEnd() + "]");
				mLog.severe("Document tokens: " + tokensWithByteSpan(m.getExtent().getByteStart(), m.getExtent().getByteEnd()));
				mLog.severe("Document prefix: " + mID);
				System.exit(1);
			}
		}
	}
	  
	//TODO
	// The entry of the class
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
	public void parse() {
		// set prefix
		readRawText();
		// parse the documents
		parseDocument();
		
		// read the EecbTokens
        List<List<EecbToken>> sentences = tokenizeAndSegmentSentences(mRawText);
        
        mSentences = sentences;
        for (List<EecbToken> sentence : sentences) {
        	for (EecbToken token : sentence) {
        		addToken(token);
        	}
        }
        
        // set the tokenOffset for each mentions
        matchCharSeqs();
        
        // construct the mEntityMentions matrix
        Set<String> entityKeys = mEntityMentions.keySet();
        int sentence;
        for (String key : entityKeys) {
            EecbEntityMention em = mEntityMentions.get(key);
            sentence = em.sentenceID();

            // adjust the number of rows if necessary
            while (sentence >= mSentenceEntityMentions.size()) {
                    mSentenceEntityMentions.add(new ArrayList<EecbEntityMention>());
                    mSentenceEventMentions.add(new ArrayList<EecbEventMention>());
            }
            ArrayList<EecbEntityMention> sentEnts = mSentenceEntityMentions.get(sentence);
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
        Set<String> eventKeys = mEventMentions.keySet();
        for (String key : eventKeys) {
            EecbEventMention em = mEventMentions.get(key);
            sentence = em.sentenceID(); // add sentence id

            /*
             * adjust the number of rows if necessary -- if you're wondering why we do
             * this here again, (after we've done it for entities) it's because we can
             * have an event with no entities near the end of the document and thus
             * won't have created rows in mSentence*Mentions
             */
            while (sentence >= mSentenceEntityMentions.size()) {
                    mSentenceEntityMentions.add(new ArrayList<EecbEntityMention>());
                    mSentenceEventMentions.add(new ArrayList<EecbEventMention>());
            }

            // store the event mentions in increasing order
            // (a) first, event mentions with no arguments
            // (b) then by the start position of their head, or
            // (c) if start is the same, in increasing order of ends
            ArrayList<EecbEventMention> sentEvents = mSentenceEventMentions.get(sentence);
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
	}
	
	/**
	 * parse the documents in order to align the mentions
	 * 
	 * @param document
	 * @param matchResult
	 * @param extentsWithArgumentRoles
	 */
	public void parseDocument(){
		// READ the mentions.txt file
		HashMap<String, ArrayList<String>> annotations = readAnnotation();
		ArrayList<String> annotation = annotations.get(mID);
		assert annotation != null;
		HashSet<String> corefMap = getCorefID(annotation);   // each entity V3, N27
		int idOffset = 1;
		// according to every id
		for (String id : corefMap) {
			if (id.startsWith("N")) {
				// Entity
				EecbEntity entity = new EecbEntity(mID + "-" + id);
				for (String anno : annotation) {
					
					//String value = type + ":" + documentID +":" + sentenceNumber + ":" + corefID + ":" + startIndex + ":" + endIndex + ":" + startCharIndex + ":" + endCharIndex;
					// anno N:1:1:27:3:5:13:27
					String[] annos = anno.split("-");
					String key = annos[0] + annos[3];
					if (key.equals(id)) {
						String sentenceID = annos[1] + "-" + annos[2];
						int documentSentenceID = documentPositioninTopic.get(sentenceID);
						String sentence = lRawText.get(documentSentenceID);
						int start = Integer.parseInt(annos[6]);
						int end = Integer.parseInt(annos[7]);
					    String mentionText = getMentionExtent(sentence, start, end);
					    int[] byteoffset = convertByteOffset(sentence, start, end);
					    String ID = mID + "-" + key + "-" + Integer.toString(idOffset + baseID + 100000 * Integer.parseInt(annos[1]));
					    EecbCharSeq mention = new EecbCharSeq(mentionText, byteoffset[0], byteoffset[1], documentSentenceID);
					    EecbEntityMention entityMention = new EecbEntityMention(ID, mention, null, documentSentenceID); // HEAD will be processed later
					    addEntityMention(entityMention);
					    entity.addMention(entityMention);
					    idOffset++;
					}
				}
				addEntity(entity);
			} else {
				// Event
				EecbEvent event = new EecbEvent(mID + "-" + id);
				for (String anno : annotation) {
					String[] annos = anno.split("-");
					String key = annos[0] + annos[3];
					if (key.equals(id)) {
						String sentenceID = annos[1] + "-" + annos[2];
						int documentSentenceID = documentPositioninTopic.get(sentenceID);
						String sentence = lRawText.get(documentSentenceID);
						int start = Integer.parseInt(annos[6]);
						int end = Integer.parseInt(annos[7]);
						String mentionText = getMentionExtent(sentence, start, end);
						int[] byteoffset = convertByteOffset(sentence, start, end);
					    String ID = mID + "-" + key + "-" + Integer.toString(idOffset + baseID + 100000 * Integer.parseInt(annos[1]));
					    EecbCharSeq mention = new EecbCharSeq(mentionText, byteoffset[0], byteoffset[1], documentSentenceID);
					    
					    // because we do not know the extent, so we just use the mention as its extent
					    EecbEventMention eventMention = new EecbEventMention(ID, mention, mention, documentSentenceID);
					    addEventMention(eventMention);
					    event.addMention(eventMention);
					    idOffset++;
					}
				}
				addEvent(event);
			}
		}	
	}

	/**
	 * Due to the different byteoffset used in the mentions.txt and Stanford tokenization tool
	 * Hence, need to transform the byteoffset used in the mentions.txt into Stanford tokenization tool style
	 * <b>Difference</b>
	 * mentions.txt: calculate the byteoffset for each word by eliminating the whitespace
	 * tokenization tool style: calculate the byteoffset for each word with whitespace 
	 * 
	 * @param sentence
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public int[] convertByteOffset(String sentence, int startIndex, int endIndex) {
		// adjust startIndex and endIndex
		int tmp = endIndex;
		String sent = sentence.replaceAll(" ", "");
		if (tmp > sent.length()) {
			tmp = sent.length();
		}
		
		int span = endIndex - startIndex;
		endIndex = tmp;
		startIndex = tmp - span;
		
		
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
	
	/**
	 * just read the annotated example
	 */
	private void readRawText() {
		int j = 0;
		// put all documents into one document
		for (String file : mFiles) {
			try {
				String filename = ExperimentConstructor.DATA_PATH + mID + File.separator + file;
				BufferedReader br = new BufferedReader(new FileReader(filename));
				int i = 0;
				// filename : 1.eecb
				for (String line = br.readLine(); line != null; line = br.readLine()) {
					String strline = line.replaceAll("\\<[^\\>]*\\>", "");
					if (strline.length() < line.length()) {
						lRawText.add(strline);
						documentPositioninTopic.put(file.substring(0, file.length() - 5) + "-" + Integer.toString(i), j);
						sentencePositionDocument.put(j, file.substring(0, file.length() - 5) + "-" + Integer.toString(i));
						j = j + 1;
					}
					i = i + 1;
				}
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(1);
			}
		}
		
		StringBuilder sb = new StringBuilder();
        StringBuilder nsb = new StringBuilder();
        for (String line : lRawText) {
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
	public List<List<EecbToken>> tokenizeAndSegmentSentences(String rawText) {
		List<List<EecbToken>> sentences = new ArrayList<List<EecbToken>>();
		String[] sens = rawText.split("\n");
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		for (int i = 0; i < sens.length; i++) {
			List<EecbToken> sentence = new ArrayList<EecbToken>();
			String sen = sens[i];
		    Annotation seAnno = new Annotation(sen);
		    pipeline.annotate(seAnno);
		    List<CoreMap> seSentences = seAnno.get(SentencesAnnotation.class);
		    for(CoreMap ses : seSentences) {
		    	boolean newline = true;
		    	for (int j = 0; j < ses.get(TokensAnnotation.class).size(); j++) {
		    		CoreLabel token = ses.get(TokensAnnotation.class).get(j);
		    		String word = token.getString(TextAnnotation.class);
		    		int start = token.get(CharacterOffsetBeginAnnotation.class);
		    		int end = token.get(CharacterOffsetEndAnnotation.class);
		    		EecbToken eecbToken = new EecbToken(word, "", "", start, end, i);
		    		sentence.add(eecbToken);
		    		
		    		//if (j == (ses.get(TokensAnnotation.class).size() - 1)) newline = false;
		    		//String tokens = createTokens(j, token, newline);
		    		//Train.writeTextFile(GlobalConstantVariables.TOKENS_PATH + mPrefix + ".tokens", tokens);
		    	}
		    }
		    sentences.add(sentence);
		}
		
		return sentences;
	}
	
	/**
	 * according to the specification of http://barcelona.research.yahoo.net/dokuwiki/doku.php?id=conll2008:format
	 * output the tokens for the Semantic Role Labeling Software in order to create the result
	 * 
	 * @param j
	 * @param token
	 * @return
	 */
	public String createTokens(int j, CoreLabel token, boolean newline) {
		StringBuilder sb = new StringBuilder();
		sb.append(j+1);
		sb.append("\t");
		String word = token.getString(TextAnnotation.class).replace("\\", "");
		sb.append(word + "\t");
		String lemma = token.getString(LemmaAnnotation.class).replace("\\", "");
		if (lemma == "") lemma = "_";
		sb.append(lemma + "\t");
		sb.append("_\t");
		String pos = token.getString(PartOfSpeechAnnotation.class);
		sb.append(pos + "\t");
		sb.append(word + "\t");
		sb.append(lemma + "\t");
		sb.append(pos + "\t");
		sb.append("0\t");
		sb.append("_");
		if (newline)	{
			sb.append("\n");
		} else {
			sb.append("\n\n");
		}
		return sb.toString();
	}
	
	// according to every coref ID, and add the entity mention into the document
	public HashSet<String> getCorefID(ArrayList<String> annotation) {
		HashSet<String> corefMap = new HashSet<String>();
		for (String annos : annotation) {
			String[] anno = annos.split("-");
			String key = anno[0] + anno[3];
			corefMap.add(key);
		}
		return corefMap;
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
		String mentionPath = ExperimentConstructor.MENTION_PATH;    // mentions.txt path
		
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
				String value = type + "-" + documentID + "-" + sentenceNumber + "-" + corefID + "-" + startIndex + "-" + endIndex + "-" + startCharIndex + "-" + endCharIndex;
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
