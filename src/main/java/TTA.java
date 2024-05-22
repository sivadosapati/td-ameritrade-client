import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TTA {

	public static void main123(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("/Users/sivad/tta.txt"));
		reader.readLine();
		ArrayList<String> lines = new ArrayList<String>();
		
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			String[] x = line.split("\t");
			String number = parse(x[2]);
			for( int i =0;i<x.length;i++) {
				String n = x[i];
				if( i == 1)continue;
				if (i == 2)
					n = number;
				System.out.print(n+",");
			}
			System.out.println();
		}
		reader.close();

	}
	
	public static void main(String[] args) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader("/Users/sivad/git/td-ameritrade-client/src/main/java/tta_committee_chairs_with_phones.csv"));
		//reader.readLine();
		ArrayList<String> lines = new ArrayList<String>();
		int counter = 1;
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			String[] x = line.split(",");
			if( x.length >= 3 && !x[2].trim().equals(""))
				System.out.println(counter+++","+line);
		}
		reader.close();

	}

	private static String parse(String phone) {
		char ch[] = phone.toCharArray();
		String a = "";
		for (char x : ch) {
			if (Character.isDigit(x)) {
				a += x;
			}
		}
		return a;
	}

	public static void mainssss(String args[]) throws Exception {

		// sno,number,name,templateid,var1,var2,var3,var4
		// HX8bd62845458a6abf37de1d0e336856e6
		BufferedReader reader = new BufferedReader(
				new FileReader("/Users/sivad/git/td-ameritrade-client/src/main/java/x.txt"));

		ArrayList<String> lines = new ArrayList<String>();
		int counter = 1;
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			String[] x = line.split(",");
			if (x.length > 2) {
				String phone = x[2].trim();
				if (phone.equals(""))
					continue;
				System.out.println(counter + "," + phone + "," + x[1].trim() + "," + x[0].trim()
						+ ",HX8bd62845458a6abf37de1d0e336856e6");
			}
			counter++;

		}

	}

	public static String nameSplitter(String line) {
		// Sample lines with different formats
		String[] lines = { "John Doe", "Jane Doe - (123) 456-7890", "Alice - 987-654-3210", "Bob - +1 345-678-9012",
				"Carol - 555-1234", "David" };

		// Regular expression patterns to match different phone number formats
		String phonePattern = "(\\(\\d{3}\\)\\s*\\d{3}-\\d{4})|(\\d{3}-\\d{3}-\\d{4})|(\\+1\\s*\\d{3}-\\d{3}-\\d{4})";
		Pattern pattern = Pattern.compile(phonePattern);

		String name = line.trim();
		String phone = "";

		Matcher matcher = pattern.matcher(line);
		if (matcher.find()) {
			phone = matcher.group().trim();
			name = line.substring(0, matcher.start()).trim();
		}

		return name + "," + phone;
	}

	private static void parseLine(String csvLine) {
		// Split the CSV line by commas
		String[] fields = csvLine.split(",");

		// Pattern to match name and phone number in the format "Name - Number"
		Pattern pattern = Pattern.compile("\"(.*?) - (\\d+)\"");

		// List to store the parsed results
		List<String> parsedCSV = new ArrayList<>();

		String chair = fields[0];

		// Iterate through each field
		for (int i = 1; i < fields.length; i++) { // Starting from index 1 to skip the first empty field
			String field = fields[i].trim();
			Matcher matcher = pattern.matcher(field);
			if (matcher.matches()) {
				// Extract name and phone number from the matched group
				String name = matcher.group(1);
				String phone = matcher.group(2);
				// Append the parsed result to the list
				parsedCSV.add(chair + ", " + name + ", " + phone);
			} else if (!field.isEmpty()) {
				// If no name and phone number found, but field is not empty, assume it's just a
				// name
				parsedCSV.add(chair + ", " + field + ", ");
			}
		}

		// Print the parsed CSV in the specified format
		for (String result : parsedCSV) {
			System.out.println(result);
		}
	}

}

/*
 * 
 * Write a Java program to parse a line which is a csv with this format
 * ,Chair,Advisor 1,Advisor 2,Co-Chair 1,Co-Chair 2,Co-Chair 3,Co-Chair
 * 4,Co-Chair 5,Advisor 3,Advisor 4.
 * 
 * 
 * Each field, other than Chair can have a name and phone number in it, You need
 * to look for phone number in the other fields. Name and Phone number will be
 * in " ", like "Name - Number", if present.
 * 
 * After parsing, you are expected to print the file in this format
 * 
 * Chair, Advisor 1 Name, Advisor 1 Phone Chair, Advisor 2 Name, Advisor 2 Phone
 * 
 * Not every attribute may have phone number but that't okay
 * 
 * Can you please write the java program to parse the original CSV and print the
 * new CSV
 * 
 */

//
