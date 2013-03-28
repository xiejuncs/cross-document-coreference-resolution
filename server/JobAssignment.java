package edu.oregonstate.server;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import edu.oregonstate.io.ResultOutput;
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
		
		// give user a tip about backing up the experiment results of TEMPORARYRESULT directory
		System.out.println("Do you want to delete the TEMPORYRESULT file? If not, please make a backup for the experiment results.....");
		Scanner scanner = new Scanner(System.in);
		String aggrement = scanner.next();
		if (aggrement.equals("n")) {
			System.exit(1);
		}
		
		// if yes, then delete the TEMPORARYRESULT directory
		Command.rmdir("/nfs/guille/xfern/users/xie/Experiment/corpus/TEMPORYRESUT");
		// create TEMPORYRESULT
		Command.mkdir("/nfs/guille/xfern/users/xie/Experiment/corpus/TEMPORYRESUT");
		
		// generate each experiment's configuration file, do not execute the configuration file
		ExperimentGeneration generator = new ExperimentGeneration(mFolderPath, mFolderName);
		generator.generateExperimentFiles();
		
		Command.mkdir(mDirecotryPath + "/pipeline");
		// go to the generated directory, and find out how many experiments are needed to execute
		File experimentDirectory = new File(mDirecotryPath);
		String[] experiments = experimentDirectory.list();
		
		// for each experiment, generate the config file for each job.
		// for example, datageneration has many jobs, then the learn will just have one job
		for (String experiment : experiments) {
			// omit pipeline
			if (experiment.equals("pipeline")) continue;
			
			String jobConfigPrefix = mDirecotryPath + "/pipeline/" + experiment;
			String mainClasspath = "edu.oregonstate.server.Pipeline";
			String parameter = mDirecotryPath + "/" + experiment;
			generateRunFile(jobConfigPrefix, mainClasspath, parameter);
			
			String mExperimentPath = mDirecotryPath + "/pipeline";
			generateSimpleFile(jobConfigPrefix, mExperimentPath, experiment);
			
			Command.chmod(mExperimentPath);

			ClusterConnection connection = new ClusterConnection();
			try {
				connection.connect();
				String jobSimpleName = jobConfigPrefix + "-simple.sh";

				connection.submitJob(jobSimpleName);
				connection.disconnect();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	
	}
	
	
	/**
	 * generate the run file
	 * 
	 * @param jobConfigPrefix
	 */
	private void generateRunFile(String jobConfigPrefix, String mainClassPath, String parameter) {
		String runPath = jobConfigPrefix + "-run.sh";
		StringBuilder sb = new StringBuilder();
		sb.append("CLASSPATH=/nfs/guille/xfern/users/xie/Experiment/jarfile/cr.jar");                                 // change the name according to the jar file
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/commons-collections-3.2.1.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/commons-io-2.4.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/commons-logging-1.1.1.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/Jama-1.0.3.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/jaws-bin.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/joda-time.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/jsch-0.1.49.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/log4j-1.2.17.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/stanford-corenlp-2012-05-22-models.jar");
		sb.append(":/nfs/guille/xfern/users/xie/Experiment/lib/xom.jar\n");
		sb.append("export CLASSPATH\n");
		sb.append("java -Xmx8g " + mainClassPath + " " + parameter);
		ResultOutput.writeTextFile(runPath, sb.toString());
	}


	private void generateSimpleFile(String jobConfigPrefix, String mExperimentPath, String topic) {
		String simplePath = jobConfigPrefix + "-simple.sh";
		StringBuilder sb = new StringBuilder();
		sb.append("#!/bin/csh\n\n");
		sb.append("# Give the job a name\n");
		sb.append("#$ -N Jun-" + topic + "\n");
		sb.append("# set working directory on all host to\n");
		sb.append("# directory where the job was started\n");
		sb.append("#$ -cwd\n\n");

		sb.append("# send all process STDOUT (fd 2) to this file\n");
		sb.append("#$ -o " + mExperimentPath + "/" + topic + "-screencross.txt\n\n");

		sb.append("# send all process STDERR (fd 3) to this file\n");
		sb.append("#$ -e " +  mExperimentPath + "/" + topic + "-job_outputcross.err\n\n");

		sb.append("# specify the hardware platform to run the job on.\n");
		sb.append("# options are: amd64, em64t, i386, volumejob (use i386 if you don't care)\n");
		sb.append("#$ -q eecs,eecs1,eecs2,share\n\n");

		sb.append("# Commands\n");
		sb.append(jobConfigPrefix + "-run.sh");
		ResultOutput.writeTextFile(simplePath, sb.toString().trim());
	}
	
	
	/**
	 * execute the experiments
	 * 
	 * @param args
	 */
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
