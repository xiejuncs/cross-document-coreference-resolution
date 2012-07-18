package edu.oregonstate.io;

import java.io.File;
import edu.oregonstate.util.*;

public class ReaderInputFile {

	public static void main(String[] args) {
		
		String path = GlobalConstantVariables.CORPUS_PATH;
		String document;
		
		File corpus = new File(path);
		File[] listofFiles = corpus.listFiles();
		
		for (int i = 0; i < listofFiles.length; i++ ) {
			
			if (listofFiles[i].isDirectory()) {
				
				document = listofFiles[i].getName();
				
				File topic = new File(path + "/" +document);
				
				File[] listofDocuments = topic.listFiles();
				
				for (int j = 0; j < listofDocuments.length; j++) {
					
					String specificDocument = listofDocuments[j].getName();
					
					System.out.println(document + " consist of " + specificDocument);
				}
			}
		}	
		
	}
	
	
}
