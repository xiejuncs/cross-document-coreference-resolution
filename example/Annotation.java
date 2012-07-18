package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.oregonstate.util.GlobalConstantVariables;

/**
 * Read the document, and strip the annotation tag
 * then according to the mention file, add the annotation 
 * to the plain text we've got recently
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Annotation {

	public static String filename = GlobalConstantVariables.MENTION_ANNOTATION_PATH;
	// store them into the data strcture
	public ArrayList<ArrayList<String>> corpus = new ArrayList<ArrayList<String>>();
	public ArrayList<EntityMention> entity = new ArrayList<EntityMention>();
	public ArrayList<EventMention> event = new ArrayList<EventMention>();
	
	class EntityMention {
		public String topicID;
		public String documentID;
		public String sentenceNumber;
		public String corefID;
		public String startIndex;
		public String endIndex;
		public String startCharIndex;
		public String endCharIndex;
		public String mentionText;
		
		public EntityMention(String topicID, String documentID,
							String sentenceNumber, String corefID, String startIndex, String endIndex,
							String startCharIndex, String endCharIndex, String mentionText) {
			this.topicID = topicID;
			this.documentID = documentID;
			this.sentenceNumber = sentenceNumber;
			this.corefID = corefID;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.startCharIndex = startCharIndex;
			this.endCharIndex = endCharIndex;
			this.mentionText = mentionText;
		}
		
		@Override
		public String toString() {
			return "EntityMention: [" + topicID + ", " + documentID + ", " +
					sentenceNumber + ", " + corefID + ", " + startIndex + ", " +
					endIndex + ", " + startCharIndex + ", " + endCharIndex +  ", " + mentionText +"]";
		}
	}
	
	class EventMention {
		public String topicID;
		public String documentID;
		public String sentenceNumber;
		public String corefID;
		public String startIndex;
		public String endIndex;
		public String startCharIndex;
		public String endCharIndex;
		public String mentionText;
		
		public EventMention (String topicID, String documentID,
							String sentenceNumber, String corefID, String startIndex, String endIndex,
							String startCharIndex, String endCharIndex, String mentionText) {
			this.topicID = topicID;
			this.documentID = documentID;
			this.sentenceNumber = sentenceNumber;
			this.corefID = corefID;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.startCharIndex = startCharIndex;
			this.endCharIndex = endCharIndex;
			this.mentionText = mentionText;
		}
		
		@Override
		public String toString() {
			return "Event: [" + topicID + ", " + documentID + ", " +
					sentenceNumber + ", " + corefID + ", " + startIndex + ", " +
					endIndex + ", " + startCharIndex + ", " + endCharIndex +  ", " + mentionText +"]";
		}
	}
	
	// add annotation to the the plain text
	public void addAnnotation(String name, String[] record) throws IOException {
		ArrayList<String> document = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(name));
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				line = line.replaceAll("\\<[^\\>]*\\>", "");
				document.add(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		String type = record[0];
		String topicID = record[1];
		String documentID = record[2];
		String sentenceNumber = record[3];
		String corefID = record[4];
		String startIndex = record[5];
		String endIndex = record[6];
		String startCharIndex = record[7];
		String endCharIndex = record[8];
		System.out.println(Integer.parseInt(startCharIndex) + "-" + Integer.parseInt(endCharIndex));
		System.out.println(document.get(Integer.parseInt(sentenceNumber)));
		System.out.println(document.get(Integer.parseInt(sentenceNumber)).substring(Integer.parseInt(startCharIndex), Integer.parseInt(endCharIndex)));
		String mentionText = document.get(Integer.parseInt(sentenceNumber)).substring(Integer.parseInt(startCharIndex), Integer.parseInt(endCharIndex));
		
		if (!type.equals("V")) {
			// Event
			EntityMention em = new EntityMention(topicID, documentID, sentenceNumber, corefID, startIndex, endIndex, startCharIndex, endCharIndex, mentionText);
			entity.add(em);
		} else {
			// Entity
			EventMention em = new EventMention(topicID, documentID, sentenceNumber, corefID, startIndex, endIndex, startCharIndex, endCharIndex, mentionText);
			event.add(em);
		}
		corpus.add(document);
	}
	
	public static void main(String[] args) throws IOException {
		Annotation anno = new Annotation();
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				if (line.startsWith("#")) continue;
				System.out.println(line);
				String[] record = line.split("\t"); // the separator is \t
				String topic = record[1];
				String document = record[2];
				String name = "corpus/EECB1.0/data/" + topic + "/" + document + ".eecb"; // get file name
				anno.addAnnotation(name, record);
			}
			entitiesBufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println(anno.entity);
	}

}
