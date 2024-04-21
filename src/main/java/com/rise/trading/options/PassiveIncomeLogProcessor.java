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
		System.setOut(new PrintStream(new FileOutputStream("temp.txt")));
		// String token = "Not creating a purchase entry for";
		List<String> tokens = Arrays.asList("---------No Position -> Placing purchaseSellCallOrPutIfLongOptionExists",
				"-------Not Placing purchaseSellCallOrPutIfLongOptionExists",
				"Not Creating a purchase entry for",
				"-----Write Code for DontBuyOptionException");
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
