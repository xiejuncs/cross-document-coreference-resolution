package edu.oregonstate.example;

public class StringTrim {

	public static void main(String[] args) {
		String text = "Thenewscomesasashockforeveryonewhojustassumedthe\\/seldom-seenactresshadbeenholedupthereforawhile-atleastwhenshewasn'ttouringU.S.frathousesand\\/ortequilaconventions.";
		
		System.out.println(text.replace("\\", ""));
	}
}
