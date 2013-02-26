package edu.oregonstate.example;


import java.util.HashSet;
import java.util.Set;

public class VectorNormalization {

	public static void main(String[] args) {
		Set<String> former = new HashSet<String>();
		former.add("1");
		former.add("2");
		former.add("3");
		
		Set<String> latter = new HashSet<String>();
		latter.add("1");
		latter.add("3");
		latter.add("4");
		
		Set<String> union = new HashSet<String>();
		union.addAll(former);
		union.addAll(latter);
		System.out.println(union.size());
		
		Set<String> intersection = new HashSet<String>();
		intersection.addAll(former);
		intersection.retainAll(latter);
		System.out.println(intersection.size());

		System.out.println( union.size() - intersection.size());
		
		
		
	}
}
