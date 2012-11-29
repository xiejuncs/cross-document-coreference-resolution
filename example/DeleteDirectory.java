package edu.oregonstate.example;

import java.io.File;

public class DeleteDirectory {

	public static void main(String[] args) {
		String path = "/scratch/JavaFile/corpus/TEMPORYRESUT/Thu-Nov-22-13:14:34-PST-2012-StochasticGradientConsideringBeam-10-BeamSearch-1-300-2.5/documentobject";
		File directory = new File(path);
		
		boolean delete = directory.delete();
		assert delete == true;
		
		System.out.println("done");
	}
}
