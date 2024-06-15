package job.postings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridCoordinate;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

public class JobPostingSpreadSheet {
	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	// private static final List<String> SCOPES =
	// Collections.singletonList(SheetsScopes.SPREADSHEETS);
	private static final List<String> SCOPES = new ArrayList<String>(SheetsScopes.all());

	// private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	// public static final String CREDENTIALS_FILE_PATH =
	// "/Users/sivad/Downloads/client_secret_google.json";

	// public static final String CREDENTIALS =
	// "/Users/sivad/Downloads/client_secret_766426810034-i3l577ngunu8vs0j5f9v1jte4ekq9oum.apps.googleusercontent.com.json";

	public static final String CREDENTIALS = "/Users/sivad/Downloads/client_secret_766426810034-tut5cbnb1ktomt9c169474307rh1gl1u.apps.googleusercontent.com.json";
	static final String CREDENTIALS_FILE_PATH = CREDENTIALS;

	/**
	 * Creates an authorized Credential object.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		// InputStream in =
		// SheetsQuickStart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	static void writeExample(String spreadsheetId) throws Exception {

		Sheets service = getSheetsService();
		List<Request> requests = new ArrayList<>();

		List<CellData> values = new ArrayList<>();

		values.add(new CellData().setUserEnteredValue(new ExtendedValue().setStringValue("Hello World!")));
		requests.add(new Request().setUpdateCells(
				new UpdateCellsRequest().setStart(new GridCoordinate().setSheetId(0).setRowIndex(0).setColumnIndex(0))
						.setRows(Arrays.asList(new RowData().setValues(values)))
						.setFields("userEnteredValue,userEnteredFormat.backgroundColor")));

		BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
		service.spreadsheets().batchUpdate(spreadsheetId, batchUpdateRequest).execute();
	}

	public static Sheets getSheetsService() throws Exception {
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		// String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
		// String spreadsheetId =
		// "1-7kqYXHmKLudnKrjEIiu_iV96HNPEuwr/edit#gid=526196651";
		// String spreadsheetId = "1wWYOG4IM9Rd_O_bSj7SUS-1FVDBZgY8sP06QNniIKG8";

		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		return service;

	}

	public static void main(String... args) throws Exception {
		// identifyRowAndTable();
		String spreadsheetId = "1ZRyPXjuybQbnSnN_KTVRH_Fa0L0yPVKjVt-bbGDtiH4";
		//writeExample(spreadsheetId);
		readContentForJobPostings(spreadsheetId, "Sheet1");
	}

	public static List<List<Object>> readContentForJobPostings(String sheetId, String range) throws Exception {
		Sheets sheets = getSheetsService();
		// Build a new authorized API client service.
		ValueRange response = sheets.spreadsheets().values().get(sheetId, range).execute();

		List<List<Object>> values = response.getValues();
		for (List row : values) {
			for (Object o : row) {
				System.out.println(o + ",");
			}
			System.out.println();
		}
		return response.getValues();

	}

	private static void identifyRowAndTable() throws Exception, IOException {
		String spreadsheetId = "1fF1Qgj8hH0PjGHOqtvGaPuwXhREffgHdZzgcSVpPWqw";
		// String range = "Class Data!A2:E";
		String range = "Sheet1";
		Sheets sheets = getSheetsService();
		// Build a new authorized API client service.
		ValueRange response = sheets.spreadsheets().values().get(spreadsheetId, range).execute();
		List<List<Object>> values = response.getValues();
		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {
			// System.out.println("Name, Major");
			int counter = 0;
			System.out.println("sno,countrycode,number,name,title,var2,var3,var4");
			// System.out.println("Name,Phone,Email,Donation,Spouse Name,Kid1 Name,
			// Age1,Kid2 Name, Age2,Kid3 Name, Age3");

			for (List row : values) {
				String name = (String) row.get(0);
				name = name.replaceAll(",", ":");
				String table[] = ((String) row.get(1)).split(",");
				if (table.length != 2)
					continue;
				String rowNo = table[0];
				String tableNo = table[1];
				String seats = (String) row.get(2);
				String phone = (String) row.get(5);
				if (phone.equals("null")) {
					continue;
				}
				// sno,country_code,phone,name,title,var2,var3,var4
				String tableNoString = "Table Number : " + tableNo;
				String rownNoString = "Row Number : " + rowNo;
				String seatsString = "Seats Allotted : " + seats;
				System.out.println("1,1," + phone + "," + name + ",Garu," + tableNoString + "," + rownNoString + ","
						+ seatsString);
			}
		}
	}

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 */
	public static void mainOldest(String... args) throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		// String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
		// String spreadsheetId =
		// "1-7kqYXHmKLudnKrjEIiu_iV96HNPEuwr/edit#gid=526196651";
		String spreadsheetId = "1wWYOG4IM9Rd_O_bSj7SUS-1FVDBZgY8sP06QNniIKG8";
		// String range = "Class Data!A2:E";
		String range = "Worksheet";
		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		ValueRange response = service.spreadsheets().values().get(spreadsheetId, range).execute();
		List<List<Object>> values = response.getValues();
		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {
			// System.out.println("Name, Major");
			int counter = 0;
			System.out.println("Name,Phone,Email,Donation,Spouse Name,Kid1 Name, Age1,Kid2 Name, Age2,Kid3 Name, Age3");
			for (List row : values) {
				counter++;
				if (counter <= 2) {

					continue;
				}
				// Print columns A and E, which correspond to indices 0 and 4.
				String x = (String) row.get(3);
				try {
					Entry e = makeEntry(x);
					String y = (String) row.get(4);
					try {
						e.spouseKidsString = makeSpouseKidsString(y);
					} catch (Exception ee) {
					}
					e.donation = (String) row.get(8);
					System.out.println(
							e.name + "," + e.phone + "," + e.email + "," + e.donation + "," + e.spouseKidsString);
				} catch (Exception e) {
					// System.out.println("########### " +x+ " -> "+e.getMessage());
				}
			}
		}
	}

	private static String makeSpouseKidsString(String input) {
		String[] lines = input.split("\n");
		String spouseFirstName = "";
		String spouseLastName = "";
		List<String> kids = new ArrayList<>();
		List<Integer> ages = new ArrayList<>();

		for (int i = 0; i < lines.length; i++) {
			if (lines[i].startsWith("Spouse First Name:")) {
				spouseFirstName = lines[i].split(": ")[1];
			} else if (lines[i].startsWith("Spouse Last Name:")) {
				spouseLastName = lines[i].split(": ")[1];
			} else if (lines[i].startsWith("kid Name:")) {
				kids.add(capitalize(lines[i].split(": ")[1]));
			} else if (lines[i].startsWith("Age:")) {
				ages.add(Integer.parseInt(lines[i].split(": ")[1]));
			}
		}

		String spouseName = capitalize(spouseFirstName + " " + spouseLastName);
		String kid1 = kids.size() > 0 ? kids.get(0) : "";
		String age1 = ages.size() > 0 ? String.valueOf(ages.get(0)) : "";
		String kid2 = kids.size() > 1 ? kids.get(1) : "";
		String age2 = ages.size() > 1 ? String.valueOf(ages.get(1)) : "";
		String kid3 = kids.size() > 2 ? kids.get(2) : "";
		String age3 = ages.size() > 2 ? String.valueOf(ages.get(2)) : "";
		String kid4 = kids.size() > 3 ? kids.get(3) : "";
		String age4 = ages.size() > 3 ? String.valueOf(ages.get(3)) : "";

		// StringBuilder csvOutput = new StringBuilder("Spouse
		// Name,Kid1,Age1,Kid2,Age2,Kid3,Age3,Kid4,Age4\n");
		StringBuilder csvOutput = new StringBuilder();
		csvOutput.append(spouseName);

		if (!kid1.isEmpty()) {
			csvOutput.append(",").append(kid1).append(",").append(age1);
			if (!kid2.isEmpty()) {
				csvOutput.append(",").append(kid2).append(",").append(age2);
				if (!kid3.isEmpty()) {
					csvOutput.append(",").append(kid3).append(",").append(age3);
					if (!kid4.isEmpty()) {
						csvOutput.append(",").append(kid4).append(",").append(age4);
					}
				}
			}
		}

		// String csvOutput = spouseName + "," + kid1 + "," + age1 + "," + kid2 + "," +
		// age2;
		return csvOutput.toString();

	}

	private static Entry makeEntry(String x) {
		StringTokenizer st = new StringTokenizer(x, "\n");
		String name = st.nextToken().split("\\:")[1];
		name = capitalize(name);
		String email = st.nextToken().split("\\:")[1];
		String phone = st.nextToken().split("\\:")[1];
		// phone = phone;
		Entry e = new Entry();
		e.name = name;
		e.phone = phone;
		e.email = email;
		return e;
	}

	public static String capitalize(String sentence) {
		String[] words = sentence.split("\\s+");
		StringBuilder capitalizedSentence = new StringBuilder();

		for (String word : words) {
			if (word.length() > 0) {
				capitalizedSentence.append(Character.toUpperCase(word.charAt(0)))
						.append(word.substring(1).toLowerCase()).append(" ");
			}
		}

		// Remove the trailing space
		return capitalizedSentence.toString().trim();
	}
}

class Entry {
	String name;
	String phone;
	String email;
	String donation;
	String spouseKidsString = "";
}