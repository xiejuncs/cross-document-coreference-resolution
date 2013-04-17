package edu.oregonstate.classifier.svm;

import edu.oregonstate.classifier.svm.libsvm.svm;
import edu.oregonstate.classifier.svm.libsvm.svm_model;

/**
 * An interface to LibSVM
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LibSVMInterface {

	
	public static void main(String[] args) {
		String filePath = "/scratch/Software/libsvm-3.17/tools/prune.model";
		
		svm_model model = null;
		try {
			model = svm.svm_load_model(filePath);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("done");
	}
}
