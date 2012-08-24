package edu.oregonstate.example;

/**
 * EECB corpus interface for gold mentions
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Tokenization {

	public static void main(String[] args) {
		//String text = "Bettie Page, the 1950s pinup and bondage model who provided more than a half-century of inspiration for everybody from Madonna to the Suicide Girls to artist Olivia De Berardinis (as well as having her image used on all sorts of merchandise), died yesterday in Los Angeles at the age of 85.";
		String text = "BettiePage,the1950spinupandbondagemodelwhoprovidedmorethanahalf-centuryofinspirationforeverybodyfromMadonnatotheSuicideGirlstoartistOliviaDeBerardinis(aswellashavingherimageusedonallsortsofmerchandise),diedyesterdayinLosAngelesattheageof85.";
		
		// 52 61
		int astartIndex = 225;
		int aendIndex = 235;
		/*
		int startIndex = 72;
		int endIndex = 95;
		
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
			if ((offset >= startIndex) && (offset < endIndex) ) {
				if (offset == startIndex) astartIndex = i;
				if (offset == (endIndex-1)) aendIndex = i+1;
				if (offset == startIndex && (character.equals(" "))) continue;
				sb.append(character);
			}
			if (!character.equals(" ")) offset = offset + 1;
		}
		System.out.println(sb.toString());
		System.out.println(sb.toString().length());
		*/
		System.out.println(text.length());
		System.out.println(text.substring(astartIndex, aendIndex));
		System.out.println(text.substring(astartIndex, aendIndex).length());
	}
}
