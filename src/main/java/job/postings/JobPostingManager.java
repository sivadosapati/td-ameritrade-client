package job.postings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise.trading.options.CSVParser;
import com.rise.trading.options.Util;

public class JobPostingManager {

	static String SESSION_ID_FILE = "session_id.txt";

	public static void main(String[] x) throws Exception {
		try {
			manageJobs();
			//findRecruitersRejectingCandidates();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void findRecruitersRejectingCandidates() throws Exception {
		String a = "https://vendor.ilabor360.com/fetchCandidateSubmissionListView?searchTerm=&requisitionId=0&startingDate=01-04-2024&endingDate=07-02-2024&page=1&pageSize=500&skip=0&take=500";
		String url = "https://vendor.ilabor360.com/getSubmissionComment.action?candidateSubmissionId=";

		String sessionCookie = getSessionCookie();
		String content = iLaborConnector.sendGetRequest(a, sessionCookie);
		ObjectMapper mapper = new ObjectMapper();
		CandidateSubmissionList list = mapper.readValue(content, CandidateSubmissionList.class);
		for (CandidateEntry ce : list.candidateSubmissionListView) {
			String id = ce.candidateSubmissionId;
			String x = url + id;
			String con = iLaborConnector.sendGetRequest(x, sessionCookie);
			CandidateSubmissionComment csc = mapper.readValue(con, CandidateSubmissionComment.class);
			CandidateSubmissionEntry comment = getFirstComment(csc);
			if (comment != null) {
				System.out.println(ce.candidateSubmissionId+","+ce.requisitionId+","+comment.commentBy.contact.fullName+","+comment.commentBy.userName+",\""+ce.lastComment+"\","+ce.lastCommentDate);
			}
			sleepForRandomSeconds(15);
			
		}
		System.out.println("Done");

	}

	private static CandidateSubmissionEntry getFirstComment(CandidateSubmissionComment csc) {
		List<CandidateSubmissionEntry> x = csc.listSubmissionComment;
		if (x.size() == 0) {
			return null;
		}
		return x.get(0);
	}

	public static String getSessionCookie() throws Exception {
		File f = new File(SESSION_ID_FILE);
		String sessionCookie = null;
		if (f.exists()) {
			long last = f.lastModified();
			long current = System.currentTimeMillis();
			long timeElapsed = (current - last) / 1000;
			System.out.println(timeElapsed);
			if (timeElapsed > 30 * 60) {
				System.out.println("Login again after 30 minutes");
				sessionCookie = iLaborConnector.login();
				Util.writeContent(SESSION_ID_FILE, sessionCookie);
			} else {
				System.out.println("Return existing session cookie");
				sessionCookie = Util.readContent(SESSION_ID_FILE);
			}
		} else {
			System.out.println("Login for first time");
			sessionCookie = iLaborConnector.login();
			Util.writeContent(SESSION_ID_FILE, sessionCookie);
		}

		return sessionCookie;
	}

	private static RuntimeException CODE_ME = new RuntimeException("CODE_ME");

	public static Job getJob(String jobId, String sessionCookie) throws Exception {
		String dataUrl1 = "https://vendor.ilabor360.com/fetchRequisitionRecord?id=" + jobId;
		String content = iLaborConnector.sendGetRequest(dataUrl1, sessionCookie);
		ObjectMapper mapper = new ObjectMapper();
		Job job = mapper.readValue(content, Job.class);
		return job;
	}

	public static JobPostings makeJobListingsFromGoogleSpreadsheet() throws Exception {
		// throw CODE_ME;
		String latestFile = createUpldateJobListingsFromSpreadsheet();
		JobPostings old = createJobListingsFromSpreadSheet(latestFile);
		return old;
	}

	public static List<String> getJobsWhoHaveJDInGoogleDrive() {
		File dir = new File(JOB_DIRECTORY);
		List<String> data = new ArrayList<String>();
		File[] files = dir.listFiles();
		for (File f : files) {
			if (hasJobDescription(f)) {
				data.add(findJobId(f.getName()));
			}
		}
		return data;
	}

	private static String findJobId(String folderName) {
		String x = folderName.substring(0, 6);
		return x;
	}

	private static boolean hasJobDescription(File dir) {
		File[] children = dir.listFiles();
		if (children == null)
			return false;
		for (File f : children) {
			if (f.getName().equals("jd.html")) {
				return true;
			}
		}
		return false;
	}

	public static File writeJobPostingToGoogleDrive(Job job) throws Exception {
		File f = findJobFolderIfPresent(job);
		if (f == null)
			return null;
		f.mkdirs();
		File jd = new File(f, "jd.html");
		Util.writeContent(jd.getAbsolutePath(), job.requisitionRecObj.jobDescription);
		return jd.getParentFile();
	}

	private static File findJobFolderIfPresent(Job job) {
		File dir = new File(JOB_DIRECTORY);
		String jobId = job.findJobId();
		if (jobId == null) {
			System.out.println("Null -> " + Util.toJSON(job));
			return null;
		}
		for (File x : dir.listFiles()) {
			if (x.isDirectory()) {
				if (x.getName().startsWith(jobId)) {
					return x;
				}
			}
		}
		return new File(JOB_DIRECTORY,
				job.requisitionRecObj.requisitionId + " - " + job.requisitionRecObj.jobTitle.jobTitleCode);
	}

	static String JOB_DIRECTORY = Util.getDirectoryForJobs();

	private static JobPostings getLatestJobPostings(String sessionCookie) throws Exception {

		String dataUrl1 = "https://vendor.ilabor360.com/showRequisitionViewList?searchTerm=&page=1&pageSize=500&skip=0&take=500";
		String content = iLaborConnector.sendGetRequest(dataUrl1, sessionCookie);

		return getJobPostings(content);
	}

	public static void manageJobs() throws Exception {
		String sessionCookie = getSessionCookie();
		JobPostings latest = getLatestJobPostings(sessionCookie);
		// System.out.println("Some Job from source ->
		// "+latest.getJobPosting("130989"));
		JobPostings old = makeJobListingsFromGoogleSpreadsheet();
		mergeJobPostings(old, latest);
		writeJobPostings(old);
		// System.out.println("Some Job -> "+old.getJobPosting("130989"));
		List<String> jobsHavingJDs = getJobsWhoHaveJDInGoogleDrive();
		// System.out.println(jobsHavingJDs);
		List<String> jobsThatNeedJDs = findJobsThatNeedTobCrawled(jobsHavingJDs, old);
		for (String x : jobsThatNeedJDs) {
			Job job = getJob(x, sessionCookie);
			File f = writeJobPostingToGoogleDrive(job);
			if (f == null)
				continue;
			System.out.println("Wrote JD for -> " + f.getAbsolutePath());
			int xx = sleepForRandomSeconds(60);
			// return;
		}
		System.out.println("Done");

	}

	private static int sleepForRandomSeconds(int x) throws InterruptedException {
		int xx = getRandom(x);
		//System.out.println("Sleeping for -> " + xx);
		Thread.sleep(xx * 1000);
		return xx;
	}

	private static int getRandom(int x) {
		return (int) (Math.random() * x);
	}

	private static List<String> findJobsThatNeedTobCrawled(List<String> jobsHavingJDs, JobPostings old) {
		List<String> jobsToBeCrawled = new ArrayList<String>();
		for (JobPosting posting : old.requisitionViewList) {
			if (jobsHavingJDs.contains(posting.requisitionId)) {
				continue;

			}
			jobsToBeCrawled.add(posting.requisitionId);
		}
		return jobsToBeCrawled;
	}

	private static void mergeJobPostings(JobPostings oldJobPostings, JobPostings latestJobPostings) {
		System.out.println(oldJobPostings.size() + " -> " + latestJobPostings.size());
		for (JobPosting jp : oldJobPostings.requisitionViewList) {
			// System.out.println(jp.requisitionId);
			JobPosting x = latestJobPostings.getJobPosting(jp.requisitionId);
			if (x == null) {
				jp.setJobIsInactive();
				continue;
			}
			jp.copyDataFromSource(x);
			latestJobPostings.removeJobPosting(x);
		}
		System.out.println(oldJobPostings.size() + " -> " + latestJobPostings.size());
		for (JobPosting jp : latestJobPostings.requisitionViewList) {
			if (jp.isJobActive()) {
				oldJobPostings.addJobPosting(jp);
			}
		}
		System.out.println(oldJobPostings.size() + " -> " + latestJobPostings.size());

	}

	private static void writeJobPostings(JobPostings jobPostings) throws IOException {
		String output = jobPostings.findHeader() + "\n";
		for (JobPosting jp : jobPostings.requisitionViewList) {

			// System.out.println(Util.toJSON(jp));
			output = output + jp.getShareableEntryForSourcers() + "\n";
		}
		PrintWriter writer = new PrintWriter(new FileWriter("jobs.csv"), true);
		writer.print(output);
		writer.close();
		System.out.println("Jobs.csv is written with -> " + jobPostings.size() + " records");
	}

	private static JobPostings getJobPostings(String content) throws JsonProcessingException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		JobPostings jobPostings = mapper.readValue(content, JobPostings.class);
		return jobPostings;
	}

	private static JobPostings createJobListingsFromSpreadSheet(String file) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		List<JobPosting> jobPostings = new ArrayList<JobPosting>();
		JobPostings jp = new JobPostings();
		String line = reader.readLine();
		CSVParser parser = new CSVParser();
		List<String> keys = parser.parseCSV(line);
		jp.header = keys;
		while (true) {
			JobPosting x = new JobPosting();
			line = reader.readLine();
			if (line == null)
				break;
			List<String> values = parser.parseCSV(line);
			// Map<String, String> content = makeMap(keys, values);
			x.setValuesFromSpreadSheet(keys, values);
			jobPostings.add(x);
		}
		jp.requisitionViewList = jobPostings;
		return jp;

	}

	private static String createUpldateJobListingsFromSpreadsheet() {

		File dir = new File(System.getProperty("user.home") + "/Downloads/");
		File ff[] = dir.listFiles();
		File file = null;
		for (File f : ff) {
			String name = f.getName();
			if (name.startsWith("Job Requirements - ")) {

				if (file == null) {
					file = f;
					continue;
				}
				long t = f.lastModified();
				if (t > file.lastModified()) {
					file = f;
				}
			}
		}
		return file.getAbsolutePath();
	}

}
