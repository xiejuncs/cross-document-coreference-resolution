package edu.oregonstate.example;

import java.util.*;

public class ThreeSum {

	// 12 22 38
	public static void main(String[] args) {
		int[] num = {109, 72, 107, 102, 226, 43, 99, 210, 428, 329, 217, 74, 111, 428, 319};
		int target = 399;
		ArrayList<ArrayList<Integer>> solution = solution(num, target);
		System.out.println("done");
		
	}
	
	public static ArrayList<ArrayList<Integer>> solution(int[] num, int target) {
		// Start typing your Java solution below
		// DO NOT write main() function
		Arrays.sort(num);
		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> oneSol = new ArrayList<Integer>();
		combinationSum2Help(num, 0, target, result, oneSol);
		return result;
	}

	public static void combinationSum2Help(int[] num, int start, int target,
			ArrayList<ArrayList<Integer>> result, ArrayList<Integer> oneSol) {
		if (target == 0) {
			ArrayList<Integer> copy = new ArrayList<Integer>();
			copy.addAll(oneSol);
			result.add(copy);
			return;
		}
		if (target < 0 || start == num.length)
			return;
		int prev = -1;
		for (int i = start; i < num.length; i++) {
			if (num[i] != prev) {
				oneSol.add(num[i]);
				combinationSum2Help(num, i + 1, target - num[i], result, oneSol);
				oneSol.remove(oneSol.size() - 1);
				prev = num[i];
			}
		}
	}
	
}
