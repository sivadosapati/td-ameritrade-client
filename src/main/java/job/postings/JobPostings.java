package job.postings;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobPostings {
	public int iTotalRecords;
	public List<JobPosting> requisitionViewList = new ArrayList<JobPosting>();
	public List<String> header = new ArrayList<String>();

	public int size(){
		return requisitionViewList.size();
	}
	public String findHeader() {
		if( header.size() == 0) {
			return JobPosting.getHeader();
		}
		return JobPosting.createHeader(header);
		
	}

	public JobPosting getJobPosting(String requisitionId) {
		for(JobPosting x : requisitionViewList) {
			if( x.requisitionId.equals(requisitionId)) {
				return x;
			}
		}
		return null;
	}

	public void removeJobPosting(JobPosting x) {
		requisitionViewList.remove(x);
		//System.out.println(size());
		
	}

	public void addJobPosting(JobPosting jp) {
		requisitionViewList.add(jp);
		
	}
	public String toString() {
		return requisitionViewList.toString();
	}
}
