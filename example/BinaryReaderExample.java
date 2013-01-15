package edu.oregonstate.example;

import java.io.*;
import java.util.*;

public class BinaryReaderExample {

	public static void main(String[] args) throws Exception {
		DataInputStream dis = null;
		List<double[]> dataset = new ArrayList<double[]>();
		try {
			dis = new DataInputStream(new FileInputStream("example.txt"));
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
		
		System.out.println("done");
	}
}
