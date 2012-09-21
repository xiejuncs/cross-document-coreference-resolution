package edu.oregonstate.example;

import java.util.Properties;

import com.rits.cloning.Cloner;

public class StringTrim {

	public static void main(String[] args) {
		String test = "xyz";
		Cloner cloner = new Cloner();
		cloner.setDumpClonedClasses(true);
		String clTest = cloner.deepClone(test);
		System.out.println(clTest);
		cloner = null;
		System.gc();
		System.out.println(clTest);
		String clTest1 = cloner.deepClone(clTest);
		System.out.println(clTest1);
	}
}
