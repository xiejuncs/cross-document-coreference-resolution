package edu.oregonstate.example;

import java.util.*;

public class Weight {

	public static void main(String[] args) {
		
		List<Integer> previousIDs = new ArrayList<Integer>();
		previousIDs.add(1);
		previousIDs.add(2);
		previousIDs.add(3);
		
		List<Integer> currentIDs = new ArrayList<Integer>();
		currentIDs.add(1);
		currentIDs.add(3);
		currentIDs.add(4);
		
		currentIDs.removeAll(previousIDs);
		System.out.println(currentIDs);
	}
}
