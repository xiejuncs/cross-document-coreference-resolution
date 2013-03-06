package edu.oregonstate.server;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JobState {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
			"MM/dd/yyyy HH:mm:ss");

	private int jobId;

	private double prior;

	private String displayName;

	private String fullName;

	private String userName;

	private String state;

	private Date startTime;

	private String queue;

	private Integer slotsJaTaskId;

	public JobState(int jobId, double prior, String displayName,
			String userName, String state, Date startTime, String queue,
			Integer slotsJaTaskId) {
		this.jobId = jobId;
		this.prior = prior;
		this.displayName = displayName;
		this.userName = userName;
		this.state = state;
		this.startTime = startTime;
		this.queue = queue;
		this.slotsJaTaskId = slotsJaTaskId;
	}

	public static List<Integer> parseJobIds(String info) throws Exception {
		List<Integer> jobIds = new ArrayList<Integer>();
		if (info.isEmpty()) {
			return jobIds;
		}
		String[] lines = info.trim().split("\\n");
		for (int i = 0; i < lines.length; ++i) {
			String line = lines[i].trim();
			if (i == 0) {
				if (!"job-ID  prior   name       user         state submit/start at     queue                          jclass                         slots ja-task-ID"
						.equals(line)) {
					throw new Exception("Unexpected header: " + line);
				}
			} else if (i >= 2) {
				// int jobId = Integer.valueOf(line.substring(0, 7).trim());
				int jobId = Integer.valueOf(line.split("\\s+")[0]);
				jobIds.add(jobId);
			}
		}
		return jobIds;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("===== Job State =====");
		buffer.append("jobId: " + jobId + "\n");
		buffer.append("prior: " + prior + "\n");
		buffer.append("displayName: " + displayName + "\n");
		buffer.append("userName: " + userName + "\n");
		buffer.append("state: " + state + "\n");
		buffer.append("startTime: " + DATE_FORMAT.format(startTime) + "\n");
		buffer.append("queue: " + queue + "\n");
		buffer.append("slotsJaTaskId: " + slotsJaTaskId + "\n");
		return buffer.toString();
	}

	public int getJobId() {
		return jobId;
	}

	public double getPrior() {
		return prior;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getFullName() {
		return fullName;
	}

	public String getUserName() {
		return userName;
	}

	public String getState() {
		return state;
	}

	public Date getStartTime() {
		return startTime;
	}

	public String getQueue() {
		return queue;
	}

	public Integer getSlotsJaTaskId() {
		return slotsJaTaskId;
	}
}
