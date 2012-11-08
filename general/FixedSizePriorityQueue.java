package edu.oregonstate.general;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * fixed size priority queue to maintain the beam
 * The priority queue is based on the max-heap
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class FixedSizePriorityQueue <E> implements Iterator<E>, Serializable, Cloneable {
	private static final long serialVersionUID = 1L;
	int size;
	int capacity;
	List<E> elements;
	double[] priorities;
	
	public List<E> getElements() {
		return elements;
	}
	
	public double[] getPriorities() {
		return priorities;
	}

	protected int parent(int loc) {
	    return (loc - 1) / 2;
	}

	protected int leftChild(int loc) {
	    return 2 * loc + 1;
	}

	protected int rightChild(int loc) {
	    return 2 * loc + 2;
	}

	protected void heapifyUp(int loc) {
	    if (loc == 0) return;
	    int parent = parent(loc);
	    if (priorities[loc] > priorities[parent]) {
	        swap(loc, parent);
	        heapifyUp(parent);
	    }
	}

	protected void heapifyDown(int loc) {
	    int max = loc;
	    int leftChild = leftChild(loc);
	    if (leftChild < size()) {
	      double priority = priorities[loc];
	      double leftChildPriority = priorities[leftChild];
	      if (leftChildPriority > priority)
	          max = leftChild;
	      int rightChild = rightChild(loc);
	      if (rightChild < size()) {
	          double rightChildPriority = priorities[rightChild(loc)];
	          if (rightChildPriority > priority && rightChildPriority > leftChildPriority)
	              max = rightChild;
	          }
	    }
	    if (max == loc)
	        return;
	    swap(loc, max);
	    heapifyDown(max);
	}

	protected void swap(int loc1, int loc2) {
	    double tempPriority = priorities[loc1];
	    E tempElement = elements.get(loc1);
	    priorities[loc1] = priorities[loc2];
	    elements.set(loc1, elements.get(loc2));
	    priorities[loc2] = tempPriority;
	    elements.set(loc2, tempElement);
	}

	protected void removeFirst() {
	    if (size < 1) return;
	    swap(0, size - 1);
	    size--;
	    elements.remove(size);
	    priorities[size] = 0.0;
	    heapifyDown(0);
	}

	  /**
	   * Returns true if the priority queue is non-empty
	   */
	public boolean hasNext() {
	    return ! isEmpty();
	}

	/**
	   * Returns the element in the queue with highest priority, and pops it from
	   * the queue.
	*/
	public E next() {
	    E first = peek();
	    removeFirst();
	    return first;
	}

	  /**
	   * Not supported -- next() already removes the head of the queue.
	   */
	public void remove() {
	    throw new UnsupportedOperationException();
	}

	  /**
	   * Returns the highest-priority element in the queue, but does not pop it.
	   */
	public E peek() {
	    if (size() > 0)
	        return elements.get(0);
	    throw new NoSuchElementException();
	}

	  /**
	   * Gets the priority of the highest-priority element of the queue.
	   */
	public double getPriority() {
	    if (size() > 0)
	      return priorities[0];
	    throw new NoSuchElementException();
	}

	  /**
	   * Number of elements in the queue.
	   */
	public int size() {
	    return size;
	}

	  /**
	   * True if the queue is empty (size == 0).
	   */
	public boolean isEmpty() {
	    return size == 0;
	}

	  /**
	   * Adds a key to the queue with the given priority.  If the key is already in
	   * the queue, it will be added an additional time, NOT promoted/demoted.
	   *
	   * @param key
	   * @param priority
	   */
	public boolean add(E key, double priority) {
		if (size == capacity) {
			FixedSizePriorityQueue<E> pq = clone();
			elements = new ArrayList<E>(capacity);
			priorities = new double[capacity];
			size = 0;
			while (!pq.isEmpty()) {
				double prio = pq.getPriority();
				E element = pq.next();
				if (pq.size() < 1) {
					
					if (prio < priority) {
						prio = priority;
						element = key;
					}
				}

				elements.add(element);
			    priorities[size] = prio;
			    heapifyUp(size);
			    size++;
			    
			}

	    } else {

	    	elements.add(key);
	    	priorities[size] = priority;
	    	heapifyUp(size);
	    	size++;
	    }
	    return true;
	}

	  /**
	   * Returns a representation of the queue in decreasing priority order.
	   */
	public String toString() {
	    return toString(size());
	}
	
	public FixedSizePriorityQueue<E> clone() {
		FixedSizePriorityQueue<E> clonePQ = new FixedSizePriorityQueue<E>(capacity);
	    clonePQ.size = size;
	    clonePQ.capacity = capacity;
	    clonePQ.elements = new ArrayList<E>(capacity);
	    clonePQ.priorities = new double[capacity];
	    if (size() > 0) {
	        clonePQ.elements.addAll(elements);
	        System.arraycopy(priorities, 0, clonePQ.priorities, 0, size());
	    }
	    return clonePQ;
	}

	  /**
	   * Returns a representation of the queue in decreasing priority order,
	   * displaying at most maxKeysToPring elements.
	   *
	   * @param maxKeysToPrint
	   */
	public String toString(int maxKeysToPrint) {
		FixedSizePriorityQueue<E> pq = clone();
	    StringBuilder sb = new StringBuilder("[");
	    int numKeysPrinted = 0;
	    while (numKeysPrinted < maxKeysToPrint && pq.hasNext()) {
	      double priority = pq.getPriority();
	      E element = pq.next();
	      sb.append(element.toString());
	      sb.append(" : ");
	      sb.append(priority);
	      if (numKeysPrinted < size() - 1)
	        sb.append(", ");
	      numKeysPrinted++;
	    }
	    if (numKeysPrinted < size())
	      sb.append("...");
	    sb.append("]");
	    return sb.toString();
	}

    // constructor
	public FixedSizePriorityQueue(int capacity) {
		 elements = new ArrayList<E>(capacity);
		 priorities = new double[capacity];
		 this.capacity = capacity;
	}
	
    public static void main(String[] args) {
	    FixedSizePriorityQueue<String> pq = new FixedSizePriorityQueue<String>(2);
	    System.out.println(pq);
	    pq.add("one",1);
	    System.out.println(pq);
	    pq.add("three",2);
	    System.out.println(pq);
	    pq.add("four",3);
	    System.out.println(pq);
	    pq.add("two",4);
	    System.out.println(pq);
	}
}
