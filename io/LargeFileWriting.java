package edu.oregonstate.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.general.DoubleOperation;
import edu.oregonstate.util.EecbConstants;

/** 
 * write large data set to a output file
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LargeFileWriting {

	/* File Path */
	private final String mPath;

	/* experiment settings */
	private final Properties mProps;

	public LargeFileWriting(String path) {
		mPath = path;
		mProps = ExperimentConstructor.experimentProps;
	}

	/**
	 * write arrays to file
	 * 
	 * @param records
	 */
	public void writeArrays(List<String> records) {
		boolean binary = Boolean.parseBoolean(mProps.getProperty(EecbConstants.IO_BINARY_PROP, "false"));
		
		try {
			// write file into binary form or not
			if (binary) {
				writeRawinByte(records);
			} else {
				writeRawinText(records);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * in its raw form
	 * 
	 * @param records
	 * @throws IOException
	 */
	private void writeRawinText(List<String> records) throws IOException {
		File file = new File(mPath);
		try {
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
	
	/**
	 * into byte form
	 * 
	 * @param records
	 */
	private void writeRawinByte(List<String> records) {
		try {
			System.out.print("Writing byte...\n");
			DataOutputStream dos = new DataOutputStream( new FileOutputStream(mPath));
			for (String record: records) {
				double[] features = DoubleOperation.transformString(record, ",");

				for (int i = 0; i < features.length; i++) {				
					dos.writeDouble(features[i]);

					if ( i == features.length - 1) {
						dos.writeChar('\n');
					} else {
						dos.writeChar('\t');
					}
				}
			}
			
			dos.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	/**
	 * Example to run this class
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int RECORD_COUNT = 4000000;
		String RECORD = "Help I am trapped in a fortune cookie factory";
		List<String> records = new ArrayList<String>();
		for (int i = 0; i < RECORD_COUNT; i++) {
			records.add(RECORD);
		}
		String path = "example.txt";

		LargeFileWriting writer = new LargeFileWriting(path);
		writer.writeArrays(records);
	}
	
}
