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

import edu.oregonstate.CDCR;
import edu.oregonstate.features.Feature;
import edu.oregonstate.util.Constants;
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
		ResultOutput.writeTextFile(CDCR.outputFileName, corpusPath);
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
	public static void deleteResult(String directoryName) {
		File directory = new File(directoryName);
		File[] files = directory.listFiles();
		if (files == null) return;
		for (File file : files) {
			if (!file.delete()) {
				System.out.println("Failed to delete "+file);
			}
		}
	}
	
	/** just delete the file according to the filePath  */
	public static void deleteFile(String filePath) {
		File file = new File(filePath);
		boolean success = file.delete();
		assert success == true;
	}
	
	// serialize the 
	public static <T> void serialize1(T object) {
		try
	      {
	         FileOutputStream fileOut = new FileOutputStream(CDCR.resultPath + "object.ser");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(object);
	         out.close();
	         fileOut.close();
	      }catch(IOException i)
	      {
	          i.printStackTrace();
	      }
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize1(boolean delete) {
		T cluster = null;
		try
        {
           FileInputStream fileIn = new FileInputStream(CDCR.resultPath + "object.ser");
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
    	   ResultOutput.deleteResult(CDCR.resultPath);
       }
       return cluster;
	}
	
	public static <T> void serialize(T object, int id, String directory) {
		try
	      {
	         FileOutputStream fileOut = new FileOutputStream(directory + id +".ser");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(object);
	         out.close();
	         fileOut.close();
	      }catch(IOException i)
	      {
	          i.printStackTrace();
	      }
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(String fileName, String directory, boolean delete) {
		T cluster = null;
		try
        {
           FileInputStream fileIn = new FileInputStream(directory + fileName);
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
    	   deleteFile(directory + fileName);
       }
       
       return cluster;
	}
	
}
