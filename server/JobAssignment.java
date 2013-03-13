package edu.oregonstate.server;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.oregonstate.experiment.ExperimentGeneration;
import edu.oregonstate.util.Command;

/**
 * Submit job and transfer file based on JSCH
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class JobAssignment {
	
	// folder name in the cluster
	private final String mFolderName;

	// folder path in the cluster
	private final String mFolderPath;
	
	// directory path: mFolderPath + "/" + mFolderName
	private final String mDirecotryPath;
	
	public JobAssignment (String path, String folderName) {
		mFolderName = folderName;
		mFolderPath = path;
		mDirecotryPath = mFolderPath + "/" + mFolderName;
	}

	/**
	 * submit the whole experiment to the clusters
	 */
	public void submitExperimentJobs() {
		
		// delete exist directory
		Command.rmdir(mDirecotryPath);
		Command.rmdir("/nfs/guille/xfern/users/xie/Experiment/corpus/TEMPORYRESUT");
		
		// generate each experiment's configuration file, do not execute the configuration file
		ExperimentGeneration generator = new ExperimentGeneration(mFolderPath, mFolderName);
		generator.generateExperimentFiles();
		
		// go to the generated directory, and find out how many experiments are needed to execute
		File experimentDirectory = new File(mDirecotryPath);
		String[] experiments = experimentDirectory.list();
		
		// for each experiment, generate the config file for each job.
		// for example, datageneration has many jobs, then the learn will just have one job
		for (String experiment : experiments) {
			String experimentPath = mDirecotryPath + "/" + experiment;
			Pipeline pipeline = new Pipeline(experimentPath);
			Command.chmod(experimentPath);
			pipeline.generateConfigurationFile();
		}
	}
	
	
	
	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/experiment";

		//get current date time with Date()
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String folderName = dateFormat.format(date);
		
		JobAssignment jobs = new JobAssignment(path, folderName);
		jobs.submitExperimentJobs();
	}
	
}
