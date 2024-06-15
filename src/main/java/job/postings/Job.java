package job.postings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
	public RequisitionRecObj requisitionRecObj;
	
	public String findJobId() {
		if( requisitionRecObj == null)
			return null;
		return requisitionRecObj.requisitionId;
	}
}

@JsonIgnoreProperties(ignoreUnknown = true)
class RequisitionRecObj {
	public String jobDescription;
	public String requisitionId;
	public JobTitle jobTitle;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class JobTitle {
	public String jobTitleCode;
	
}