package edu.oregonstate.example;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.oregonstate.general.DoubleOperation;

public class BinaryWriterExample {

	/**
	 * data
	 * @param path
	 * @return
	 */
	private static List<String> readData(String path) {
		long start = System.currentTimeMillis();
		List<String> datas = new ArrayList<String>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				datas.add(line);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000f + " seconds: read text");
		System.out.println(datas.size());
		return datas;
	}
	
	/**
	 * text file
	 * 
	 * @param records
	 * @param mPath
	 * @throws IOException
	 */
	private static void writeRawinText(List<String> records, String mPath) {
		long start = System.currentTimeMillis();
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {

		}
		
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000f + " seconds");
	}
	
	private static void writeRawinByte(List<String> records, String mPath) {
		long start = System.currentTimeMillis();
		try {
			System.out.print("Writing byte...\n");
			DataOutputStream dos = new DataOutputStream( new FileOutputStream(mPath));
			for (String record: records) {
				String[] features = record.split(";");
				String goodFeatures = features[0];
				String badFeature = features[1];
				double[] goodNumericFeatures = DoubleOperation.transformString(goodFeatures, ",");
				double[] badNumericFeatures = DoubleOperation.transformString(badFeature, ",");

				for (int i = 0; i < goodNumericFeatures.length; i++) {
					dos.writeDouble(goodNumericFeatures[i]);
					dos.writeChar('\t');
				}

				for (int i = 0; i < badNumericFeatures.length; i++) {				
					dos.writeDouble(badNumericFeatures[i]);

					if ( i == badNumericFeatures.length - 1) {
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
		
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000f + " seconds");
	}
	
	private static void readByteData(String path) {
		long start = System.currentTimeMillis();
		DataInputStream dis = null;
		List<double[]> dataset = new ArrayList<double[]>();
		try {
			dis = new DataInputStream(new FileInputStream(path));
			double[] datas = new double[78];
			int i = 0;
			while (true) {
				double data = dis.readDouble();
				char ch = dis.readChar();
				datas[i] = data;
				if (ch == '\n') {
					dataset.add(datas);
					datas = new double[78];
					i = 0;
					continue;
				}
				i++;
			}
		} catch (EOFException eof) {
			System.out.println(" >> Normal program termination.");
		} catch (FileNotFoundException noFile) {
			System.err.println("File not found! " + noFile);
		} catch (IOException io) {
			System.err.println("I/O error occurred: " + io);
		} catch (Throwable anything) {
			System.err.println("Abnormal exception caught !: " + anything);
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException ignored) {
				}
			}

		}
		
		System.out.println(dataset.size());
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000f + " seconds");
	}
	
	public static void main(String[] args) {
		
		String path = "testing-1-1-1";
		List<String> dataset = readData(path);
		writeRawinText(dataset, "testing-1-1-1-raw.txt");
		writeRawinByte(dataset, "testing-1-1-1-byte.txt");
		
		readData("testing-1-1-1-raw.txt");
		
		readByteData("testing-1-1-1-byte.txt");
		
	}
	
}
