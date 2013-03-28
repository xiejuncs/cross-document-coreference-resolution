package edu.oregonstate.example;

import edu.oregonstate.util.Command;

public class ReadLearnedWeight {

	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/experiment/2013-03-19/experiment0/4-searchlearnedweightwithfeature-6-config.properties";
		boolean exist = Command.fileExists(path);
		System.out.println(exist);
		
	}
	
}
