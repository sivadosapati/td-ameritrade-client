package com.rise.trading.options;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PassiveIncomeLogProcessor {

	public static void main(String[] args) throws Exception {
		System.setOut(new PrintStream(new FileOutputStream("temp.csv")));
		// String token = "Not creating a purchase entry for";
		List<String> tokens = Arrays.asList("---------No Position -> Placing purchaseSellCallOrPutIfLongOptionExists",
				"-------Not Placing purchaseSellCallOrPutIfLongOptionExists",
				"Not Creating a purchase entry for",
				"-----Write Code for DontBuyOptionException", "placeCoveredCallsForEquities", "Finding a new possible option","is less than 0.10 and it's not worth to open a spread for","Create Alternate Spread");
		
		String file = "/Users/sivad/git/td-ameritrade-client/passive_income.log";
		Stream<String> lines = Files.lines(Paths.get(file));
		lines.forEach((l) -> {
			for (String token : tokens)
				if (l.contains(token)) {
					System.out.println(l);
				}
		});
		lines.close();
	}

}
