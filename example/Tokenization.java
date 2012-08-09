package edu.oregonstate.example;

/**
 * EECB corpus interface for gold mentions
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Tokenization {

	public static void main(String[] args) {
		String text = "Word comes from People magazine and other celebrity news outlets that Tara Reid, 33, who starred in ``American Pie'' and appeared on U.S. TV show ``Scrubs,'' has entered the Promises Treatment Center in Malibu, California - the same facility that in the past has been the rehab facility of choice for many a Hollywood star.";
		
		int startIndex = 223;
		int endIndex = 268;
		int offset = 0;
		char[] chars = text.toCharArray();
		int textLength = chars.length;
		String[] characters = new String[chars.length];
		for (int i = 0; i < textLength; i++) {
			char character = chars[i];
			characters[i] = Character.toString(character);
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < textLength; i++) {
			String character = characters[i];
			if (!character.equals(" ")) offset = offset + 1;
			if ((offset > startIndex) && (offset <= endIndex) ) {
				if (offset == endIndex && (character.equals(" "))) continue;
				sb.append(character);
			}
			
			
		}
		System.out.println(sb.toString());
		System.out.println(sb.toString().length());
	}
}
