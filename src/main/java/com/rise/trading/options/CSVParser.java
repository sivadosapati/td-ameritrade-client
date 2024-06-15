package com.rise.trading.options;

import java.util.ArrayList;
import java.util.List;

public class CSVParser {
	public static List<String> parseCSV(String csv) {
		List<String> result = new ArrayList<>();
		StringBuilder currentField = new StringBuilder();
		boolean inQuotes = false;

		for (int i = 0; i < csv.length(); i++) {
			char c = csv.charAt(i);

			if (c == '"') {
				inQuotes = !inQuotes; // Toggle the inQuotes flag
			} else if (c == ',' && !inQuotes) {
				// If we encounter a comma outside of quotes, we end the current field
				result.add(currentField.toString().trim());
				currentField.setLength(0); // Reset the StringBuilder
			} else {
				// Append the current character to the current field
				currentField.append(c);
			}
		}

		// Add the last field
		result.add(currentField.toString().trim());

		// Remove quotes from fields that are quoted
		for (int i = 0; i < result.size(); i++) {
			String field = result.get(i);
			if (field.startsWith("\"") && field.endsWith("\"")) {
				result.set(i, field.substring(1, field.length() - 1));
			}
		}

		return result;
	}

}