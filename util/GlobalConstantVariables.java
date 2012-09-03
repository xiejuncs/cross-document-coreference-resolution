package edu.oregonstate.util;

/** 
 * Define the global constant in this class:
 * global constant can be file path
 * @author xie
 *
 */
public class GlobalConstantVariables {

	// define the EECB corpus path. There are forty four folders in this directory.
	// Each folder is a topic. There are several documents to describe the topic. So we need to do coreference resolution 
	// on those documents referring to the same topic
	public static final String CORPUS_PATH = "corpus/EECB2.0/data/";
	public static final String WHOLE_CORPUS_PATH = "corpus/EECB2.0/data/";
	public static final String MENTION_ANNOTATION_PATH = "corpus/mentions.txt";
	public static final String WORD_SIMILARITY_PATH = "corpus/sims.lsp";
	public static final String WORD_NET_CONFIGURATION_PATH = "corpus/file_properties.xml";
	public static final String RESULT_PATH = "corpus/RESULT/";
	public static final String TOKENS_OUTPUT_PATH = "corpus/tokenoutput/";
	
}
