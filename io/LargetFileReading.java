package edu.oregonstate.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.oregonstate.experiment.ExperimentConstructor;
import edu.oregonstate.features.FeatureFactory;
import edu.oregonstate.util.EecbConstants;

/**
 * Read file from document
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class LargetFileReading {

	// experiment property setting
	private final Properties prop;
	
	// whether use binary (true) or text file (false),
	private final boolean binary;
	
	// feature size
	private final int featureSize;
	
	public LargetFileReading() {
		prop = ExperimentConstructor.experimentProps;
		binary = Boolean.parseBoolean(prop.getProperty(EecbConstants.IO_BINARY_PROP, "false"));
		featureSize = FeatureFactory.getFeatures().length;
	}
	
	/**
	 * read data
	 * 
	 * @param path
	 * @return
	 */
	public List<List<List<double[]>>> readData(String path) {
		List<List<List<double[]>>> dataset = new ArrayList<List<List<double[]>>>();
		
		if (binary) {
			// binary input
			
		} else {
			// text input
			dataset = processTextData(path);
		}
		
		return dataset;
	}
	
	/**
	 * process the text data from the sparse representation of features
	 * 
	 * @param path
	 * @return
	 */
	private List<List<List<double[]>>> processTextData(String path) {
		List<List<List<double[]>>> dataset = new ArrayList<List<List<double[]>>>();
		List<String> record = readTextData(path);
		List<List<List<String>>> records = processTextData(record);
		List<List<String>> goodRecord = records.get(0);
		List<List<String>> badRecord = records.get(1);
 		assert goodRecord.size() == badRecord.size();
 		
 		List<List<double[]>> goodFeatures = new ArrayList<List<double[]>>();
 		List<List<double[]>> badFeatures = new ArrayList<List<double[]>>();
 		
 		// add data
 		for (int i = 0; i < goodRecord.size(); i++) {
 			List<String> goodrec = goodRecord.get(i);
 			List<String> badrec = badRecord.get(i);
 			List<double[]> goodFeature = processString(goodrec);
 			if (isAllZero(goodFeature)) continue;
 			goodFeatures.add(goodFeature);
 			badFeatures.add(processString(badrec));
 		}
 		
 		assert goodFeatures.size() == badFeatures.size();
 		dataset.add(goodFeatures);
 		dataset.add(badFeatures);
		
		return dataset;
	}
	
	/**
	 * is the vector zero vector
	 * 
	 * @param goodFeature
	 * @return
	 */
	public boolean isAllZero(List<double[]> goodFeature) {
		boolean isallzero = true;
		double[] feature = goodFeature.get(0);
		for (double feat : feature) {
			if (feat > 0) {
				isallzero = false;
			}
		}
		
		return isallzero;
	}
	
	/**
	 * process the records into the data
	 * 
	 * @param records
	 * @return
	 */
	private List<double[]> processString(List<String> records) {
		List<double[]> dataset = new ArrayList<double[]>();
		
		for (String record : records) {
			double[] feature = new double[featureSize];
			if (record.length() != 1) {
				String[] features = record.split("\t");
				for (int i = 1; i < features.length; i++) {
					String featureString = features[i];
					String[] featureStringDetail = featureString.split(":");
					int index = Integer.parseInt(featureStringDetail[0]);
					double value = Double.parseDouble(featureStringDetail[1]);
					feature[index] = value;
				}
				
			}
			dataset.add(feature);
		}
		
		return dataset;
	}
	
	/**
	 * get list of records
	 * 
	 * @param record
	 * @return
	 */
	private List<List<List<String>>> processTextData(List<String> record) {
		List<List<List<String>>> records = new ArrayList<List<List<String>>>();
		List<List<String>> goodRecord = new ArrayList<List<String>>();
		List<List<String>> badRecord = new ArrayList<List<String>>();
		
		List<String> goodData = new ArrayList<String>();
		List<String> badData = new ArrayList<String>();
		for (int i = 0; i < record.size(); i++) {
			String rec = record.get(i);
			
			if (rec.equals("NEWDATASET")) {
				if (goodData.size() != 0) {
					goodRecord.add(goodData);
					badRecord.add(badData);
					goodData = new ArrayList<String>();
					badData = new ArrayList<String>();
				}
			} else if (rec.startsWith("G")) {
				goodData.add(rec);
			} else {
				badData.add(rec);
			}

			if (i == record.size() - 1) {
				goodRecord.add(goodData);
				badRecord.add(badData);
			}
		}
		
		records.add(goodRecord);
		records.add(badRecord);
		return records;
	}
	 
	/**
	 * read data
	 * 	
	 * @param path
	 * @return
	 */
	private List<String> readTextData(String path) {
		List<String> dataset = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.equals("")) {
					continue;
				}
				
				dataset.add(line);
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return dataset;
	}
	
	/**
	 * read data from byte file
	 * 
	 * @param path
	 * @return
	 */
	private List<double[]> readByteData(String path) {
		DataInputStream dis = null;
		List<double[]> dataset = new ArrayList<double[]>();
		int length = FeatureFactory.getFeatures().length;
		try {
			dis = new DataInputStream(new FileInputStream(path));
			double[] datas = new double[2 * length];
			int i = 0;
			while (true) {
				double data = dis.readDouble();
				char ch = dis.readChar();
				datas[i] = data;
				if (ch == '\n') {
					dataset.add(datas);
					datas = new double[2 * length];
					i = 0;
					continue;
				}
				i++;
			}
		} catch (EOFException eof) {
			
		} catch (FileNotFoundException noFile) {
			
		} catch (IOException io) {
			
		} catch (Throwable anything) {
			
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException ignored) {
				}
			}

		}
		
		return dataset;
	}
	
}
