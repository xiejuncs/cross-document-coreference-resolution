package edu.oregonstate.search;

import java.util.List;
import java.util.ArrayList;

/**
 * represent the state  
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class State<T> {
	
	// every state consists of a set of CorefClusters, in order to make it generic   
	private List<T> state;
	public boolean isGoal = false;

	public State() {
		state = new ArrayList<T>();
	}

	public void add(T element) {
		state.add(element);
	}

	public void addAll(List<T> elements) {
		state.addAll(elements);
	}

	public T get(int i) {
		return state.get(i);
	}	 	 

	public void remove(int i) {
		state.remove(i);
	} 

	public List<T> getState() {
		return state;
	}
	
}
