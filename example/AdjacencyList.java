package edu.oregonstate.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Graph represented by an adjacency list.
 * 
 * Reference: Introduction to Algorithms - CLRS.
 * 
 * @author ar
 * @since: 26/02/2011
 */

public class AdjacencyList
{
    
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
    
    Map<Node, List<Node>> nodes;

    public AdjacencyList()
    {
        nodes = new HashMap<Node, List<Node>>();
    }
    
    public void addEdge(Node n1, Node n2)
    {
        if (nodes.containsKey(n1)) {
            nodes.get(n1).add(n2);
        } else {
            ArrayList<Node> list = new ArrayList<Node>();
            list.add(n2);
            nodes.put(n1, list);
        }
    }
    
    public List<Integer> bfs(Node s)
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
    
    public static void main(String[] args)
    {
        AdjacencyList graph = new AdjacencyList();
        Node n1 = new Node(0);
        Node n2 = new Node(1);
        Node n3 = new Node(2);
        Node n4 = new Node(3);
        Node n5 = new Node(4);
        Node n6 = new Node(5);
        Node n7 = new Node(6);
        Node n8 = new Node(7);
        Node n9 = new Node(8);
        Node n10 = new Node(9);
        Node n11 = new Node(10);
        Node n12 = new Node(11);
        Node n13 = new Node(12);
        Node n14 = new Node(13);
        Node n15 = new Node(14);
        
        graph.addEdge(n2, n1);
        graph.addEdge(n2, n8);
        graph.addEdge(n2, n15);
        
        graph.addEdge(n8, n7);
        graph.addEdge(n8, n12);
        
        graph.addEdge(n7, n3);
        graph.addEdge(n7, n5);
        graph.addEdge(n7, n6);
        
        graph.addEdge(n12, n10);
        graph.addEdge(n12, n13);
        
        graph.addEdge(n3, n4);
        
        graph.addEdge(n10, n11);
        graph.addEdge(n10, n9);
        graph.addEdge(n13, n14);
        
        graph.bfs(n1);
        System.out.println("done");

    }
}
