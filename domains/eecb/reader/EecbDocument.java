package edu.oregonstate.domains.eecb.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;
import java.util.List;
import java.util.logging.Logger;

import edu.oregonstate.domains.eecb.EecbReader;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntity;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEntityMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceEventMention;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.AceToken;

/**
 * Stores the EECB elements annotated in this Document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbDocument extends EecbElement {
	/** document preifx */
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
	
	public void setPrefix(String mPrefix) {
		this.mPrefix = mPrefix;
	}
	
	public int getSentenceCount() {
	    return mSentenceEntityMentions.size();
	}
	
	public List<EecbToken> getSentence(int index) {
	    return mSentences.get(index);
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
	public static EecbDocument parseDocument(String prefix, String annotation) throws IOException {
		mLog.info("Reading Document : " + prefix);
		mLog.info("Reading Annotation file from : " + annotation);
		String[] array = prefix.split("/");
		String id = array[3] + "-" + array[4];
		EecbDocument doc = new EecbDocument(id);
		// read the raw text
		doc.setPrefix(prefix);
		doc.readRawText(prefix);
		// Input the gold mention into the document according to the annotation file
		
		
		return doc;
	}
	
	/**
	 * According to the EECB specification, parse one document
	 */
	public static EecbDocument parseDocument(File f) {
		String fileID = f.getName();
		EecbDocument eecbDoc = new EecbDocument(fileID);
		HashMap<String, ArrayList<String>> annotations = readAnnotation();
		// get the document's annotation in order to get the gold entities and events
		ArrayList<String> annotation = annotations.get(fileID);
		
		
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
	 * Read the raw text
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
			}
			entitiesBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		mRawText = sb.toString();
	}
	
	
}
