package edu.oregonstate.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class ClusterConnection {

	// print log information
	private static final Logger log = Logger.getLogger(ClusterConnection.class);

	// host name
	private String host;

	private String user;

	private String password;

	private String stdout;

	private String stderr;

	private Session session;

	private int exitStatus;

	// Millisecond: 0.001
	private static final long COMMAND_TIME_INTERVAL = 1000;

	public ClusterConnection() {
		this("submit-em64t-01.hpc.engr.oregonstate.edu", "xie", "88jx$85");
	}

	public ClusterConnection(String host, String user, String password) {
		this.host = host;
		this.user = user;
		this.password = password;
	}

	public void connect() throws Exception {
		disconnect();

		JSch jsch = new JSch();
		session = jsch.getSession(user, host, 22);
		String homeDir = System.getProperty("user.home");
		String knownHostPath = homeDir + File.separator + ".ssh"
				+ File.separator + "known_hosts";
		jsch.setKnownHosts(knownHostPath);
		// If two machines have SSH passwordless logins setup, the following
		// line is not needed:
		session.setPassword(password);
		session.connect();
	}

	public void disconnect() {
		if (session != null) {
			session.disconnect();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		disconnect();
	}

	public void execCommand(String cmd) throws Exception {
		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		channel.setCommand(cmd);
		channel.setInputStream(null);
		channel.setErrStream(null);
		InputStream in = channel.getInputStream();
		InputStream err = channel.getErrStream();
		stdout = "";
		stderr = "";
		channel.connect();
		while (true) {
			stdout += getRespond(in);
			stderr += getRespond(err);
			if (channel.isClosed()) {
				exitStatus = channel.getExitStatus();
				break;
			}
		}
		channel.disconnect();
		log.info("==========================================");
		log.info("Command '" + cmd + "' executed");
		log.info("stdout:\n" + (stdout.isEmpty() ? "[EMPTY]" : stdout));
		log.info("stderr:\n" + (stderr.isEmpty() ? "[EMPTY]" : stderr));
		log.info("exit-status: " + exitStatus);
		Thread.sleep(COMMAND_TIME_INTERVAL);
	}

	public String getRespond(InputStream is) throws IOException {
		StringBuffer buffer = new StringBuffer();
		byte[] tmp = new byte[1024];
		while (is.available() > 0) {
			int i = is.read(tmp, 0, 1024);
			if (i < 0)
				break;
			buffer.append(new String(tmp, 0, i));
		}
		return buffer.toString().trim();
	}

	public String getStdout() {
		return stdout;
	}

	public String getStderr() {
		return stderr;
	}

	public List<Integer> queryJobIds() throws Exception {
		execCommand("qstat -u xie");
		return JobState.parseJobIds(stdout);
	}

	public int submitJob(String scriptPath) throws Exception {
		execCommand("qsub " + scriptPath);
		System.out.println("qsub " + scriptPath);
		String stdout = getStdout().trim();
		if (!stdout.startsWith("Your job")
				|| !stdout.endsWith("has been submitted")) {
			System.out.println(stdout);
			throw new Exception("Job cannot be submitted! script:" + scriptPath
					+ "\nstdout:" + stdout + "\nstderr:" + stderr);
		}
		stdout = stdout.replaceAll("Your job", "").trim();
		int jobId = Integer.valueOf(stdout.split("\\s+")[0]);
		log.info("jobId is " + jobId);
		return jobId;
	}

	public void deleteJob(int jobId) throws Exception {
		execCommand("qdel " + jobId);
	}
	
}
