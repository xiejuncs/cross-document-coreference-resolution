package edu.oregonstate.domains.eecb.reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;
import java.util.List;
import java.util.logging.Logger;

import edu.oregonstate.domains.eecb.EecbReader;

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
				sb.append(line);
			}
			entitiesBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		mRawText = sb.toString();
	}
}
