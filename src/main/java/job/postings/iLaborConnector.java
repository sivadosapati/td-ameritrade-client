package job.postings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.rise.trading.options.Util;


 
public class iLaborConnector {
	//private static String sessionCookie = "JSESSIONID=B698E2B9020E92DAF2FEC2966A4B5A1E";
	
	
	public static String login() throws Exception{
		String loginUrl = "https://vendor.ilabor360.com/authenticateLogin";
		//String loginParams = "username=your-username&password=your-password";
	
		String loginParams = getLoginParams();
		String sessionCookie = sendPostRequest(loginUrl, loginParams);
		return sessionCookie;
	}
	

	public static void main(String[] args) throws Exception {
		try {
	
			String sessionCookie = login();

			//Subsequent requests using the session cookie
			String dataUrl1 = "https://vendor.ilabor360.com/showRequisitionViewList?searchTerm=&page=1&pageSize=500&skip=0&take=500";
			dataUrl1 = "https://vendor.ilabor360.com/fetchRequisitionRecord?id=130148";
			String data1 = sendGetRequest(dataUrl1, sessionCookie);
			System.out.println(data1);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String getLoginParams() throws Exception {
		String urlParameters = "userNameForLogin="
				+ URLEncoder.encode(Util.getILaborUserName(), StandardCharsets.UTF_8.toString()) + "&passwordForLogin="
				+ URLEncoder.encode(Util.getILaborPassword(), StandardCharsets.UTF_8.toString()) + "&submit=Login";

		return urlParameters;
	}
	String curl = "curl 'https://vendor.ilabor360.com/authenticateLogin' \\\n" + 
			"  -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7' \\\n" + 
			"  -H 'accept-language: en-US,en;q=0.9' \\\n" + 
			"  -H 'cache-control: max-age=0' \\\n" + 
			"  -H 'content-type: application/x-www-form-urlencoded' \\\n" + 
			"  -H 'cookie: JSESSIONID=B698E2B9020E92DAF2FEC2966A4B5A1E' \\\n" + 
			"  -H 'origin: https://vendor.ilabor360.com' \\\n" + 
			"  -H 'priority: u=0, i' \\\n" + 
			"  -H 'referer: https://vendor.ilabor360.com/logout' \\\n" + 
			"  -H 'sec-ch-ua: \"Google Chrome\";v=\"125\", \"Chromium\";v=\"125\", \"Not.A/Brand\";v=\"24\"' \\\n" + 
			"  -H 'sec-ch-ua-mobile: ?0' \\\n" + 
			"  -H 'sec-ch-ua-platform: \"macOS\"' \\\n" + 
			"  -H 'sec-fetch-dest: document' \\\n" + 
			"  -H 'sec-fetch-mode: navigate' \\\n" + 
			"  -H 'sec-fetch-site: same-origin' \\\n" + 
			"  -H 'sec-fetch-user: ?1' \\\n" + 
			"  -H 'upgrade-insecure-requests: 1' \\\n" + 
			"  -H 'user-agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36' \\\n" + 
			"  --data-raw 'userNameForLogin=siva%40risetogether.pro&passwordForLogin=Seattle123%24&submit=Login'";
	private static String sendPostRequest(String urlString, String urlParameters) throws Exception {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		// HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		HttpURLConnection.setFollowRedirects(false);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

		connection.setRequestProperty("User-Agent", userAgent);
		connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
		connection.setRequestProperty("Origin", "https://vendor.ilabor360.com");
		

		// Send post request
		try (OutputStream os = connection.getOutputStream()) {
			byte b[] = urlParameters.getBytes(StandardCharsets.UTF_8);
			System.out.println(new String(b));
			os.write(b);
			os.flush();
		}

		// Print response for debugging
		getResponse(connection);

		// Capture and store session cookies
		String sessionCookie = getSessionCookie(connection);
		System.out.println(sessionCookie);
		return sessionCookie;

	}

	public static String sendGetRequest(String urlString, String sessionCookie) throws Exception {
		URL url = new URL(urlString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

		connection.setRequestProperty("User-Agent", userAgent);
		connection.setRequestProperty("Sec-Fetch-Site", "same-origin");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		//connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
		connection.setRequestProperty("Origin", "https://vendor.ilabor360.com");

		// Set the session cookie in the request header
		if (!sessionCookie.isEmpty()) {
			//System.out.println("Setting cookie -> " + sessionCookie);
			connection.setRequestProperty("Cookie", sessionCookie);
		}

		// Print response for debugging
		return getResponse(connection);
	}

	private static String getSessionCookie(HttpURLConnection connection) {
		Map<String, List<String>> headerFields = connection.getHeaderFields();
		List<String> cookiesHeader = headerFields.get("Set-Cookie");
		System.out.println(cookiesHeader);
		if (cookiesHeader != null) {
			StringBuilder cookieBuilder = new StringBuilder();
			for (String cookie : cookiesHeader) {
				if (cookieBuilder.length() > 0) {
					cookieBuilder.append("; ");
				}
				cookieBuilder.append(cookie.split(";", 2)[0]);
				break;
			}
			return cookieBuilder.toString();
		}
		return null;
		
	}

	private static void displayResponseHeaders(HttpURLConnection connection) {
		Map<String,List<String>> fields = connection.getHeaderFields();
		for( Map.Entry<String,List<String>> entry : fields.entrySet()) {
			System.out.println(entry.getKey()+ " -> "+entry.getValue());
		}
	}
	private static String getResponse(HttpURLConnection connection) throws Exception{
		
		//System.out.println(connection.getFollowRedirects()+" -> "+connection.getInstanceFollowRedirects());		
		int responseCode = connection.getResponseCode();
		//System.out.println("Response Code: " + responseCode);
		//displayResponseHeaders(connection);

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuilder response = new StringBuilder();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//System.out.println("Response: " + response.toString());
		return response.toString();
	}
}
