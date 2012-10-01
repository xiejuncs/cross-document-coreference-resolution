package edu.oregonstate.search;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * represent the state  
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class State<T> implements Serializable {
	
	private static final long serialVersionUID = 8666265337578515592L;
	// every state consists of a set of CorefClusters, in order to make it generic
	private Map<Integer, T> state;
	private int id;

	public State() {
		state = new HashMap<Integer, T>();
		id = 0;
	}
	
	public void add(Integer i, T element) {
		state.put(i, element);
	}

	public T get(int i) {
		return state.get(i);
	}

	public void remove(int i) {
		state.remove(i);
	} 

	public Map<Integer, T> getState() {
		return state;
	}
	
	public void setID(int val) {
		this.id = val;
	}
	
	public int getID() {
		return this.id;
	}
	
	// This one needs to be take cared
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			@SuppressWarnings("unchecked")
			State<T> s = (State<T>) obj;
			Map<Integer, T> sList = s.state;
			if (sList.size() == state.size()) {
				Set<T> objValues = new HashSet<T>(s.getState().values());
				Set<T> stateValues = new HashSet<T>(state.values());
				boolean equal = objValues.equals(stateValues);
				return equal;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
}
