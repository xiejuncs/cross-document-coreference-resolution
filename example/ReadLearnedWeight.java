package edu.oregonstate.example;

import edu.oregonstate.util.Command;

public class ReadLearnedWeight {

	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/experiment/2013-04-03/0-experiment/0-datageneration-1121-simple.sh";
		boolean exist = Command.fileExists(path);
		System.out.println(exist);
		
	}
	
}
