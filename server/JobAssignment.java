package edu.oregonstate.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Submit job and transfer file based on JSCH
 * 
 * @author Jun Xie (xie@eecs.oregonstate.edu)
 *
 */
public class JobAssignment {

	
	
	
	public static void main(String[] args) {
		String path = "/nfs/guille/xfern/users/xie/Experiment/experiment/";

		//get current date time with Date()
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String folderName = dateFormat.format(date);
	}
}
