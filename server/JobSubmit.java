package edu.oregonstate.server;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * submit the jobs to cluster automatically
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class JobSubmit {

	public static void main(String[] args) throws Exception {
		String originalPath = "/nfs/guille/xfern/users/xie/Experiment/experiment/";
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		//get current date time with Date()
		Date date = new Date();
		String folderName = dateFormat.format(date);
		System.out.println(folderName);
		
		String folderPath = originalPath + folderName;
		Runtime.getRuntime().exec("chmod -R u+x " + folderPath);
		
		File corpusDir = new File(folderPath);
		String[] directories = corpusDir.list();
		
		//submit job
		for (String directory : directories) {
			if (directory.startsWith("Job")) continue;
			String simplePath = folderPath + "/" + directory + "/simple.sh";
			System.out.println(simplePath);
			Runtime.getRuntime().exec("qsub " + simplePath);
		}
		System.out.println("done!");
	}
}