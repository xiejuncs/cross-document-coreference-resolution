package edu.oregonstate.search;

import java.util.*;

/**
 * Implementation of Beam Search
 */ 
public class BeamSearch {

	// a hash map is used to store nodes that have been visited 	
	private static HashMap<Integer, Integer> closedList = new HashMap<Integer, Integer>();
	private static HashMap<Integer, Integer> beam = new HashMap<Integer, Integer>();
	private static final Integer[] heuristic = {1, 0, 1, 1, 2, 1, 1, 2, 2, 1};
	
	public static Integer compare_hashMap_min(HashMap<Integer, Integer> scores) {
                Collection c = scores.values();
                Integer minvalue = (Integer) Collections.min(c);
                Integer minIndex = 0;
                Set<Integer> scores_set = scores.keySet();
                Iterator<Integer> scores_it = scores_set.iterator();
                while(scores_it.hasNext()) {
                        Integer id = scores_it.next();
                        Integer value = scores.get(id);
                        if (value == minvalue) {
                                minIndex = id;
                                break;
                        }
                }
                return minIndex;
        }
	
	public static Integer search(Graph G, Integer start, Integer goal, Integer beamWidth) {
		int g = 0;
                int v = G.V();
		int initial = v + 1; // just the symbol of null
		int fail = initial + 1;
		System.out.println("If failed, then the result will be : " + fail );	
		closedList.put(start, initial);
                beam.put(start, initial);

		// main Loop
		while (beam.size() != 0) {
			System.out.println("Beam :" + beam);
			System.out.println("ClosedList :" + closedList);
                        HashMap<Integer, Integer> set = new HashMap<Integer, Integer>();
                        for (Integer node : beam.keySet()) {
                                for (Integer neighbor : G.adj(node)) {
                                        if (neighbor == goal) {
                                               return g + 1; 
                                        }
                                        set.put(neighbor, node);
                                }
                        }
			System.out.println("Set : " + set);
                        beam = new HashMap<Integer, Integer>();
                        g = g + 1;

                        while ((set.size() != 0 ) && (beamWidth > beam.size())) {
                                HashMap<Integer, Integer> heuristicValue = new HashMap<Integer, Integer>();
                                for (Integer key : set.keySet()) {
                                        heuristicValue.put(key, heuristic[key]);
                                }
                                Integer minIndex = compare_hashMap_min(heuristicValue);
				Iterator<Integer> keys = set.keySet().iterator();
				while(keys.hasNext()) {
					Integer key = keys.next();
					if (key == minIndex) keys.remove();	
				}

                                if (!closedList.containsKey(minIndex)) {
                                        closedList.put(minIndex, set.get(minIndex));
                                        beam.put(minIndex, set.get(minIndex));
                                }
                        }
                }
		 
		return fail;
	}	

	public static void main(String[] args) {
		
		args = new String[2];
		args[0] = "tinyG.txt";
		args[1] = "2";
		if (args.length < 2) {
			System.out.println("Java Usage: java BeamSearch filename beamwidth");
			System.out.println("example: java BeamSearch tinyG.txt 2");
			System.out.println("filename : the original file, see tinyG.txt ");
			System.out.println("beamwidth : can be 1, 2, 3, 4, .... ");
			System.out.println("About the choice of beam width, shown as the link http://jhave.org/algorithms/graphs/beamsearch/beamsearch.shtml");
			System.exit(1); 
		}		
		// constructure a graph 
		In in = new In(args[0]);
		Graph G = new Graph(in);
		//StdOut.println(G); 
		int beamWidth = Integer.parseInt(args[1]);
		int start = 8;
		int goal = 1;
		System.out.println("System configuration : [ beam width :" + beamWidth + "]");
		System.out.println("======================================");
		System.out.println("Search Path");
		int result = search(G, start, goal, beamWidth);
		System.out.println("======================================");
		System.out.println(result);				
	 	System.out.println("Done");	 
	}	 
}
