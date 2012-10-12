package edu.oregonstate.cluster;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;

import edu.oregonstate.general.CounterMap;

/**
 * Implementation of tf-idf-weighted term vector representation.
 * 
 * the clustering algorithm uses the vector-space model to represent each document.
 * In this model, each document $d$ is considered to be a vector in the term-space. 
 * In particular, the algorithm employed the tf-idf term weighting model, in which 
 * each document can be represented as ($tf_{1}\log(n/df_{1}))$,....). where $tf_{i}$
 * is the frequence of the ith term in the document and $df_{i}$ is the number of 
 * documents that contain the $i$th term. To account for documents of different lengths,
 * the length of each document vector is normalized so that it is of unit length $|d| = 1$
 * 
 * Jun Xie(xiejuncs@gmail.com)
 */
public class TFIDF {

	private List<List<String>> mDocuments;
	public Set<String> dictionary;
	private Map<String, Integer> wordTotalCount;
	private Map<String, List<Integer>> invertedIndex;
	private Map<String, Map<Integer, Integer>> wordCount;
	private CounterMap<String, Integer> tfidf; // word and document index
	private int documentCount;
	
	/**
	 * initialize all fields 
	 * 
	 * @param documents : each document contains a list of string
	 * @param titles : all titles name
	 */
	public TFIDF(List<List<String>> documents) {
		mDocuments = documents;
		documentCount = mDocuments.size();
		dictionary = new HashSet<String>();
		invertedIndex = new HashMap<String, List<Integer>>();
		wordCount = new HashMap<String, Map<Integer, Integer>>();
		tfidf = new CounterMap<String, Integer>();
		wordTotalCount = new HashMap<String, Integer>();
	}
	
	/**
	 * build TF IDF
	 * 
	 * @return retrun normalized tfidf vector
	 */
	public CounterMap<String, Integer> buildTFIDF() {
		buildDictionary();
		index();
		calculateTFIDF();
		
		return tfidf;
	}
	
	/**
	 * calculate the tfidf
	 */
	private void calculateTFIDF() {
		for (String token : wordCount.keySet()) {
			Map<Integer, Integer> docFreq = wordCount.get(token);
			int tokenFreq = invertedIndex.get(token).size();
			for (Integer doc : docFreq.keySet()) {
				Integer count = docFreq.get(doc);
				double w = 0.0;
				if (count > 0) {
					w = 1 + Math.log10(count);
				}
				Double value = w * Math.log10(documentCount/tokenFreq);
				tfidf.setCount(token, doc, value);
			}
			
		}
	}
	
	/**
	 * build inverted Index
	 */
	private void index() {
		Iterator<String> it = dictionary.iterator();
		while (it.hasNext()) {
			String token = it.next();
			List<Integer> posting = new ArrayList<Integer>();
			Map<Integer, Integer> count = new HashMap<Integer, Integer>();
			for (int i = 0; i < mDocuments.size(); i++) {
				List<String> document = mDocuments.get(i);
				if (document.contains(token)) {
					posting.add(i);
				}
				int occurance = Collections.frequency(document, token);
				count.put(i, occurance);
				wordCount.put(token, count);
			}
			invertedIndex.put(token, posting);
		}
	}
	
	/**
	 * terms that appear in a single document were removed <b>NOTE</b> processing in later part
	 */
	private void buildDictionary() {
		for (int i = 0; i < mDocuments.size(); i++) {
			List<String> document = mDocuments.get(i);
			for (String token : document) {
				boolean contains = wordTotalCount.containsKey(token);
				int count = 0;
				if (contains) {
					count = wordTotalCount.get(token);
				}
				wordTotalCount.put(token, (count + 1));
			}
		}
		
		// delete the token with the count being 1
		Iterator<String> it = wordTotalCount.keySet().iterator();
		while (it.hasNext()) {
			String token = it.next();
			if (wordTotalCount.get(token) < 2) {
				it.remove();
			}
		}
		
		dictionary = wordTotalCount.keySet();
	}
	
}
