package edu.oregonstate.server;

import java.util.*;

/**
 * Submit the jobs consecutively
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class Pipeline {

	public static void main(String[] args) {
		// connect to the server
		ClusterConnection connection = new ClusterConnection();
		try {
			connection.connect();
			List<Integer> jobIds = connection.queryJobIds();
			
			for (Integer id : jobIds) {
				System.out.println(id);
			}
			
			connection.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
}
