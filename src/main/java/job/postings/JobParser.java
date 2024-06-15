package job.postings;

import java.io.FileWriter;
import java.io.PrintWriter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise.trading.options.Util;

public class JobParser {

	static String directory = Util.getDirectoryForJobs();

	public static void main(String[] args) throws Exception {
		String content = Util.readContent("job.json");
		// System.out.println(content);
		ObjectMapper mapper = new ObjectMapper();
		Job job = mapper.readValue(content, Job.class);
		String jobDesc = job.requisitionRecObj.jobDescription;
		System.out.println(jobDesc);

		PrintWriter writer = new PrintWriter(new FileWriter(directory + "/test.html"));
		writer.println(jobDesc);
		writer.flush();
		writer.close();
	}
	

}


