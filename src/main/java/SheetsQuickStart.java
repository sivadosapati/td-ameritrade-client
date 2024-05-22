
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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
import com.google.api.services.sheets.v4.model.ValueRange;

public class SheetsQuickStart {
	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
	// private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	static final String CREDENTIALS_FILE_PATH = VolunteerParser.CREDENTIALS_FILE_PATH;

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
		InputStream in = new FileInputStream(VolunteerParser.CREDENTIALS_FILE_PATH);
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

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 */
	public static void main(String... args) throws IOException, GeneralSecurityException {
		// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		// String spreadsheetId = "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
		// String spreadsheetId =
		// "1-7kqYXHmKLudnKrjEIiu_iV96HNPEuwr/edit#gid=526196651";
		String spreadsheetId = "1iKsc8Pr0kGu8BB1t7Ft4xgQX7DB0ijAoQ5L4IcTRw0w";
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
					} catch (Exception ee) {}
					e.donation = (String)row.get(8);
					System.out.println(e.name + "," + e.phone + "," + e.email + "," + e.donation+","+e.spouseKidsString);
				} catch (Exception e) {
					//System.out.println("########### " +x+ " -> "+e.getMessage());
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

        //StringBuilder csvOutput = new StringBuilder("Spouse Name,Kid1,Age1,Kid2,Age2,Kid3,Age3,Kid4,Age4\n");
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

		//String csvOutput = spouseName + "," + kid1 + "," + age1 + "," + kid2 + "," + age2;
		return csvOutput.toString();

	}

	private static Entry makeEntry(String x) {
		StringTokenizer st = new StringTokenizer(x, "\n");
		String name = st.nextToken().split("\\:")[1];
		name = capitalize(name);
		String email = st.nextToken().split("\\:")[1];
		String phone = st.nextToken().split("\\:")[1];
		phone = VolunteerParser.cleanPhone(phone);
		Entry e = new Entry();
		e.name = name;
		e.phone = phone;
		e.email = email;
		return e;
	}

	private static String capitalize(String sentence) {
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