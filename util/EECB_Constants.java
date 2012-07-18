package edu.oregonstate.util;

/**
 * define the constants used in the EECB corpus
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class EECB_Constants {

	public static final String EECB_PROP = "dcoref.eecb";
	public static final String SCORE_PROP = "dcoref.score";
	
	/** if false, use mention prediction */
	public static final boolean USE_GOLD_MENTIONS = false;
	
	public static final String MENTION_FINDER_PROP = "dcoref.mentionFinder";
	
	/** if true, skip coreference resolution. do mention detection only */
	  public static final boolean SKIP_COREF = false;
	
	
}
