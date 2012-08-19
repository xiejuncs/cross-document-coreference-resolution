package edu.oregonstate.example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ContainsKey {

	public static void main(String[] args) {
		String smi = "I've been . to 908 in this morning. But '' it is really matter!";
		
		List<String> cmi = Arrays.asList(smi.replaceAll("[^a-zA-Z0-9 ]", " ").split("\\s+"));
		
		System.out.println(cmi);
	}
}
