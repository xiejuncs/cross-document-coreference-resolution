package edu.oregonstate.example;

import java.io.*;
import java.util.logging.*;

public class LoggerExample {

	public static final Logger logger = Logger.getLogger(LoggerExample.class.getName());
	
	public static void main(String[] args) throws IOException {
		logger.info("example");
	}
}
