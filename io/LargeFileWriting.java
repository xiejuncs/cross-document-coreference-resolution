package edu.oregonstate.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/** 
 * write large data set to a output file
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LargeFileWriting {

	/* File Path */
	private final String mPath;
	
	public LargeFileWriting(String path) {
		mPath = path;
	}
	
	/**
	 * write arrays to file
	 * 
	 * @param records
	 */
	public void writeArrays(List<String> records) {
		try {
			writeRaw(records);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writeRaw(List<String> records) throws IOException {
                File file = new File(mPath);
                try {
                        System.out.print("Writing raw...\n");
                        FileWriter writer = new FileWriter(file, true);
                        for (String record: records) {
                                writer.write(record);
                                writer.write("\n");
                        }
                        writer.flush();
                        writer.close();
                } finally {
                        
                }
        }
	
	public static void main(String[] args) {
		int RECORD_COUNT = 4000000;
		String RECORD = "Help I am trapped in a fortune cookie factory";
		List<String> records = new ArrayList<String>();
		for (int i = 0; i < RECORD_COUNT; i++) {
	        records.add(RECORD);
	    }
		String path = "example.txt";
		
		LargeFileWriting writer = new LargeFileWriting(path);
		for (int i = 0; i < 2; i++) {
			writer.writeArrays(records);
		}
	}
}
