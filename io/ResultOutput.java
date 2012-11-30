package edu.oregonstate.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.Feature;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.general.FixedSizePriorityQueue;
import edu.oregonstate.search.State;
import edu.oregonstate.util.EecbConstants;
import edu.stanford.nlp.dcoref.CorefCluster;
import edu.stanford.nlp.dcoref.Mention;
import edu.stanford.nlp.stats.Counter;

import Jama.Matrix;

public class ResultOutput {

	// write the string to file
	public static void writeTextFile(String fileName, String s) {
	    try {
	    	BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
	        out.write(s);
	        out.newLine();
	        out.close();
	    } catch (IOException e) {
	      e.printStackTrace();
	    } 
	}
	
	// write the string to file
	public static void writeTextFilewithoutNewline(String fileName, String s) {
	    try {
	    	BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
	        out.write(s);
	        out.close();
	    } catch (IOException e) {
	      e.printStackTrace();
	    } 
	}
	
	/** get all the sub-directories under the specific directory */
	public static String[] getTopics(String corpusPath) {
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, corpusPath);
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
	
	/** print the JAMA matrix */
	public static String printModel(Matrix model, String[] featureName) {
		StringBuilder sb = new StringBuilder();
		sb.append("bias weight: " + model.get(0, 0) + "\n");
		for (int i = 0; i < featureName.length; i++) {
			sb.append(featureName[i] + " weight: " + model.get(i+1, 0) + "\n");
		}
		return sb.toString();
	}
	
	public static String printStructredModel(double[] model, String[] featureName) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < featureName.length; i++) {
			sb.append(featureName[i] + " weight: " + model[i] + "\n");
		}
		return sb.toString();
	}
	
	/** print the JAMA matrix */
	public static String printModel(Matrix model) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < model.getRowDimension(); i++) {
			sb.append("weight: " + model.get(i, 0) + "\n");
		}
		return sb.toString().trim();
	}
	
	// put features and quality together in order to create a string for output
	public static String buildString(Counter<String> features, double quality) {
		StringBuilder sb = new StringBuilder();
		boolean add = false;
		for (String feature : Feature.featuresName){
			double value = features.getCount(feature);
			if (value > 0.0) add = true;
			sb.append(value + ",");
		}
		sb.append(quality);
		sb.append("\n");
		if (add) {
			return sb.toString();
		} else {
			return "";
		}
	}
	
	// delete the intermediate result in case of wrong linear model
	// and also delete the whole directory
	public static void deleteResult(String directoryName) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		if (files == null) {
			return;
		} else if (files.length > 0) {
			for (File file : files) {
				if (!file.delete()) {
					System.out.println("Failed to delete "+file);
				}
			}
		}

		boolean delete = directory.delete();
		assert delete == true;
	}
	
	/** just delete the file according to the filePath  */
	public static void deleteFile(String filePath) {
		File file = new File(filePath);
		boolean success = file.delete();
		assert success == true;
	}
	
	public static <T> void serialize(T object, int id, String directory) {
		try
	      {
	         FileOutputStream fileOut = new FileOutputStream(directory + "/" + id +".ser");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(object);
	         out.close();
	         fileOut.close();
	      }catch(IOException i)
	      {
	          i.printStackTrace();
	      }
	}
	
	public static <T> void serialize(T object, String id, String directory) {
		try {
			FileOutputStream fileOut = new FileOutputStream(directory + "/" + id +".ser");
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(object);
			out.close();
			fileOut.close();
		} catch (IOException i) {
			i.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String fileName, String directory, boolean delete) {
		T cluster = null;
		try
        {
           FileInputStream fileIn = new FileInputStream(directory + "/" +  fileName + ".ser");
           ObjectInputStream in = new ObjectInputStream(fileIn);
           cluster = (T) in.readObject();
           in.close();
           fileIn.close();
       }catch(IOException i) {
           i.printStackTrace(); 
       }catch(ClassNotFoundException c)
       {
           c.printStackTrace();
           System.exit(1);
       }
       if (delete) {
    	   deleteFile(directory + "/" + fileName + ".ser");
       }
       
       return cluster;
	}
	
	/**
	 * print the cluster information
	 * 
	 * @param clusters
	 * @return
	 */
	public static String printCluster(Map<Integer, CorefCluster> clusters) {
		StringBuilder sb = new StringBuilder();
		for (Integer key : clusters.keySet()) {
			CorefCluster cluster = clusters.get(key);
			sb.append(Integer.toString(key) + "[ ");
			for (Mention mention : cluster.getCorefMentions()) {
				sb.append(mention.mentionID + " ");
			}
			sb.append(" ]");
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	/** print the current time in order to know the duration of the experiment */
	public static void printTime() {
		String timeStamp = Calendar.getInstance().getTime().toString().replaceAll("\\s", "-");
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, timeStamp);
		ResultOutput.writeTextFile(ExperimentConstructor.logFile, "\n\n");
	}
	
	/** print the beam information, because beam is represented as priority queue, we just print the id */
	public static String printBeam(FixedSizePriorityQueue<State<CorefCluster>> beam) {
		List<State<CorefCluster>> elements = beam.getElements();
		double[] priorities = beam.getPriorities();
		
		assert elements.size() == priorities.length;
		StringBuilder sb = new StringBuilder();
		sb.append("beam: \n");
		for (int i = 0; i < elements.size(); i++) {
			State<CorefCluster> state = elements.get(i);
			double priority = priorities[i];
			sb.append(priority + " " + state.toString() + "\n");
		}
		return sb.toString();
	}
}
