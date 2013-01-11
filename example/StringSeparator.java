package edu.oregonstate.example;

public class StringSeparator {

	public static void main(String[] args) {
		String test = "0.txt";
		String[] components = test.split("\\.");

		System.out.println(components[0]);
	}
}
