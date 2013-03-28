package edu.oregonstate.example;


public class Weight {

	public static void main(String[] args) {
		String line = " 3650    4163    2934";
		line = line.trim();
		System.out.println(line);
		String[] elements = line.split("\\s+");
		for (String element : elements) {
			System.out.println(element);
		}
	}
	
	
}
