package edu.oregonstate.experiment.dataset;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.experiment.IDataSet;
import edu.oregonstate.featureExtractor.SrlResultIncorporation;
import edu.oregonstate.util.DocumentMerge;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.Document;

/**
 * use the within result produced by Stanford Multi-Sieve System, and then incorporate them together
 * do coreference resolution to train a heuristic weight
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class WithinCross implements IDataSet {
		
	/** corpus path */
	private static String corpusPath;
	
	/** srl path */
	private static String srlPath;
	
	static{
		corpusPath = (String) ExperimentConstructor.mParameters.get(EecbConstants.DATASET).get("corpusPath");
		srlPath = (String) ExperimentConstructor.mParameters.get(EecbConstants.DATASET).get("srlpath");
	}
	
	public WithinCross() {
	}
	
	@Override
	public Document getData(String[] topics) {
		Document corpus = new Document();
		IDocument documentExtraction = new SingleDocument();
		DocumentMerge dm; 
		for (String topic : topics) {
			List<String> files  = getSortedFileNames(topic);
			for (String file : files) {
				try {
					String path = corpusPath + topic + "/" + file;
					Document document = documentExtraction.getDocument(path);
					// combine the documents together
					dm = new DocumentMerge(document, corpus);
					dm.mergeDocument();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			
			srlIncorporation(topic, corpus);              // incorporate the srl result
			corpus.fill();                                // incorporate SYNONYM
		}
		
		return corpus;
	}
	
	/** incorporate the srl result into the Corpus mentions */
	private void srlIncorporation(String topic, Document corpus) {
		SrlResultIncorporation srlResult = new SrlResultIncorporation(srlPath + topic + ".output");
		srlResult.alignSRL(corpus.predictedOrderedMentionsBySentence);
	}
	
	
	/** get files orded by the file name */
	protected List<String> getSortedFileNames(String topic) {
		List<String> files  = new ArrayList<String>();
		String topicPath = corpusPath + topic + "/";
		files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
		sort(files);
		return files;
	}
	
	/** sort the names of the files */
	private void sort(List<String> files) {
		Integer[] numbers = new Integer[files.size()];
		for (int i = 0; i < files.size(); i++) {
			numbers[i] = Integer.parseInt(files.get(i).substring(0, files.get(i).length() - 5));
		}
		Arrays.sort(numbers);
		for (int i = 0; i < numbers.length; i++) {
			files.set(i, Integer.toString(numbers[i]) + ".eecb");
		}
	}
	
}
