package edu.oregonstate.ie.dcoref;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;

import edu.oregonstate.CRC_MAIN;
import edu.oregonstate.domains.eecb.EecbReader;
import edu.oregonstate.util.EECB_Constants;
import edu.oregonstate.util.GlobalConstantVariables;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.dcoref.Document;
import edu.stanford.nlp.dcoref.Semantics;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.trees.Tree;

public class EECBMentionExtractor extends MentionExtractor {
	private EecbReader eecbReader;
	
	private String corpusPath;
	protected int fileIndex = 0;
	protected String[] files;
	
	private static Logger logger = CRC_MAIN.logger;
	
	public EECBMentionExtractor(LexicalizedParser p, Dictionaries dict, Properties props, Semantics semantics) throws Exception {
		super(dict, semantics);
		stanfordProcessor = loadStanfordProcessor(props);
		
		corpusPath = props.getProperty(EECB_Constants.EECB_PROP, GlobalConstantVariables.CORPUS_PATH);
		eecbReader = new EecbReader(stanfordProcessor, false);
		
		eecbReader.setLoggerLevel(Level.INFO);
		files = read(corpusPath);
	}
	
	public Document nexDoc() throws Exception {
		List<List<CoreLabel>> allWords = new ArrayList<List<CoreLabel>>();
		List<List<Mention>> allGoldMentions = new ArrayList<List<Mention>>();
		List<List<Mention>> allPredictedMentions;
		List<Tree> allTrees = new ArrayList<Tree>();
		Annotation anno;
		
		try {
			String filename="";
		    while(files.length > fileIndex){
		        if(files[fileIndex].contains("apf.xml")) {
		        	filename = files[fileIndex];
		            fileIndex++;
		            break;
		        }
		        else {
		            fileIndex++;
		            filename="";
		        }
		    }
		    if(files.length <= fileIndex && filename.equals("")) return null;
		    
		   anno = eecbReader
			
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		
	}
	
	// Define the two variables for obtaining the list of file names
	public static int spc_count = 1;
	private static ArrayList<String> file;
	
	/**
	 * Given a corpus path, get the list of file names resides in the folder and its sub-folder
	 * 	 * 
	 * @param corpusPath
	 * @return
	 */
	private String[] read(String corpusPath) {
		file = new ArrayList<String>();
		File aFile = new File(corpusPath);
		Process(aFile);
		String[] fileArray = file.toArray(new String[file.size()]);
		return fileArray;
	}
	
	/**
	 * Iterate the folder and its sub-folder.
	 * If the File object is a directory, recursive
	 * If the File object is a file, then get its name
	 * 
	 * @param aFile
	 */
	public void Process(File aFile) {
		spc_count++;
		String spcs = "";
		for (int i = 0; i < spc_count; i++)
		      spcs += " ";
		if(aFile.isFile())
		    file.add(aFile.getParent() + "/" + aFile.getName());
			//System.out.println(spcs + "[FILE] " + aFile.getParent() + "/" + aFile.getName());
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
	
}
