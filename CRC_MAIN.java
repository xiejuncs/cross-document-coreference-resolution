package edu.oregonstate;

import java.util.Arrays;
import java.io.File;
import Jama.Matrix;

/**
 * a helper class for EventCoreference and EventCoreferenceBySearch
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class CRC_MAIN {

	public static void printModel(Matrix model, String[] featureName) {
		System.out.println("bias weight: " + model.get(0, 0));
		for (int i = 0; i < featureName.length; i++) {
			System.out.println(featureName[i] + " weight: " + model.get(i+1, 0));
		}
	}
	
	// delete the intermediate result in case of wrong linear model
	public static void deleteResult(String directoryName) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		for (File file : files) {
			if (!file.delete()) {
				System.out.println("Failed to delete "+file);
			}
		}
	}
	
	// GET topics from the corpusPath
	public static String[] getTopics(String corpusPath) {
		File corpusDir = new File(corpusPath);
		String[] directories = corpusDir.list();
		// sort the arrays in order to execute in directory sequence
		// sort string array and sort int array are different.
		// Hence, I need to convert the string array to int array first, and then transform back
		int[] dirs = new int[directories.length];
		for (int i = 0; i < directories.length; i++) {
			dirs[i] = Integer.parseInt(directories[i]);
		}
		Arrays.sort(dirs);
		for (int i = 0; i < directories.length; i++) {
			directories[i] = Integer.toString(dirs[i]);
		}
		return directories;
	}
	
}
