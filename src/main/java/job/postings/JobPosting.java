package job.postings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JobPosting {
	public Double c2crate;
	public Date createDate;
	public String customerName;
	public String location;
	public String jobTitle;
	public String requisitionId;
	public String projectDuration;
	public Date projectedStartDate;
	public Date projectedEndDate;
	public String releaseDate;
	public String requisitionStatus;
	public int noOfSubmissions;
	public int submitStatus;

	public Map<String, String> adhocData = new LinkedHashMap<String, String>();

	public Map<String, String> valueData = new LinkedHashMap<String, String>();

	private static LinkedHashMap<String, String> fixedHeaderMap = makeFixedHeaderMap();

	private static LinkedHashMap<String, String> makeFixedHeaderMap() {
		LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
		map.put("Current Status", "getStatus");
		map.put("Requisition Id", "requisitionId");
		map.put("Job Title", "jobTitle");
		map.put("Location", "location");
		map.put("Customer Name", "customerName");
		map.put("Candidate Rate", "computeRate");
		map.put("Project Start Date", "projectedStartDate");
		map.put("Project End Date", "projectedEndDate");
		map.put("Project Duration", "projectDuration");
		map.put("Job Creation Date", "createDate");
		map.put("Number of Submissions", "noOfSubmissions");

		return map;
	}

	public boolean isJobActive() {
		if (submitStatus == 1) {
			return true;
		}
		return false;
	}

	public static String getHeader() {
		// return "Current Status,Requisition Id,Job Title,Location,Customer
		// Name,Candidate Rate,Project Start Date,Project End Date,Project Duration,Job
		// Creation Date,Number of Submissions";
		return createHeader(fixedHeaderMap.keySet());

	}

	public static String createHeader(Collection<String> keys) {
		String header = appendOutput(keys, "");

		return header;
	}

	public String getShareableEntryForSourcers() {
		String output = "";
		if (valueData.size() != 0) {
			output = appendOutput(valueData.values(), output);
		} else {
			for (String x : fixedHeaderMap.values()) {
				output = output + getValueForFieldOrMethod(x) + ",";
			}
		}
		output = appendOutput(adhocData.values(), output);

		return output;
	}

	private static String appendOutput(Collection<String> values, String output) {
		for (String x : values) {
			output = output + escape(x) + ",";
		}
		return output;
	}

	private Field getField(String x) {
		Field f[] = this.getClass().getFields();
		for (Field ff : f) {
			String name = ff.getName();
			if (name.equals(x)) {
				return ff;
			}
		}
		return null;
	}

	private String getValueForFieldOrMethod(String x) {
		try {
			Field f = getField(x);
			if (f == null) {
				Method method = this.getClass().getDeclaredMethod(x);
				return method.invoke(this, null) + "";
			} else {
				Object object = f.get(this);
				if (f.getType() == Date.class) {
					return toDate((Date) object);
				}
				if (f.getType() == String.class) {
					return escape((String) object);
				}
				return object + "";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getShareableEntryForSourcersOld() {
		return getStatus() + "," + requisitionId + "," + escape(jobTitle) + "," + escape(location) + ","
				+ escape(customerName) + "," + computeRate() + "," + toDate(projectedStartDate) + ","
				+ toDate(projectedEndDate) + "," + projectDuration + "," + toDate(createDate) + "," + noOfSubmissions
				+ "," + getAdhocDataIfPresent();
	}

	private String getAdhocDataIfPresent() {
		if (adhocData.size() == 0) {
			return "";
		}
		String data = "";
		for (String value : adhocData.values()) {
			data = data + escape(value) + ",";
		}
		return data;
	}

	private static SimpleDateFormat format = new SimpleDateFormat("YYYY/MM/dd");

	public String toDate(Date d) {
		return format.format(d);
		// return d.toLocaleString();
		// return 1900+d.getYear()+"/"+d.getMonth()+"/"+d.getDay();
	}

	public String getStatus() {
		if (submitStatus == 1) {
			return "Open";
		}
		if (submitStatus == 0) {
			return "Not Accepting";
		}
		return "Filled";

	}

	private Double computeRate() {
		Double x = c2crate * 0.8 ;
		x = Math.floor(x);
		while (true) {
			if (x % 5 == 0) {
				return x;
			}
			x--;
		}

	}

	private static String escape(String input) {
		return "\"" + input.trim() + "\"";
	}

	public void setValuesFromSpreadSheet(List<String> keys, List<String> values) {
		Iterator<String> itr = values.iterator();
		for (String key : keys) {
			String value = itr.next();
			String mappedKey = fixedHeaderMap.get(key);
			if (mappedKey == null) {
				adhocData.put(key, value);
				continue;
			} else {
				valueData.put(key, value);
				// setValue(key, value);
			}
		}
		setRequisitionIdFromValueData();
	}

	private void setRequisitionIdFromValueData() {
		String x = valueData.get("Requisition Id");;
		this.requisitionId = x;
		//System.out.println(x);
		
	}

	public void copyDataFromSource(JobPosting jp) {
		// map.put("Current Status", "getStatus");
		// map.put("Requisition Id", "requisitionId");
		// map.put("Job Title", "jobTitle");
		// map.put("Location", "location");
		// map.put("Customer Name", "customerName");
		// map.put("Candidate Rate", "computeRate");
		// map.put("Project Start Date", "projectedStartDate");
		// map.put("Project End Date", "projectedEndDate");
		// map.put("Project Duration", "projectDuration");
		// map.put("Job Creation Date", "createDate");
		// map.put("Number of Submissions", "noOfSubmissions");)

		this.submitStatus = jp.submitStatus;
		this.requisitionId = jp.requisitionId;
		this.jobTitle = jp.jobTitle;
		this.location = jp.location;
		this.customerName = jp.customerName;
		this.c2crate = jp.c2crate;
		this.projectedStartDate = jp.projectedStartDate;
		this.projectedEndDate = jp.projectedEndDate;
		this.projectDuration = jp.projectDuration;
		this.createDate = jp.createDate;
		this.noOfSubmissions = jp.noOfSubmissions;
	
		this.valueData = new LinkedHashMap<String, String>();

	}

	private void setValue(String key, String value) {
		if (key.equals("getStatus")) {
			if (value.equals("Open")) {
				this.submitStatus = 1;
			} else {
				this.submitStatus = 0;
			}
		}
		if (key.equals("requisitionId")) {
			this.requisitionId = value;
		}
		if (key.equals("jobTitle")) {
			this.jobTitle = value;
		}
		if (key.equals("location")) {
			this.location = value;
		}
		if (key.equals("customerName")) {
			this.customerName = value;
		}
		// map.put("Current Status", "getStatus");
		// map.put("Requisition Id", "requisitionId");
		// map.put("Job Title", "jobTitle");
		// map.put("Location", "location");
		// map.put("Customer Name", "customerName");
		// map.put("Candidate Rate", "computeRate");
		// map.put("Project Start Date", "projectedStartDate");
		// map.put("Project End Date", "projectedEndDate");
		// map.put("Project Duration", "projectDuration");
		// map.put("Job Creation Date", "createDate");
		// map.put("Number of Submissions", "noOfSubmissions");
	}

	public void setJobIsInactive() {
		this.submitStatus = 0;
		
	}
	
	public String toString() {
		return getShareableEntryForSourcers();
	}

}
