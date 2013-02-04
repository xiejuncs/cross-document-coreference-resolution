package edu.oregonstate.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.oregonstate.general.DoubleOperation;

public class VectorNormalization {

	public static void main(String[] args) {
		int size = 10;
		List<Integer> arrays = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			arrays.add(i);
		}
		
		Collections.shuffle(arrays);
		
		System.out.println("done");
	}
}
