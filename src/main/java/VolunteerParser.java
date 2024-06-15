
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

class GoogleAuthorizeUtil {
	public static Credential authorize() throws IOException, GeneralSecurityException {
		InputStream in = new FileInputStream(VolunteerParser.CREDENTIALS_FILE_PATH);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JacksonFactory.getDefaultInstance(),
				new InputStreamReader(in));

		List<String> scopes = Arrays.asList(SheetsScopes.SPREADSHEETS);

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), clientSecrets,
				scopes).setDataStoreFactory(new MemoryDataStoreFactory()).setAccessType("offline").build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

		return credential;
	}

}

public class VolunteerParser {

	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
	// private static final String SPREADSHEET_ID =
	// "1-7kqYXHmKLudnKrjEIiu_iV96HNPEuwr";

	// private static final String SPREADSHEET_ID =
	// "https://docs.google.com/spreadsheets/d/1-7kqYXHmKLudnKrjEIiu_iV96HNPEuwr/edit#gid=125219895";

	// private static String SPREADSHEET_ID
	// ="1sILuxZUnyl_7-MlNThjt765oWshN3Xs-PPLfqYe4DhI/edit#gid=0";
	// static String SPREADSHEET_ID =
	// "1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms";
	// static String SPREADSHEET_ID =
	// "1clDPQd61YSCbqBM67HHzh3SaY1ymrIwBrOdVGa-vsMo";
	static String SPREADSHEET_ID = "1CSEzwOMbHG7JI0Cqvyht4_cCJnQFtzGsQB51no-t10s";

	public static final String CREDENTIALS_FILE_PATH = "/Users/sivad/Downloads/client_secret_google.json";

	public static final String CREDENTIALS = "/Users/sivad/Downloads/client_secret_766426810034-i3l577ngunu8vs0j5f9v1jte4ekq9oum.apps.googleusercontent.com.json";

	private static Sheets getSheetsServiceOld() throws IOException, GeneralSecurityException {
		GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE_PATH))
				.createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));
		return new Sheets.Builder(new com.google.api.client.http.javanet.NetHttpTransport(),
				new com.google.api.client.json.jackson2.JacksonFactory(), new HttpCredentialsAdapter(credentials))
						.setApplicationName(APPLICATION_NAME).build();
	}

	private static Sheets getSheetsService() throws IOException, GeneralSecurityException {
		Credential credential = GoogleAuthorizeUtil.authorize();
		return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(),
				credential).setApplicationName(APPLICATION_NAME).build();
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {
		// Sheets service = new
		// Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY,
		// null)
		// .setApplicationName(APPLICATION_NAME).build();
		Sheets service = getSheetsService();

		// Get all sheet names
		Spreadsheet spreadsheet = service.spreadsheets().get(SPREADSHEET_ID).execute();
		// sspreadsheet = service.spreadsheets().get

		List<Sheet> sheets = spreadsheet.getSheets();

		List<List<Object>> combinedData = new ArrayList<>();
		int count = 0;
		List<Group> groups = new ArrayList<Group>();
		for (Sheet sheet : sheets) {
			if (count == 0) {
				count++;
				continue;
			}
			String sheetName = sheet.getProperties().getTitle();
			// String range = sheetName + "!B:B,D:D";
			String range = sheetName + "!A2:G";

			ValueRange response = service.spreadsheets().values().get(SPREADSHEET_ID, range).execute();

			List<List<Object>> values = response.getValues();

			for (List<Object> row : values) {
				if (row.size() >= 4) {
					List<Object> filteredRow = new ArrayList<>();
					String name = SheetsQuickStart.capitalize(row.get(1) + "".trim());
					name = cleanName(name);
					String phone = row.get(3) + "".trim();
					if (name.equals("") || phone.equals("")) {
						continue;
					}
					if (name.equals("Full Name")) {
						continue;
					}
					phone = cleanPhone(phone);
					filteredRow.add(name); // 2nd column (B)
					filteredRow.add(phone); // 4th column (D)
					// System.out.println(name+","+phone+","+sheetName);
					addToGroup(sheetName, name, phone, groups);
					combinedData.add(filteredRow);
				}
			}
		}
		
	

		// System.out.println("-------------");

		// Sort combined data based on the 2nd column (B) in descending order
		combinedData.sort(Comparator.comparing(o -> o.get(0).toString(), Comparator.reverseOrder()));

		// Print the sorted data
		for (List<Object> row : combinedData) {
			// System.out.printf("%s, %s%n", row.get(0), row.get(1));
		}
		// System.out.println("*********************");
		Collections.sort(groups, Group.GroupComparator);
		for (Group g : groups) {
			System.out.println(g.toString());
		}
	}
	
	private static String cleanName(String name) {
		return name.replace("(Co-Chair)","").replace(("(co-chair)"), "").replace(("(Chair)"), "");
	}

	private static void addToGroup(String sheetName, String name, String phone, List<Group> groups) {
		Group newGroup = new Group(sheetName, name, phone);
		for (Group g : groups) {
			String n = g.name;
			String p = g.phone;
			if (n.equalsIgnoreCase(name) && p.equals(phone)) {
				// System.out.println("Duplicate name and phone -> "+g.toString()+" ->
				// "+newGroup.toString());
				return;
			}
			if (n.equalsIgnoreCase(name)) {
				// System.out.println("Duplicate name only -> "+g.toString()+" ->
				// "+newGroup.toString());
				return;
			}
			if (p.equals(phone)) {
				// System.out.println("Duplicate phone only -> "+g.toString()+" ->
				// "+newGroup.toString());
				return;
			}
			if (phone.length() != 10) {
				return;
			}
		}
		// System.out.println(name+","+phone+","+sheetName);
		groups.add(newGroup);

	}

	public static String cleanPhone(String phone) {
		String p = "";
		for (char ch : phone.toCharArray()) {
			if (Character.isDigit(ch)) {
				p = p + ch;
			}
		}
		return p;
	}
}

class Group {
	String theme;
	String name;
	String phone;

	Group(String t, String name, String p) {
		this.theme = t;
		this.name = name;
		this.phone = p;
	}

	public String toString() {
		return name + "," + phone + "," + theme;
	}

	// Comparator for sorting by name, then theme, then phone
	public static Comparator<Group> GroupComparator = new Comparator<Group>() {
		@Override
		public int compare(Group g1, Group g2) {
			// Compare by name first
			int nameCompare = g1.name.compareTo(g2.name);
			if (nameCompare != 0) {
				return nameCompare;
			}

			// If names are equal, compare by theme
			int themeCompare = g1.theme.compareTo(g2.theme);
			if (themeCompare != 0) {
				return themeCompare;
			}

			// If both names and themes are equal, compare by phone
			return g1.phone.compareTo(g2.phone);
		}
	};
}