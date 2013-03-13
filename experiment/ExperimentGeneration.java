package edu.oregonstate.experiment;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import edu.oregonstate.io.ResultOutput;
import edu.oregonstate.util.Command;

/**
 * generate the configuration file
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class ExperimentGeneration {

	private final String mFolderName;

	private final String mDirecotryPath;

	public String[] GOLDMENTION_PROP = {"true", "false"};

	public String[] METHOD_FUNCTION_NUMBER_PROP = {"1"};

	public String[] FEATURE_ATOMIC_NAMES = {"F"};

	public ExperimentGeneration (String path, String folderName) {
		mFolderName = folderName;
		mDirecotryPath = path + "/" + folderName;
	}

	public void generateExperimentFiles() {
		// generate the main folder
		generateMainFolder();

		generateSubFolders();		
	}

	private void generateMainFolder() {
		Command.mkdir(mDirecotryPath);
	}

	private void generateSubFolders() {
		// get its corresponding property
		ExperimentProperties properties = new ExperimentProperties();
		Class propertyClass = properties.getClass();
		Field[] propertyFields = propertyClass.getFields();
		Map<String, String> propertyMap = new HashMap<String, String>();
		for (Field field : propertyFields) {
			try {
				propertyMap.put(field.getName(), field.get(properties).toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// get its specific arguments
		ExperimentArguments arguments = new ExperimentArguments();
		Class argumentClass = arguments.getClass();
		Field[] argumentFields = argumentClass.getFields();
		List<Map<String, List<String>>> argumentMap = new ArrayList<Map<String,List<String>>>();
		for (Field field : argumentFields) {
			try {
				List<String> argument = Arrays.asList(((String[]) field.get(arguments)));
				Map<String, List<String>> specificArgument = new HashMap<String, List<String>>();
				specificArgument.put(field.getName(), argument);
				argumentMap.add(specificArgument);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		int length = argumentMap.size();
		List<Node> combinations = new ArrayList<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		Node initialNode = new Node();
		queue.offer(initialNode);

		int index = 0;
		while (queue.size() > 0) {
			Node node = queue.poll();

			if (index == length) {
				break;
			}

			Map<String, List<String>> array = argumentMap.get(index);
			for (String key : array.keySet()) {
				String configKey = propertyMap.get(key);

				List<String> elements = array.get(key);

				for (String element : elements) {
					Node child = node.cat(configKey + " = " + element);
					queue.offer(child);
					if (child.configuration.size() == length) {
						combinations.add(child);
					}
				}
			}

			if (allSameLength(queue)) {
				index += 1;
			}

		}

		for (int i = 0; i < combinations.size(); i++) {
			String subFolderPath = mDirecotryPath + "/experiment" + i;
			Command.mkdir(subFolderPath);
			
			generateConfigurationFile(subFolderPath, combinations.get(i));
			String prefix = "experiment" + i;
			
			//generateRunFile(subFolderPath, prefix);

			//generateSimpleFile(subFolderPath, prefix);
		}
		
	}

	
	private static boolean allSameLength(Queue<Node> queue) {
		Set<Integer> lengths = new HashSet<Integer>();
		Iterator<Node> iterator = queue.iterator();
		while(iterator.hasNext()) {
			Node node = iterator.next();
			lengths.add(node.configuration.size());
		}
		
		return lengths.size() == 1 ? true : false;
	}

	/**
	 * generate config.properties
	 * 
	 * @param subFolderPath
	 * @param method
	 * @param startRate
	 */
	private void generateConfigurationFile(String subFolderPath, Node combination) {
		String configPath = subFolderPath + "/config.properties";
		StringBuilder sb = new StringBuilder();
		
		List<String> specifications = combination.configuration;
		for (String specifiication : specifications) {
			sb.append(specifiication + "\n");
		}

		ResultOutput.writeTextFile(configPath, sb.toString().trim());
	}

//	private void generateRunFile(String subFolderPath, String prefix) {
//		String runPath = subFolderPath + "/run.sh";
//		String clusterPath = "/nfs/guille/xfern/users/xie/Experiment/experiment/" + mFolderName + "/" + prefix;
//		String command = "java -Xmx8g -jar /nfs/guille/xfern/users/xie/Experiment/jarfile/CoreferenceResolution.jar " + clusterPath + "/config.properties";
//		ResultOutput.writeTextFile(runPath, command);
//	}
//
//	private void generateSimpleFile(String subFolderPath, String prefix) {
//		String simplePath = subFolderPath + "/simple.sh";
//		StringBuilder sb = new StringBuilder();
//		sb.append("#!/bin/csh\n\n");
//		sb.append("# Give the job a name\n");
//		sb.append("#$ -N Jun-coreference-resolution-" + prefix + "\n");
//		sb.append("# set working directory on all host to\n");
//		sb.append("# directory where the job was started\n");
//		sb.append("#$ -cwd\n\n");
//
//		String clusterPath = "/nfs/guille/xfern/users/xie/Experiment/experiment/" + mFolderName + "/" + prefix;
//		sb.append("# send all process STDOUT (fd 2) to this file\n");
//		sb.append("#$ -o " + clusterPath + "/screencross.txt\n\n");
//
//		sb.append("# send all process STDERR (fd 3) to this file\n");
//		sb.append("#$ -e " + clusterPath + "/job_outputcross.err\n\n");
//
//		sb.append("# specify the hardware platform to run the job on.\n");
//		sb.append("# options are: amd64, em64t, i386, volumejob (use i386 if you don't care)\n");
//		sb.append("#$ -q eecs,eecs1,eecs2,share\n\n");
//
//		sb.append("# Commands\n");
//		sb.append(clusterPath + "/run.sh\n");
//		ResultOutput.writeTextFile(simplePath, sb.toString().trim());
//
//	}

	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/experiment/";

		//get current date time with Date()
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String folderName = dateFormat.format(date);
		//String folderName = "2013-01-29";

		ExperimentGeneration generator = new ExperimentGeneration(path, folderName);
		generator.generateExperimentFiles();
	}

}
