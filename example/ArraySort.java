package edu.oregonstate.example;

import java.util.Arrays;

public class ArraySort {

	public static void main(String[] args) {
		Integer[] sentences = {0, 2};
		Arrays.sort(sentences);
		for (Integer sentence : sentences) 
			System.out.println(sentence);
	}
}
