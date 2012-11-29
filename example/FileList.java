package edu.oregonstate.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileList {

	public static void main(String[] args) {
		String path = "../corpus/EECB1.0/data/";
		String[] topics = {"1", "2", "3", "4", "5", "6", "7", "8", "9",
			"10", "11", "12", "13", "14", "16", "18", "19", 
			"20", "21", "22", "23", "24", "25", "26", "27", "28", "29", 
			"30", "31", "32", "33", "34", "35", "36", "37", "38", "39", 
			"40", "41", "42", "43", "44", "45"};
		
		for (String topic : topics) {
			String topicPath = path + topic + "/";
			List<String> files  = new ArrayList<String>();
			files = new ArrayList<String>(Arrays.asList(new File(topicPath).list()));
			System.out.println(topic + " : " + files.size());
		}
	}
}
