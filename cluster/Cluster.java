package edu.oregonstate.cluster;

import java.util.List;
import java.util.ArrayList;
import Jama.Matrix;

import edu.oregonstate.data.EecbClusterDocument;

/**
 * cluster representation
 * 
 * Jun Xie(xiejuncs@gmail.com)
 */
public class Cluster {

	public int mID;
	public List<EecbClusterDocument> documents;
	public List<Cluster> children;
	
	public Cluster(int id) {
		mID = id;
		documents = new ArrayList<EecbClusterDocument>();
		children = new ArrayList<Cluster>();
	}
	
	public void addDocument(EecbClusterDocument document) {
		documents.add(document);
	}
	
	public void addDocuments(List<EecbClusterDocument> docus) {
		documents.addAll(docus);
	}
	
	public void addChildrens(List<Cluster> child) {
		children.addAll(child);
	}
	
	public List<EecbClusterDocument> getDocuments() {
		return documents;
	}
	
	public void addChildren(Cluster cluster) {
		children.add(cluster);
	}
	
	public List<Cluster> getChildren() {
		return children;
	}
	
	public int getID() {
		return mID;
	}
	
	/** to = to + from, and then delete from*/
	public static void mergeClusters(Cluster to, Cluster from) {
		int toID = to.getID();
		to.addChildren(to);
		for (EecbClusterDocument m : from.getDocuments()) {
			to.addDocument(m);
		}
		to.addChildren(from);
		System.out.println("merge clusters :" + toID + " <----- " + from.getID());
	}
}
