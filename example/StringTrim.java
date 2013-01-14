package edu.oregonstate.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringTrim {

	public static void main(String[] args) {
		List<Integer> ids = new ArrayList<Integer>();
		
		ids.add(1);
		ids.add(8);
		ids.add(2);
		ids.add(6);
		
		Collections.sort(ids, Collections.reverseOrder());
		
		System.out.println("done");
	}
	

}
