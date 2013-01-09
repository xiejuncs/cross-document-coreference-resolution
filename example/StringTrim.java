package edu.oregonstate.example;

public class StringTrim {

	public static void main(String[] args) {
		int x = 2;
		for (int i = 0; i < 10; i++) {
			plus(x);
		}
		
		System.out.println(x);
	}
	
	private static void plus(int x) {
		x = x * x;
	}
	
	
	
}
