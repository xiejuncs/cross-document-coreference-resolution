package edu.oregonstate.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Jama.Matrix;
import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.io.ResultOutput;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.Counter;

/**
 * Algorithm 1 in the paper
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class JointCoreferenceResolution extends IterativeResolution {

	public JointCoreferenceResolution(Document document, Matrix model) {
		super(document, model);
	}
	
	private void fillScore(Map<String, Double> scoreMap) {
		// compute the pair of the entities
		for (int i = 0; i < (clusters.size() - 1); i++) {
			for (int j = 0; j < i; j++) {
				CorefCluster c1 = clusters.get(i);
				CorefCluster c2 = clusters.get(j);
				Mention formerRep = c1.getRepresentativeMention();
				Mention latterRep = c2.getRepresentativeMention();
				if (formerRep.isPronominal() == true || latterRep.isPronominal() == true) continue;
				Counter<String> features = Feature.getFeatures(mdocument, c1, c2, false, mDictionary); // get the feature size
				double value = calculateScore(features);
				if (value > 0.5) {
					scoreMap.put(Integer.toString(i) + "-" + Integer.toString(j), value);
				}
			}
		}
	}
	
	/**
	 * iterative entity/event resolution
	 */
	@Override
	public void merge() {
		Map<String, Double> scoreMap = new HashMap<String, Double>();
		fillScore(scoreMap);
		while(scoreMap.size() > 0) {
			String index = compare_hashMap(scoreMap);
			String[] indexs = index.split("-");
			CorefCluster c1 = clusters.get(Integer.parseInt(indexs[0]));
			CorefCluster c2 = clusters.get(Integer.parseInt(indexs[1]));
			ResultOutput.writeTextFile(ExperimentConstructor.logFile, "another merge----" + c1.getClusterID() + "---->" + c2.getClusterID());
			int removeID = c1.getClusterID();
			CorefCluster.mergeClusters(mdocument, c2, c1, mDictionary);
			mdocument.corefClusters.remove(removeID);
			for (Integer id : mdocument.corefClusters.keySet()) {
            	CorefCluster cluster = mdocument.corefClusters.get(id);
            	cluster.regenerateFeature();
            }
			clusters = new ArrayList<CorefCluster>();
			for (Integer key : mdocument.corefClusters.keySet()) {
				CorefCluster cluster = mdocument.corefClusters.get(key);
				clusters.add(cluster);
			}
			scoreMap = new HashMap<String, Double>();
			fillScore(scoreMap);
		}
	}
	
}
