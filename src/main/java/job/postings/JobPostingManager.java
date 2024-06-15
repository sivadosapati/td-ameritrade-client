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
			// mainOld(x);
			manageJobs();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void mainOldest(String[] args) throws Exception {
		String sessionCookie = getSessionCookie();
		System.out.println(sessionCookie);
		JobPostings latest = getLatestJobPostingsFromPortal(sessionCookie);
		// JobPostings old = makeJobListingsFromGoogleSpreadsheet();
		String latestFile = getLatestJobListingsSpreadSheetFile();
		JobPostings old = createJobListingsFromSpreadSheet(latestFile);
		System.out.println(latest.size());
		System.out.println(old.size());
		mergeJobPostings(old, latest);
		System.out.println(old.size());
		// List<String> jobs

	}

	public static void mainOld(String[] args) throws Exception {

		// System.out.println(jobPostings.iTotalRecords);
		// JobPostings latestJobPostings = getLatestJobListings();
		String sessionId = getSessionCookie();
		JobPostings latestJobPostings = getLatestJobPostingsFromPortal(sessionId);

		String file = getLatestJobListingsSpreadSheetFile();
		JobPostings oldJobPostings = createJobListingsFromSpreadSheet(file);
		mergeJobPostings(oldJobPostings, latestJobPostings);
		writeJobPostings(oldJobPostings);
		// writeJobPostings(oldJobPostings);
		// writeJobPostings(jobPostings);
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

	public static JobPostings getLatestJobPostingsFromPortal(String sessionCookie) throws Exception {
		String dataUrl1 = "https://vendor.ilabor360.com/showRequisitionViewList?searchTerm=&page=1&pageSize=500&skip=0&take=500";
		String content = iLaborConnector.sendGetRequest(dataUrl1, sessionCookie);
		return getJobPostings(content);
		// dataUrl1 = "https://vendor.ilabor360.com/fetchRequisitionRecord?id=130148";

	}

	public static Job getJob(String jobId, String sessionCookie) throws Exception {
		String dataUrl1 = "https://vendor.ilabor360.com/fetchRequisitionRecord?id=" + jobId;
		String content = iLaborConnector.sendGetRequest(dataUrl1, sessionCookie);
		ObjectMapper mapper = new ObjectMapper();
		Job job = mapper.readValue(content, Job.class);
		return job;
	}

	public static JobPostings makeJobListingsFromGoogleSpreadsheet() throws Exception {
		// throw CODE_ME;
		String latestFile = getLatestJobListingsSpreadSheetFile();
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

	public static void manageJobs() throws Exception {
		String sessionId = getSessionCookie();
		JobPostings latest = getLatestJobPostingsFromPortal(sessionId);
		JobPostings old = makeJobListingsFromGoogleSpreadsheet();
		mergeJobPostings(old, latest);
		List<String> jobsHavingJDs = getJobsWhoHaveJDInGoogleDrive();
		System.out.println(jobsHavingJDs);
		List<String> jobsThatNeedJDs = findJobsThatNeedTobCrawled(jobsHavingJDs, old);
		for (String x : jobsThatNeedJDs) {
			Job job = getJob(x, sessionId);
			File f = writeJobPostingToGoogleDrive(job);
			if (f == null)
				continue;
			System.out.println("Wrote JD for -> " + f.getAbsolutePath());
			int xx = getRandom(60);
			System.out.println("Sleeping for -> " + xx);
			Thread.sleep(xx * 1000);
			// return;
		}

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
	}

	private static JobPostings getLatestJobListings() throws Exception {
		String content = getJobPostingContent();
		// System.out.println(content);
		return getJobPostings(content);
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

	private static String getLatestJobListingsSpreadSheetFile() {

		File dir = new File(System.getProperty("user.home") + "/Downloads/");
		File ff[] = dir.listFiles();
		File file = null;
		for (File f : ff) {
			String name = f.getName();
			if (name.startsWith("Job Requirements - Sheet1")) {

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

	private static JobPostingContent jpc = new JobPostingContentFromFlatFile();

	private static String getJobPostingContent() throws Exception {
		return jpc.getContent();

	}

}

interface JobPostingContent {
	String getContent() throws Exception;
}

class JobPostingContentFromFlatFile implements JobPostingContent {

	String file = "jobs.json";

	@Override
	public String getContent() throws Exception {
		return Util.readContent(file);
	}

}