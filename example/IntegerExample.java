package edu.oregonstate.example;

public class IntegerExample {

	public static void main(String[] args) {
		Integer a = 101025008;
		
		int topic = a/100000000;
		int doc = a/1000000 - topic * 100; 
		int start = a/1000 - topic * 100000 - doc * 1000;
		int end = a - topic * 100000000 - doc * 1000000 - start * 1000;
		System.out.println(a);
		System.out.println(topic);
		System.out.println(doc);
		System.out.println(start);
		System.out.println(end);
		Integer id = topic * 100000000 + doc * 1000000 + start * 1000 + end;
		System.out.println(id);
	}
}
