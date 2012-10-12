package edu.oregonstate.cluster;

import java.util.List;
import java.util.ArrayList;
import Jama.Matrix;

import edu.oregonstate.data.Document;

/**
 * cluster representation
 * 
 * Jun Xie(xiejuncs@gmail.com)
 */
public class Cluster {

	public int mID;
	public List<Document> documents;
	public List<Cluster> children;
	
	public Cluster(int id) {
		mID = id;
		documents = new ArrayList<Document>();
		children = new ArrayList<Cluster>();
	}
	
	public void addDocument(Document document) {
		documents.add(document);
	}
	
	public void addDocuments(List<Document> docus) {
		documents.addAll(docus);
	}
	
	public void addChildrens(List<Cluster> child) {
		children.addAll(child);
	}
	
	public List<Document> getDocuments() {
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
		for (Document m : from.getDocuments()) {
			to.addDocument(m);
		}
		to.addChildren(from);
		System.out.println("merge clusters :" + toID + " <----- " + from.getID());
	}
}
