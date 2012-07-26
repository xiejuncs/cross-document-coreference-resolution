package edu.oregonstate.ie.dcoref;

import java.util.List;
import java.util.Properties;

import edu.oregonstate.domains.eecb.reader.EecbTopic;

import edu.stanford.nlp.dcoref.Constants;
import edu.stanford.nlp.dcoref.CorefMentionFinder;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.dcoref.RuleBasedCorefMentionFinder;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.dcoref.SieveCoreferenceSystem;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.HeadFinder;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;

/**
 * generic mention extractor from a corpus
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EmentionExtractor {

	protected HeadFinder headFinder;
	
	protected String currentDocumentID;
	
	protected Dictionaries dictionaries;
	protected Semantics semantics;
	
	public CorefMentionFinder mentionFinder;
	protected StanfordCoreNLP stanfordProcessor;
	
	/** The maximum mention ID: for preventing duplicated mention ID assignment */
	protected int maxID = -1;
	
	public static final boolean VERBOSE = false;
	
	public EmentionExtractor(Dictionaries dict, Semantics semantics) {
		this.headFinder = new SemanticHeadFinder();
		this.dictionaries = dict;
		this.semantics = semantics;
		this.mentionFinder = new RuleBasedCorefMentionFinder();
	}
	
	/**
	 * Extracts the info relevant for coref from the next document in the corpus
	 * @return List of mentions found in each sentence ordered according to the tree traversal.
	 * @throws Exception 
	 */
	public EecbTopic inistantiate(EmentionExtractor mentionExtractor) throws Exception {
		return null; 
	}
	
	public Document nextDoc() throws Exception {
		return null; 
	}
	
	public void setMentionFinder(CorefMentionFinder mentionFinder)
	{
		this.mentionFinder = mentionFinder;
	}
	
	/** Load Stanford Processor: skip unnecessary annotator */
	protected StanfordCoreNLP loadStanfordProcessor(Properties props) {
	    boolean replicateCoNLL = Boolean.parseBoolean(props.getProperty(Constants.REPLICATECONLL_PROP, "false"));

	    Properties pipelineProps = new Properties(props);
	    StringBuilder annoSb = new StringBuilder("");
	    if (!Constants.USE_GOLD_POS && !replicateCoNLL)  {
	      annoSb.append("pos, lemma");
	    } else {
	      annoSb.append("lemma");
	    }
	    if(Constants.USE_TRUECASE) {
	      annoSb.append(", truecase");
	    }
	    if (!Constants.USE_GOLD_NE && !replicateCoNLL)  {
	      annoSb.append(", ner");
	    }
	    if (!Constants.USE_GOLD_PARSES && !replicateCoNLL)  {
	      annoSb.append(", parse");
	    }
	    String annoStr = annoSb.toString();
	    SieveCoreferenceSystem.logger.info("Ignoring specified annotators, using annotators=" + annoStr);
	    pipelineProps.put("annotators", annoStr);
	    return new StanfordCoreNLP(pipelineProps, false);
	}	  
}
