package edu.oregonstate.experiment;

import java.lang.reflect.Field;
import java.util.*;

public class Node {

	public List<String> configuration;
	
	public Node() {
		configuration = new ArrayList<String>();
	}
	
	public Node(List<String> configuration) {
		this.configuration = configuration;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		for (String element : configuration) {
			sb.append(element + "\n");
		}
		
		return sb.toString().trim();
	}
	
	public Node cat(String element) {		
		List<String> newconfiguration = new ArrayList<String>();
		for (String elment : configuration) {
			newconfiguration.add(elment);
		}
		newconfiguration.add(element);
		
		return new Node(newconfiguration);
	}
	
	public static void main(String[] args) {
		// get its corresponding property
		ExperimentProperties properties = new ExperimentProperties();
		Class propertyClass = properties.getClass();
		Field[] propertyFields = propertyClass.getFields();
		Map<String, String> propertyMap = new HashMap<String, String>();
		for (Field field : propertyFields) {
			try {
				propertyMap.put(field.getName(), field.get(properties).toString());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		// get its specific arguments
		ExperimentArguments arguments = new ExperimentArguments();
		Class argumentClass = arguments.getClass();
		Field[] argumentFields = argumentClass.getFields();
		List<Map<String, List<String>>> argumentMap = new ArrayList<Map<String,List<String>>>();
		for (Field field : argumentFields) {
			try {
				List<String> argument = Arrays.asList(((String[]) field.get(arguments)));
				Map<String, List<String>> specificArgument = new HashMap<String, List<String>>();
				specificArgument.put(field.getName(), argument);
				argumentMap.add(specificArgument);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		int length = argumentMap.size();
		List<Node> combinations = new ArrayList<Node>();
		Queue<Node> queue = new LinkedList<Node>();
		Node initialNode = new Node();
		queue.offer(initialNode);
		
		int index = 0;
		while (queue.size() > 0) {
			Node node = queue.poll();
			
			if (index == length) {
				break;
			}
			
			Map<String, List<String>> array = argumentMap.get(index);
			for (String key : array.keySet()) {
				String configKey = propertyMap.get(key);
				
				List<String> elements = array.get(key);
				
				for (String element : elements) {
					Node child = node.cat(configKey + " = " + element);
					queue.offer(child);
					if (child.configuration.size() == length) {
						combinations.add(child);
					}
				}
			}
			
			if (allSameLength(queue)) {
				index += 1;
			}
			
		}
		
		System.out.println("done");
	}
	
	private static boolean allSameLength(Queue<Node> queue) {
		Set<Integer> lengths = new HashSet<Integer>();
		Iterator<Node> iterator = queue.iterator();
		while(iterator.hasNext()) {
			Node node = iterator.next();
			lengths.add(node.configuration.size());
		}
		
		return lengths.size() == 1 ? true : false;
	}
}
