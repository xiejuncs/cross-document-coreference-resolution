package edu.oregonstate.example;

import edu.stanford.nlp.dcoref.Dictionaries.Animacy;

public class EnumExample {

	public static void main(String[] args) {
		Animacy animacy = Animacy.ANIMATE;
		System.out.println(animacy.toString());
	}
}
