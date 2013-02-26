package edu.oregonstate.general;

import java.util.HashSet;
import java.util.Set;

import edu.stanford.nlp.stats.Counter;

/**
 * Set Operation
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SetOperation {

	/**
	 * intersection of the keyset of two counter objects
	 * 
	 * @param formerVector
	 * @param latterVector
	 * @return
	 */
	public static Set<String> intersection(Counter<String> formerVector, Counter<String> latterVector) {
		Set<String> commonElementSet = new HashSet<String>();
		commonElementSet.addAll(formerVector.keySet());
		commonElementSet.retainAll(latterVector.keySet());
		return commonElementSet;
	}
	
	/**
	 * union of the keyset of two counter objects
	 * 
	 * @param formerVector
	 * @param latterVector
	 * @return
	 */
	public static Set<String> union(Counter<String> formerVector, Counter<String> latterVector) {
		Set<String> union = new HashSet<String>();
		union.addAll(formerVector.keySet());
		union.addAll(latterVector.keySet());
		return union;
	}
}
