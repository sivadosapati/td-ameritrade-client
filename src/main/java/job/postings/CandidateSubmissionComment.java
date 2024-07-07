package job.postings;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CandidateSubmissionComment {
	public List<CandidateSubmissionEntry> listSubmissionComment = new ArrayList<CandidateSubmissionEntry>();
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CandidateSubmissionEntry {
	public CommentBy commentBy;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CommentBy {
	public Contact contact;
	public String userName;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Contact {
	public String fullName;
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CandidateSubmissionList {
	public List<CandidateEntry> candidateSubmissionListView = new ArrayList<CandidateEntry>();
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CandidateEntry {
	public String candidateSubmissionId;
	public String requisitionId;
	public String customerName;
	public String lastComment;
	public String lastCommentDate;
}
