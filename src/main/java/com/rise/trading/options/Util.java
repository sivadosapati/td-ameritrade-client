package com.rise.trading.options;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studerw.tda.client.HttpTdaClient;

public class Util {

	private static Properties props = fetchProperties();
	private static Accounts accounts = fetchAccounts();

	public static void main(String args[]) throws Exception {
		playWithDates();
	}
	
	public static String getEODToken() {
		return props.getProperty("eod.api.token");
	}

	private static void playWithDates() throws Exception {
		String ss = "6-11-2019";
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d-M-yyyy");
		LocalDate x = LocalDate.parse(ss,dtf);
		System.out.println(x);
		
		LocalDate ld = LocalDate.now();
		// System.out.println(ld.);
		DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String s = ld.format(df);
		System.out.println(s);

		df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = sdf.parse("2019-08-02 13:35:00");
		System.out.println(d.getTime());

		d = new Date(946713600 * 1000);
		System.out.println(d);

	}

	public static List<String> readLinesFromURL(String url) {
		try {
			URL u = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			List<String> lines = new ArrayList<String>();
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				lines.add(inputLine);
			in.close();
			return lines;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}

	public static Properties getAccessProperties() {
		return props;
	}

	public static Properties fetchProperties() {
		try {
			Properties props = new Properties();
			String path = System.getProperty("user.home");
			props.load(new FileInputStream(path + "/td-ameritrade/tda.properties"));
			return props;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static boolean areEqualDates(LocalDateTime a, LocalDateTime b) {
		if (a.getDayOfMonth() == b.getDayOfMonth()) {
			if (a.getMonth() == b.getMonth()) {
				if (a.getYear() == b.getYear()) {
					return true;
				}
			}
		}
		return false;
	}

	public static Accounts fetchAccounts() {
		try {
			String path = System.getProperty("user.home");
			String content = Files.readString(Paths.get(path + "/td-ameritrade/tda.account.json"));
			Accounts accounts = new ObjectMapper().readValue(content, Accounts.class);
			return accounts;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static HttpTdaClient getHttpTDAClient() {
		try {
			HttpTdaClient httpTdaClient = new HttpTdaClient(props);
			return httpTdaClient;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static String getAccountId1() {
		return props.getProperty("account1.account.id");
	}

	public static List<String> getSymbolsToBeSkipped(String accountId) {

		if (accountId.equals(getAccountId1())) {
			String prop = props.getProperty("account1.symbols.skip", "");
			return Arrays.asList(prop.split(","));
		}
		if (accountId.equals(getAccountId2())) {
			String prop = props.getProperty("account2.symbols.skip", "");
			return Arrays.asList(prop.split(","));
		}
		if (accountId.equals(getAccountId3())) {
			String prop = props.getProperty("account3.symbols.skip", "");
			return Arrays.asList(prop.split(","));
		}
		return new ArrayList<String>();

	}

	public static String toJSON(Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getAccountId3() {
		return props.getProperty("account3.account.id");
	}

	public static String getAccountId2() {
		return props.getProperty("account2.account.id");
	}

	public static String getAccountId4() {
		return props.getProperty("account4.account.id");
	}

	public static String[] getAllAccounts() {
		Account[] accounts = getAccounts().getAccounts();
		String[] accountIds = new String[accounts.length];
		int counter = 0;
		for (Account a : accounts) {
			accountIds[counter++] = a.id;
		}
		return accountIds;
	}

	public static Accounts getAccounts() {
		return accounts;
	}
}
