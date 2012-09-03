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
	private boolean isGoal;

	public State() {
		state = new ArrayList<T>();
		isGoal = false;
	}
	
	public void detele(int i) {
		state.remove(i);
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
	
	public void setGoal(boolean goal) {
		isGoal = goal;
	}
	
	public boolean getGoal() {
		return isGoal;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			@SuppressWarnings("unchecked")
			State<T> s = (State<T>) obj;
			List<T> sList = s.state;
			if (sList.size() == state.size()) {
				for (int i = 0; i < sList.size(); i++) {
					if (!state.get(i).equals(sList.get(i))) {
						return false;
					}
				}
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
}
