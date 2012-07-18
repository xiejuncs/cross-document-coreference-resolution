package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.sun.xml.internal.ws.util.StringUtils;

import edu.oregonstate.util.GlobalConstantVariables;

/**
 * Example to get all files resides in one directory.
 * in the case of EECB, there are still sub directory .
 * So we need to read the whole path and then segment the path into different sections,
 * for example, the filename and its clusters.
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class DirectoryFileImport {

	public static int spc_count = 1;
	public static ArrayList<String> files;
	
	public void Process(File aFile) {
		spc_count++;
		String spcs = "";
		for (int i = 0; i < spc_count; i++)
		      spcs += " ";
		if(aFile.isFile()) {
			files.add(aFile.getParent() + "/" + aFile.getName());
		    //System.out.println(spcs + "[FILE] " + aFile.getParent() + "/" + aFile.getName());
		}
		else if (aFile.isDirectory()) {
			//System.out.println(spcs + "[DIR] " + aFile.getName());
		      File[] listOfFiles = aFile.listFiles();
		      if(listOfFiles!=null) {
		        for (int i = 0; i < listOfFiles.length; i++)
		          Process(listOfFiles[i]);
		      } else {
		    	  System.out.println(spcs + " [ACCESS DENIED]");
		      }
		}
		spc_count--;
	}
	
	public static void main(String[] args) {
		args = new String[1];
		args[0] = GlobalConstantVariables.CORPUS_PATH;
		files = new ArrayList<String>();
		File aFile = new File(args[0]);
		DirectoryFileImport dir = new DirectoryFileImport();
		dir.Process(aFile);
		String[] file = files.toArray(new String[files.size()]);
		
		// use one file to do experiment
		// The main thing is to get rid of those ENTITY and EVENT annotation
		String filename = file[0];
		System.out.println("Source : " + filename);
		File file1 = new File(filename);
		System.out.println(file1.getName());
		
		try {
			BufferedReader entitiesBufferedReader = new BufferedReader(new FileReader(filename));
			for (String line = entitiesBufferedReader.readLine(); line != null; line = entitiesBufferedReader.readLine()) {
				//System.out.println(line);
				line = line.replaceAll("\\<[^\\>]*\\>", "");
				//System.out.println(line);
			}
			entitiesBufferedReader.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
}
