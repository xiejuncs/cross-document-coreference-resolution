package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * Read the document, and strip the annotation tag
 * then according to the mention file, add the annotation 
 * to the plain text we've got recently
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EecbAnnotation {

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
		// creates a StanfordCoreNLP object, with POS tagging, lemmatization, NER, parsing, and coreference resolution 
	    
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String sentence = document.get(Integer.parseInt(sentenceNumber));
	    System.out.println(sentence);
	    Annotation anno = new Annotation(sentence);
	    pipeline.annotate(anno);
	    List<CoreMap> sentences = anno.get(SentencesAnnotation.class);
	    ArrayList<String> tokens = new ArrayList<String>();
	    for(CoreMap sen : sentences) {
	    	for (CoreLabel token : sen.get(TokensAnnotation.class)) {
	    		String word = token.get(TextAnnotation.class);
	    		tokens.add(word);
	    	}
	    }
	    System.out.println(tokens.size());
	    StringBuilder sb = new StringBuilder();
	    for (int i = Integer.parseInt(startIndex); i < Integer.parseInt(endIndex); i++) {
	    	sb.append(tokens.get(i) + " ");
	    }
	    String mentionText = sb.toString().trim();
		System.out.println(mentionText);
		
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
		EecbAnnotation anno = new EecbAnnotation();
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
