package edu.oregonstate.senna;

import java.io.*;

/**
 * Implementaion of how to use senna to get the semantic role labeling
 */
public class Senna {
	public static void main(String[] args) throws Exception {
		String myScript = "senna-linux64 -srl < input.txt > output.txt";
		String[] cmdArray = {"xterm", "-e", myScript};
		Runtime.getRuntime().exec(cmdArray);
		System.out.println(myScript);
	}
}
