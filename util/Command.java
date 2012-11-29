package edu.oregonstate.util;

import java.io.File;

/**
 * those commands used for creating file or something else
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Command {

	// create a directory given a path string
	public static void createDirectory(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdir();
		}
	}
	
	
}
