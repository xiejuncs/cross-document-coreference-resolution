package edu.oregonstate.example;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.HashMap;

/**
 * Interface for Semantic parsing: PropBank--NomBank frames output.
 * The pattern of the output is listed in the CoNLL website : 
 * http://barcelona.research.yahoo.net/dokuwiki/doku.php?id=conll2008:format
 * <p>
 * In addition, there is an test file at http://fileadmin.cs.lth.se/nlp/software/lth_srl/test.txt
 * <b>Example</b>
 * Run the second sentence :
 * With the passing of the years, the Sixties working-class wonder boy has metamorphosed into a very cross pensioner.
 * We can get the output.
 * In short: the predicates recognized by the tool are listed in column 11. In this case, we have 3 predicates:
 *  "passing", "boy", and "metamorphose". For each of these 3 predicates, there is an additional column listing 
 *  the arguments of that predicate. Thus the first column after the predicate column lists the arguments for 
 *  "passing"; in this case we have only one argument, the A1. Note that format lists only the heads of the arguments
 *  , in this case "of". If you want the full argument span "of the years", you'll need to generate the yield of the 
 *  syntactic dependency tree (column 9).
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class SemanticRoleLabelingOutput {
	private static List<List<String>> datas = new ArrayList<List<String>>();
	private static String outputPath = "corpus/test.output";
	private static List<Integer> headsPosition = new ArrayList<Integer>();
	private static Map<Node, List<Node>> nodes = new HashMap<Node, List<Node>>();
	private static List<Node> nodes1 = new ArrayList<Node>();
	
	public static void main(String[] args) {
		datas = read(outputPath);
		for (List<String> data : datas) {
			System.out.println(data);
		}
		
		List<String> tokens = new ArrayList<String>();
		for (List<String> data : datas) {
			tokens.add(data.get(1));
		}
		System.out.println(tokens);
		
		List<String> preds = new ArrayList<String>();
		for (List<String> data : datas) {
			preds.add(data.get(10));
		}
		System.out.println(preds);
		
		for (int i = 0; i < datas.size(); i++) {
			List<String> data = datas.get(i);
			String position = data.get(8);
			if (position.equals("0")) {
				headsPosition.add(i);
			} else {
				Integer pos = Integer.parseInt(data.get(8));
				headsPosition.add(pos -1);
			}
		}
		
		System.out.println(headsPosition);
		
		List<Integer> predicates = new ArrayList<Integer>();
		for (int i = 0; i < preds.size(); i++) {
			String pred = preds.get(i);
			if (pred.equals("_"))	continue;
			predicates.add(i);
		}
		System.out.println(predicates);
		
		int predSize = predicates.size();
		
		Map<Integer, Map<String, Integer>> arguments = new HashMap<Integer, Map<String,Integer>>();
		for (int i = 11; i <= (10 + predSize); i++) {
			int slot = i - 11;
			int index = predicates.get(slot);
			Map<String, Integer> argument = new HashMap<String, Integer>();
			for (int j = 0; j < datas.size(); j++) {
				List<String> data = datas.get(j);
				if (data.get(i).equals("A0") || data.get(i).equals("A1")) {
					argument.put(data.get(i), j);
				}
			}
			arguments.put(index, argument);
		}
		System.out.println(arguments);
		
		for (int i = 0; i < headsPosition.size(); i++) {
			nodes1.add(new Node(i));
		}
		
		for (int i = 0; i < headsPosition.size(); i++) {
			int parent = headsPosition.get(i);
			addEdge(nodes1.get(parent), nodes1.get(i));
		}
		
		Map<String, Map<String, String>> semanticRoles = new HashMap<String, Map<String,String>>();
		for (Integer index : arguments.keySet()) {
			String word = tokens.get(index);
			Map<String, Integer> roles = arguments.get(index);
			Map<String, String> semRoles = new HashMap<String, String>();
			for (String role : roles.keySet()) {
				int headPosition = roles.get(role);
				String span = find(headPosition, tokens);
				// find its yield according to headsPosition
				semRoles.put(role, span);
			}
			
			semanticRoles.put(word, semRoles);
		}
		
		System.out.println(semanticRoles);
	}
	
	public static class Node
    {
        int data;
        Node predecessor;
        
        public Node(int data)
        {
            this.data = data;
        }
        
        public String toString()
        {
            return "(" + data + ")";
        }
    }
    
    public static List<Integer> bfs(Node s)
    {    	
    	List<Integer> children = new ArrayList<Integer>();
        Set<Node> keys = nodes.keySet();
        for (Node u : keys) {
            if (u != s) {
                u.predecessor = null;
            }
        }
        s.predecessor = null;
        Queue<Node> q = new ArrayDeque<Node>();
        q.add(s);
        while (!q.isEmpty()) {
            Node u = q.remove();
            List<Node> adj_u = nodes.get(u);
            if (adj_u != null) {
                for (Node v : adj_u) {
                    v.predecessor = u;
                    q.add(v);
                }
            }
            children.add(u.data);
        }
        return children;
    }
    
    public static void addEdge(Node n1, Node n2)
    {
        if (nodes.containsKey(n1)) {
            nodes.get(n1).add(n2);
        } else {
            ArrayList<Node> list = new ArrayList<Node>();
            list.add(n2);
            nodes.put(n1, list);
        }
    }
	
	public static String find(int headPosition, List<String> tokens) {
		String span = "";
		int startIndex = 0;
		int endIndex = 0;
		List<Integer> spans = new ArrayList<Integer>();
		spans = bfs(nodes1.get(headPosition));
		startIndex = Collections.min(spans);
		endIndex = Collections.max(spans);
		assert endIndex > startIndex;
		StringBuilder sb = new StringBuilder();
		for (int i = startIndex; i <= endIndex; i++) {
			sb.append(tokens.get(i) + " ");
		}
		span = sb.toString().trim();
		return span;
	}	
	
	private static List<List<String>> read(String path) {
        List<List<String>> datums = new ArrayList<List<String>>();
        try {
                BufferedReader reader = new BufferedReader(new FileReader(path));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        String[] records = line.split("\t");
                        records[0] = Integer.toString(Integer.parseInt(records[0]) - 1);
                        List<String> data = new ArrayList<String>();
                        for (String record : records) {
                                data.add(record);
                        }
                        datums.add(data);
                }
                reader.close();
        } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
        }
        return datums;
	}
}
