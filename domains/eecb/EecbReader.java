package edu.oregonstate.domains.eecb;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import edu.oregonstate.GenericDataSetReader;
import edu.oregonstate.domains.eecb.reader.EecbDocument;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.ie.machinereading.domains.ace.AceReader;
import edu.stanford.nlp.ie.machinereading.structure.AnnotationUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.CoreMap;

/**
 * Simple Wrapper of EECB code to strcture objects
 * 
 * @author xie
 *
 */
public class EecbReader extends GenericDataSetReader {
	private Counter<String> entityCounts;
	private Counter<String> adjacentEntityMentions;
	private Counter<String> eventCounts;
	
	/**
	 * Make an EecbReader
	 */
	public EecbReader() {
		this(null, true);
	}
	
	public EecbReader(StanfordCoreNLP processor, boolean preprocess) {
		super(processor, preprocess, false, true);
		entityCounts = new ClassicCounter<String>();
		adjacentEntityMentions = new ClassicCounter<String>();
		eventCounts = new ClassicCounter<String>();
		
		logger = Logger.getLogger(EecbReader.class.getName());
	    // run quietly by default
	    logger.setLevel(Level.SEVERE);
	}
	
	public Annotation read(String path) throws IOException {
		List<CoreMap> allSentences = new ArrayList<CoreMap>();
		File basePath = new File(path);	
		assert basePath.exists();  // Assert whether the file name exists 
		Annotation corpus = new Annotation("");
		assert basePath.isFile(); // Each file name is not a directory
		
		allSentences.addAll(readDocument(basePath, corpus));
		AnnotationUtils.addSentences(corpus, allSentences);
		return corpus;
	}
	
	/**
	 * Reads in .eecb file
	 * 
	 * @param file
	 * 			a file object of EECB file
	 * @param corpus
	 * @return
	 * @throws IOException
	 */
	private List<CoreMap> readDocument(File file, Annotation corpus) throws IOException {
		String prefix = file.getName().replaceAll(".eecb", "");
		logger.info("Reading Document: " + prefix);
		List<CoreMap> results = new ArrayList<CoreMap>();
		EecbDocument eecbDocument;
		eecbDocument = EecbDocument.parseDocument(prefix, GlobalConstantVariables.MENTION_ANNOTATION_PATH);
		return results;
	}
	
}
