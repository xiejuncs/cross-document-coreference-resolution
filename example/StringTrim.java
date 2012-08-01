package edu.oregonstate.example;

public class StringTrim {

	public static void main(String[] args) {
		String text = "3*";
		if (text.endsWith("*")) {
			text = text.substring(0, text.length()-1);
			text += "000";
        }
		System.out.println(text);
		System.out.println("Done");
	}
}
