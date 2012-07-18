package edu.oregonstate.example;

import java.util.HashMap;

public class ContainsKey {

	public static void main(String[] args) {
		HashMap<String, String> entities = new HashMap<String, String>();
		String key = "id";
		boolean contains = entities.containsKey(key);
		System.out.println(contains);
	}
}
