package edu.oregonstate;

import java.io.FileInputStream;
import java.util.Properties;

import edu.oregonstate.io.EECBMentionExtractorSingleDocument;
import edu.oregonstate.io.EmentionExtractor;
import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.Document;

public class CorefSystemSingleDocument extends CorefSystem {
	
	@Override
	public Document getDocument(String singleDocument) throws Exception {
		EmentionExtractor mentionExtractor = null;
	    mentionExtractor = new EECBMentionExtractorSingleDocument(singleDocument, parser, corefSystem.dictionaries(), props, corefSystem.semantics());
	    
	    assert mentionExtractor != null;
	    // Set mention finder
	    String mentionFinderClass = props.getProperty(Constants.MENTION_FINDER_PROP);
	    if (mentionFinderClass != null) {
	        String mentionFinderPropFilename = props.getProperty(Constants.MENTION_FINDER_PROPFILE_PROP);
	        CorefMentionFinder mentionFinder;
	        if (mentionFinderPropFilename != null) {
	            Properties mentionFinderProps = new Properties();
	            mentionFinderProps.load(new FileInputStream(mentionFinderPropFilename));
	            mentionFinder = (CorefMentionFinder) Class.forName(mentionFinderClass).getConstructor(Properties.class).newInstance(mentionFinderProps);
	        } else {
	            mentionFinder = (CorefMentionFinder) Class.forName(mentionFinderClass).newInstance();
	        }
	        
	        mentionExtractor.setMentionFinder(mentionFinder);
	    }
	    if (mentionExtractor.mentionFinder == null) {
	        System.out.println("No mention finder specified, but not using gold mentions");
	    }
	    // Parse one document at a time, and do single-doc coreference resolution in each
	    Document document = mentionExtractor.inistantiateSingleDocument(singleDocument);
	    
	    return document;
	}
	
}
